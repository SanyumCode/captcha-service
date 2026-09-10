package net.Captcha.inference;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.MongoInterruptedException;

import ai.onnxruntime.OrtException;
import net.Captcha.Utils.GlobalUtils;
import net.Captcha.mongod.DBManager;
import net.Captcha.redis.RedisManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class InferInstance {
    private static final Logger logger = LoggerFactory.getLogger(InferInstance.class);
    private static InferInstance INSTANCE;
    private InferImpl inferImpl;
    private RedisManager redisManager;
    private DBManager dbManager;
    private int nThread;
    private List<Thread> threadL;
    private int isRunning = 0; //0未启动 //1已启动 //2停止错误

    private InferInstance(String modelPath, int nThread) throws OrtException {
        try {
            InferImpl.init(modelPath);
            this.inferImpl = InferImpl.getInstance();
            this.dbManager = DBManager.getInstance();
            this.redisManager = RedisManager.getInstance();
            this.nThread = nThread;
            this.threadL = new ArrayList<Thread>();
            logger.info("InferInstance初始化成功: nThread={}", nThread);
        } catch (OrtException e) {
            logger.error("InferInstance初始化失败: ONNX模型初始化错误", e);
            throw e; // 重新抛出OrtException
        } catch (RuntimeException e) {
            logger.error("InferInstance初始化失败: 依赖组件初始化错误", e);
            throw e; // 重新抛出RuntimeException
        } catch (Exception e) {
            logger.error("InferInstance初始化失败: 未知错误", e);
            throw new RuntimeException("InferInstance初始化失败: " + e.getMessage(), e);
        }
    }
    public int getIsRunning() {
        return isRunning;
    }

     // 初始化方法，必须先调用
    public static synchronized void init(String modelPath, int nThread) throws OrtException {
        if (INSTANCE == null) {
            INSTANCE = new InferInstance(modelPath, nThread);
        }

    }

    public static InferInstance getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException(
                    "InferInstance未初始化。请先调用 InferInstance.init(modelPath, nThread) 方法进行初始化。" +
                    "示例: InferInstance.init(\"path/to/model.onnx\", 4);");
        }
        return INSTANCE;
    }

    public void start() {
        // 检查初始化状态
        if (inferImpl == null || dbManager == null || redisManager == null) {
            logger.error("InferInstance未正确初始化，无法启动");
            throw new IllegalStateException("InferInstance未正确初始化，请先调用init()方法");
        }

        // 检查Redis是否可用
        if (!redisManager.isAvailable()) {
            logger.error("Redis不可用，无法启动推理线程");
            throw new IllegalStateException("Redis不可用，无法启动推理线程");
        }

        for(int i = 1; i <= nThread; i++) {
            Thread thread = new Thread(new InferThread(), String.valueOf(i)+"号线程");
            threadL.add(thread);
            thread.start();
        }
        logger.info("{}条推理线程启动成功", nThread);
        isRunning = 1;
    }
    public void stop() {
        isRunning = 0;
        long timeoutMillis = 5000;  // 最多等待 5 秒

        // 中断所有线程
        for(Thread one : threadL) {
            one.interrupt();
        }

        // 等待线程结束
        long start = System.currentTimeMillis();
        for(Thread thread : threadL) {
            long timeLeft = timeoutMillis - (System.currentTimeMillis() - start);
            if(timeLeft <= 0) break;

            try {
                thread.join(timeLeft); // 等待线程结束，超时则跳出
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // 保留中断标志
                break;
            }
        }

        // 检查未结束的线程
        boolean allStopped = true;
        for(Thread thread : threadL) {
            if(thread.isAlive()) {
                System.err.println(thread.getName() + " 未在超时时间内结束");
                allStopped = false;
            }
        }

        if (!allStopped) {
            isRunning = 2;
        } else {
            threadL.clear(); // 清理线程列表
        }
    }
    class InferThread implements Runnable{

        @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()) {
                String message = null;
                boolean isSuccess = false;
                try {
                    message = redisManager.popFromBlockingQueue("captcha");
                    // 空值检查：防止NPE
                    if (message == null || message.trim().isEmpty()) {
                        logger.warn("收到空消息，跳过处理");
                        continue;
                    }

                    String[] split = message.split("-");
                    // 数组越界防护
                    if (split.length < 3) {
                        logger.error("消息格式错误: {}", message);
                        continue;
                    }

                    Path path = Paths.get(GlobalUtils.img_Path.getPath(), split);
                    //默认处理.jpeg，后期增加多图片可以改
                    path = Paths.get(path.toString() + ".jpeg");
                    File file = path.toFile();
                    if(file.exists() && file.isFile()) {
                        String predict = null;
                        try {
                            predict = inferImpl.predict(file.getPath());
                        }catch(ImageReadException e) {
                            e.printStackTrace();
                        }
                        if(predict != null && predict.trim().matches("^[a-zA-Z0-9]+$")) {
                            dbManager.updateImage(message, predict);
                        }
                        else dbManager.invalidImage(message);
                        isSuccess = true;
                    }else {
                        dbManager.invalidImage(message);
                        isSuccess = true;
                    }
                }catch(Exception e) {
                    if(e instanceof InterruptedException || e instanceof MongoInterruptedException) {
                        System.err.println(Thread.currentThread().getName() + "收到中断信号");
                        Thread.currentThread().interrupt(); // 重新设置中断标记
                        break;
                    }else {
                        System.err.println(Thread.currentThread().getName() + ": 发生错误，数据回滚至Redis");
                        e.printStackTrace();
                    }
                }finally {
                    if(message != null && !isSuccess) {
                        try {
                            redisManager.pushToQueue("captcha", message);
                        } catch (Exception e) {
                            System.err.println(Thread.currentThread().getName() + ": 回滚至Redis失败: " + e.getMessage());
                        }
                    }
                }


            }

        }


    }





}

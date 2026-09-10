package net.Captcha.inference;

import java.util.Scanner;

import net.Captcha.mongod.DBManager;
import net.Captcha.redis.RedisManager;



public class InferMain {
    public static void main(String[] args) throws Exception {
        // 设置日志文件前缀，区分不同进程的日志
        // 如果未设置，使用默认值"app"
        if (System.getProperty("log.prefix") == null) {
            System.setProperty("log.prefix", "inference");
        }

        boolean valid = false;
        String modelPath = null;
        int nThread = 0;
        if(args.length == 2) {
            modelPath = args[0];
            try {
                nThread = Integer.parseInt(args[1]);
                valid = true;
            } catch(NumberFormatException ignored) {}

        }
        if(!valid) {
            System.err.println("Usage: java -jar .../myapp.jar <modelPath> <nThread>");
        }
        InferInstance.init(modelPath, nThread);
        InferInstance instance = InferInstance.getInstance();

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("请输入操作：1 = 启动，2 = 停止，0 = 退出程序");
            String input = scanner.nextLine().trim();


                switch (input) {
                    case "1":
                        if (instance.getIsRunning() == 0) {
                            instance.start();
                            System.out.println("[OK] 程序已启动:");
                        } else {
                            System.out.println("[WARN] 程序已经在运行");
                        }
                        break;
                    case "2":
                        if (instance.getIsRunning() == 1) {
                            System.out.println("[STOPPING] 正在优雅关闭程序...");
                            instance.stop();   // 优雅关闭
                            if(instance.getIsRunning() == 2) {
                                System.out.println("程序关闭出错，卡死...");
                            }else {
                                System.out.println("程序已正常关闭");
                            }
                        } else {
                            System.out.println("[WARN] 程序未运行或卡死");
                        }
                        break;
                    case "0":
                        if (instance.getIsRunning() == 1) {
                            System.out.println("[STOPPING] 正在优雅关闭程序...");
                            instance.stop();
                            if(instance.getIsRunning() == 2) {
                                System.out.println("程序关闭出错，卡死...");
                            }else {
                                System.out.println("程序已正常关闭");
                            }
                        }
                        running = false;
                        System.out.println("程序退出");
                        break;
                    default:
                        System.out.println("无效输入，请输入 1/2/0");
                }


        }
        scanner.close();
        DBManager.getInstance().close();
        RedisManager.getInstance().close();
        System.exit(0);
    }

}

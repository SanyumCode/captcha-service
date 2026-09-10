package net.Captcha.servlet;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.Captcha.Utils.GlobalUtils;
import net.Captcha.common.ErrorCode;
import net.Captcha.common.Image;
import net.Captcha.common.ImgType;
import net.Captcha.mongod.DBManager;
import net.Captcha.redis.RedisManager;

public class CaptchaPostServlet extends HttpServlet{
    private static final Logger logger = LoggerFactory.getLogger(CaptchaPostServlet.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private static final long serialVersionUID = 1997479069717404668L;
    private static final String REDIS_QUEUE_NAME = "captcha";
    private DBManager dbManager;
    private RedisManager redisManager;

    public CaptchaPostServlet() {
        super();
        this.dbManager = DBManager.getInstance();
        this.redisManager = RedisManager.getInstance();
    }
/**
 * Post表单的Json键为:imgBase64
 */
    @SuppressWarnings("unchecked")
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
         // 核实Content-Type
        try {
            String contentType = req.getContentType();
            if (contentType == null || !contentType.startsWith("application/json")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"ok\": false, \"cause\": \"Content-Type must be application/json\"}");
                return;
            }

             // 解析请求体
            Map<String, String> data;
            try {
                data = mapper.readValue(req.getInputStream(), Map.class);
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"ok\": false, \"cause\": \"Invalid JSON format\"}");
                return;
            }

            ErrorCode error = null;
            byte[] imageBytes = null;
            int usid = 0;
            Image img = new Image();
            String imgBase64 = data.get("imgBase64");
            String userName = data.get("user");
            String api_key = data.get("api_key");

            // 输入验证：检查参数是否为空或过长
            if(imgBase64 == null || userName == null || api_key == null) {
                error = ErrorCode.NONE_DATA;
            } else if (userName.length() > 100 || api_key.length() > 200) {
                error = ErrorCode.NONE_DATA; // 参数过长
            } else if (imgBase64.length() > 10 * 1024 * 1024) { // 限制Base64字符串最大10MB（约7.5MB图片）
                error = ErrorCode.DECODE_FAIL; // 图片过大
            }

             if(error == null) {
                 usid = dbManager.checkUser(userName, api_key);
                 if(usid == -1) error = ErrorCode.USER_INDENTITY_FAIL;
             }


            if(error == null) {
                try {
                    // Base64解码大小限制：防止内存溢出攻击
                    imageBytes = Base64.getDecoder().decode(imgBase64);
                    // 限制图片大小最大8MB
                    if (imageBytes.length > 8 * 1024 * 1024) {
                        error = ErrorCode.DECODE_FAIL;
                    }
                } catch (IllegalArgumentException | NullPointerException ignored) {
                    error = ErrorCode.DECODE_FAIL;
                } catch (OutOfMemoryError e) {
                    logger.error("Base64解码导致内存溢出", e);
                    error = ErrorCode.DECODE_FAIL;
                }

                if(imageBytes != null) {
                    ImgType imgT = checkImg(imageBytes);
                    if(imgT == ImgType.ERROR) {
                        error = ErrorCode.TYPE_FAIL;
                    }else {
                        String uidg = setUID(usid);
                        img.setUID(uidg);
                        img.setType(imgT);
                        img.setStoreTime(new Date());
                        img.setData(imageBytes);
                        img.setUserId(usid);
                        img.setStatus("I");
                         // Date -> LocalDate
                        LocalDate localDate = img.getStoreTime().toInstant()
                                                       .atZone(ZoneId.systemDefault())
                                                       .toLocalDate();

                        // 格式化为 YYYY-MM-DD
                        String formatted = localDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
                        String message = String.valueOf(img.getUserId()) + "-" + formatted + "-" + img.getUID();

                        int tryTimes = 0;
                        int step = 0;
                        boolean success = false;
                        while (tryTimes < 3 && !success) {
                            tryTimes++;
                            try {
                                if (step == 0 && GlobalUtils.writeToLocal(img, message)) {
                                    step++;
                                }
                                if (step == 1 && dbManager.addImage(img)) {
                                    step++;
                                }
                                if (step == 2 && redisManager.pushToQueue(REDIS_QUEUE_NAME, message)) {
                                    success = true;  // 全部步骤成功
                                }
                            } catch (Exception e) {
                                logger.warn("处理步骤失败: step={}, tryTimes={}", step, tryTimes, e);
                            }
                        }
                        // 如果失败，进行补偿处理
                        if (!success) {
                            error = ErrorCode.SERVER_ERROR;
                            // 进行回滚
                            if (step >= 1) {
                                try {
                                    GlobalUtils.deleteFromLocal(img, message);
                                } catch (Exception e) {
                                    logger.warn("回滚本地文件失败", e);
                                }
                            }
                            if (step == 2) {
                                try {
                                    dbManager.delImage(img);
                                } catch (Exception e) {
                                    logger.warn("回滚数据库记录失败", e);
                                }
                            }
                        }
                    }

                }
            }
            // 设置响应头，确保连接正确关闭
            resp.setContentType("application/json;charset=UTF-8");
            resp.setHeader("Connection", "close"); // 强制关闭连接，避免CLOSE_WAIT状态
            resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setHeader("Expires", "0");

            if(error != null) {
                resp.setStatus(HttpServletResponse.SC_OK);
                mapper.writeValue(resp.getOutputStream(), Map.of("ok", false, "cause", error.getMessage()));
            }else {
                resp.setStatus(HttpServletResponse.SC_OK);
                mapper.writeValue(resp.getOutputStream(), Map.of("ok", true, "uid", img.getUID()));
            }

            // 确保输出流被刷新和关闭
            resp.getOutputStream().flush();
        }catch(Exception e) {
            resp.setContentType("application/json;charset=UTF-8");
            resp.setHeader("Connection", "close"); // 异常情况下也强制关闭连接
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                mapper.writeValue(resp.getOutputStream(), Map.of("ok", false, "cause", ErrorCode.SERVER_ERROR.getMessage()));
                resp.getOutputStream().flush();
            } catch (IOException ioException) {
                logger.error("写入响应失败", ioException);
            }
            logger.error("业务处理失败", e);
        }


    }

    private String setUID(int usid) {
        // 防止无限循环：最多尝试100次
        int maxAttempts = 100;
        int attempts = 0;

        while (attempts < maxAttempts) {
            String uuid = UUID.randomUUID().toString().replace("-", ""); // 32 字符
            int start = (int) (Math.random() * 23); // 0~22，保证 substring(10) 不越界
            String uid = uuid.substring(start, start + 10);

            if (!dbManager.isUIDInUser(uid, usid)) {
                return uid;
            }
            attempts++;
        }

        // 如果100次都冲突（概率极低），使用完整UUID
        logger.warn("setUID: 100次尝试后仍有冲突，使用完整UUID");
        return UUID.randomUUID().toString().replace("-", "");
    }

    private ImgType checkImg(byte []data) {

        if(data.length < 3) {
            return ImgType.ERROR;
        }
        if(data[0] == (byte)0x89 && data[1] == 0x50 && data[2] == 0x4E) {
            return ImgType.PNG;
        }else if(data[0] == (byte)0xFF && data[1] == (byte)0xD8 && data[2] == (byte)0xFF) {
            return ImgType.JPEG;
        }else if((data[0] == 0x47 && data[1] == 0x49 && data[2] == 0x46)) {
            return ImgType.GIF;
        }else {
            return ImgType.ERROR;
        }







    }





}

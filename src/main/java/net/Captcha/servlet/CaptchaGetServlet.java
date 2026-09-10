package net.Captcha.servlet;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.Captcha.common.ErrorCode;
import net.Captcha.common.Image;
import net.Captcha.mongod.DBManager;

public class CaptchaGetServlet extends HttpServlet{
    private static final Logger logger = LoggerFactory.getLogger(CaptchaGetServlet.class);
    private static final long serialVersionUID = 1L;
    private final ObjectMapper mapper = new ObjectMapper();
    private DBManager dbManager;

    public CaptchaGetServlet() {
        super();
        this.dbManager = DBManager.getInstance();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ErrorCode error = null;
        int usid = 0;
        String coded = null;
        try {
            String userName = req.getParameter("user");
            String apiKey = req.getParameter("apiKey");
            String uid = req.getParameter("uid");

            // 输入验证：检查参数是否为空或格式不正确
            if(userName == null || apiKey == null || uid == null) {
                error = ErrorCode.NONE_DATA;
            } else if (userName.length() > 100 || apiKey.length() > 200 || uid.length() > 50) {
                error = ErrorCode.NONE_DATA; // 参数过长，可能为恶意输入
            } else if (!uid.matches("^[A-Za-z0-9]+$")) {
                error = ErrorCode.NONE_DATA; // UID格式验证，防止路径遍历攻击
            }
            if(error == null) {
                 usid = dbManager.checkUser(userName, apiKey);
                 if(usid == -1) error = ErrorCode.USER_INDENTITY_FAIL;
             }
            if(error == null) {
                Image image = dbManager.getImage(usid, uid);
                if(image == null) error = ErrorCode.NO_IMAGE_DATA;
                else {
                    String status = image.getStatus();
                    if("I".equals(status)) { // 常量在前，避免NPE
                        error = ErrorCode.NO_UPDATE_YET;
                    }else if("S".equals(status)) {
                        coded = image.getCoded();
                        if(coded == null || !coded.matches("^[A-Za-z0-9]+$")) error = ErrorCode.SERVER_ERROR;
                    }else {
                        error = ErrorCode.SERVER_ERROR;
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
                mapper.writeValue(resp.getOutputStream(), Map.of("ok", true, "coded", coded));
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



}

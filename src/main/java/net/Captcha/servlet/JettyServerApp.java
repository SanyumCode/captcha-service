package net.Captcha.servlet;

import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.Captcha.config.AppConfig;

public class JettyServerApp {
    private static final Logger logger = LoggerFactory.getLogger(JettyServerApp.class);
    public static void main(String[] args) throws Exception {
        // 设置日志文件前缀，区分不同进程的日志
        // 如果未设置，使用默认值"app"
        if (System.getProperty("log.prefix") == null) {
            System.setProperty("log.prefix", "server");
        }

        Server server = new Server();

        // 配置HTTP连接参数，防止CLOSE_WAIT状态
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSendServerVersion(false); // 隐藏服务器版本信息
        httpConfig.setSendXPoweredBy(false); // 隐藏X-Powered-By头
        httpConfig.setRequestHeaderSize(8192); // 请求头大小限制
        httpConfig.setResponseHeaderSize(8192); // 响应头大小限制

        // 创建HTTP连接工厂
        HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfig);

        // 创建一个 Connector
        ServerConnector connector = new ServerConnector(server, httpConnectionFactory);
        connector.setPort(AppConfig.current().httpPort());
        connector.setHost(AppConfig.current().httpHost());

        // 配置连接超时参数，解决TCP连接未关闭问题
        connector.setIdleTimeout(30000); // 30秒空闲超时，自动关闭空闲连接，避免CLOSE_WAIT状态
        connector.setAcceptQueueSize(128); // 设置接受队列大小
        connector.setReuseAddress(true); // 允许地址重用

        // 绑定到 server
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");

        context.addServlet(new ServletHolder(new CaptchaGetServlet()), "/captcha/get");
        context.addServlet(new ServletHolder(new CaptchaPostServlet()), "/captcha/type1");

        server.setHandler(context);
        server.setStopAtShutdown(true);
        server.start();
        logger.info("HTTP server started");
        server.join();
    }
}

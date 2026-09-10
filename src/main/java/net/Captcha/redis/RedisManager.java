package net.Captcha.redis;

import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.Captcha.config.AppConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisManager {
    private static final Logger logger = LoggerFactory.getLogger(RedisManager.class);
    private static RedisManager INSTANCE;
    private static final AppConfig CONFIG = AppConfig.current();
    private JedisPool pool;
    private volatile boolean initialized = false;
    private final Object initLock = new Object();

    private RedisManager() {
        // 延迟初始化，不在构造函数中初始化
    }

    /**
     * 延迟初始化Redis连接池
     * @throws RuntimeException 如果初始化失败
     */
    private void ensureInitialized() {
        if (!initialized) {
            synchronized (initLock) {
                if (!initialized) {
                    try {
                        DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder();
                        if (!CONFIG.redisPassword().isBlank()) {
                            builder.password(CONFIG.redisPassword());
                        }
                        if (CONFIG.redisTls()) {
                            builder.ssl(true);
                            if (CONFIG.redisCaPath().isBlank()) {
                                builder.sslSocketFactory(SSLUtils.defaultSslSocketFactory());
                            } else {
                                builder.sslSocketFactory(
                                        SSLUtils.createSslSocketFactoryWithCa(Path.of(CONFIG.redisCaPath())));
                            }
                        }
                        DefaultJedisClientConfig clientConfig = builder.build();
                        HostAndPort hp = new HostAndPort(CONFIG.redisHost(), CONFIG.redisPort());
                        pool = new JedisPool(hp, clientConfig);
                        pool.setMaxTotal(20);
                        pool.setMaxIdle(20);
                        initialized = true;
                        logger.info("Redis连接池初始化成功");
                    } catch (Exception e) {
                        logger.error("Redis初始化失败，将使用降级模式: {}", e.getClass().getSimpleName());
                        // 不抛出异常，允许程序继续运行，但pool为null
                        // 后续操作会返回false或抛出异常，由调用者处理
                        initialized = false;
                        throw new RuntimeException("Redis初始化失败");
                    }
                }
            }
        }
    }

    public void close() {
        if (pool != null) {
            pool.close();
        }
    }

    public static RedisManager getInstance() {
        if (INSTANCE == null) {
            synchronized (RedisManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new RedisManager();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 检查Redis是否可用
     * @return true如果Redis可用，false否则
     */
    public boolean isAvailable() {
        try {
            ensureInitialized();
            return pool != null;
        } catch (Exception e) {
            return false;
        }
    }
    public boolean ping() {
        try {
            ensureInitialized();
            if (pool == null) {
                return false;
            }
            try (Jedis resource = pool.getResource()) {
                return "PONG".equals(resource.ping());
            }
        } catch (Exception e) {
            logger.error("Redis ping failed", e);
            return false;
        }
    }

    public boolean pushToQueue(String queueName, String message) {
        try {
            ensureInitialized();
            if (pool == null) {
                logger.warn("Redis未初始化，无法推送消息到队列: {}", queueName);
                return false;
            }
            try (Jedis resource = pool.getResource()) {
                long rpush = resource.rpush(queueName, message);
                return rpush >= 0;
            }
        } catch (Exception e) {
            logger.error("Redis push to queue failed: {}", queueName, e);
            return false;
        }
    }

    public String popFromBlockingQueue(String queueName) {
        try {
            ensureInitialized();
            if (pool == null) {
                logger.error("Redis未初始化，无法从队列获取消息: {}", queueName);
                throw new RuntimeException("Redis未初始化，无法从队列获取消息: " + queueName);
            }
            try (Jedis resource = pool.getResource()) {
                List<String> result = resource.blpop(0, queueName);
                if (result != null && result.size() >= 2) {
                    return result.get(1);
                }
                return null;
            }
        } catch (RuntimeException e) {
            throw e; // 重新抛出RuntimeException
        } catch (Exception e) {
            logger.error("Redis pop from blocking queue failed: {}", queueName, e);
            throw new RuntimeException("Failed to pop from queue: " + queueName, e);
        }
    }

}

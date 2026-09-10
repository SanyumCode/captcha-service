package net.Captcha.config;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable runtime configuration sourced from environment variables.
 */
public final class AppConfig {
    private static final AppConfig CURRENT = from(System.getenv());

    private final String mongoUri;
    private final String mongoDatabase;
    private final String redisHost;
    private final int redisPort;
    private final String redisPassword;
    private final boolean redisTls;
    private final String redisCaPath;
    private final Path imageRoot;
    private final String httpHost;
    private final int httpPort;

    private AppConfig(Map<String, String> environment) {
        mongoUri = value(environment, "MONGO_URI", "mongodb://localhost:27017");
        mongoDatabase = value(environment, "MONGO_DATABASE", "captcha");
        redisHost = value(environment, "REDIS_HOST", "localhost");
        redisPort = port(environment, "REDIS_PORT", 6379);
        redisPassword = value(environment, "REDIS_PASSWORD", "");
        redisTls = Boolean.parseBoolean(value(environment, "REDIS_TLS", "false"));
        redisCaPath = value(environment, "REDIS_CA_PATH", "");
        imageRoot = Path.of(value(environment, "IMAGE_ROOT", "data/images")).toAbsolutePath().normalize();
        httpHost = value(environment, "HTTP_HOST", "127.0.0.1");
        httpPort = port(environment, "HTTP_PORT", 8080);
    }

    public static AppConfig current() {
        return CURRENT;
    }

    public static AppConfig from(Map<String, String> environment) {
        return new AppConfig(Objects.requireNonNull(environment, "environment"));
    }

    private static String value(Map<String, String> environment, String key, String fallback) {
        String configured = environment.get(key);
        return configured == null || configured.isBlank() ? fallback : configured.trim();
    }

    private static int port(Map<String, String> environment, String key, int fallback) {
        String configured = value(environment, key, Integer.toString(fallback));
        try {
            int parsed = Integer.parseInt(configured);
            if (parsed < 1 || parsed > 65535) {
                throw new IllegalArgumentException(key + " must be between 1 and 65535");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(key + " must be a valid port", exception);
        }
    }

    public String mongoUri() { return mongoUri; }
    public String mongoDatabase() { return mongoDatabase; }
    public String redisHost() { return redisHost; }
    public int redisPort() { return redisPort; }
    public String redisPassword() { return redisPassword; }
    public boolean redisTls() { return redisTls; }
    public String redisCaPath() { return redisCaPath; }
    public Path imageRoot() { return imageRoot; }
    public String httpHost() { return httpHost; }
    public int httpPort() { return httpPort; }
}

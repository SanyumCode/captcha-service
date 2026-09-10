package net.Captcha.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AppConfigTest {
    @Test
    void providesSafeLocalDefaults() {
        AppConfig config = AppConfig.from(Map.of());

        assertEquals("mongodb://localhost:27017", config.mongoUri());
        assertEquals("captcha", config.mongoDatabase());
        assertEquals("localhost", config.redisHost());
        assertEquals(6379, config.redisPort());
        assertEquals("", config.redisPassword());
        assertFalse(config.redisTls());
        assertEquals("127.0.0.1", config.httpHost());
        assertEquals(8080, config.httpPort());
        assertEquals(Path.of("data/images").toAbsolutePath().normalize(), config.imageRoot());
    }

    @Test
    void readsEnvironmentOverrides() {
        AppConfig config = AppConfig.from(Map.of(
                "MONGO_URI", "mongodb://db.internal:27017",
                "MONGO_DATABASE", "captcha_test",
                "REDIS_HOST", "cache.internal",
                "REDIS_PORT", "6380",
                "REDIS_PASSWORD", "unit-test-placeholder",
                "REDIS_TLS", "true",
                "REDIS_CA_PATH", "/tmp/test-ca.pem",
                "IMAGE_ROOT", "build/test-images",
                "HTTP_HOST", "0.0.0.0",
                "HTTP_PORT", "9090"));

        assertEquals("captcha_test", config.mongoDatabase());
        assertEquals("cache.internal", config.redisHost());
        assertEquals(6380, config.redisPort());
        assertTrue(config.redisTls());
        assertEquals(9090, config.httpPort());
    }

    @Test
    void rejectsInvalidPort() {
        assertThrows(IllegalArgumentException.class,
                () -> AppConfig.from(Map.of("HTTP_PORT", "70000")));
    }
}

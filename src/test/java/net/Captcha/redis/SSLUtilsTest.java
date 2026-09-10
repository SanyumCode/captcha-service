package net.Captcha.redis;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class SSLUtilsTest {
    @Test
    void exposesJvmDefaultTruststoreFactory() {
        assertNotNull(SSLUtils.defaultSslSocketFactory());
    }
}

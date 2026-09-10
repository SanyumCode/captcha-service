package net.Captcha.Utils;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ApiKeyGeneraterTest {
    @Test
    void generatesDistinctUuidKeys() {
        String first = ApiKeyGenerater.getApiKey();
        String second = ApiKeyGenerater.getApiKey();

        assertTrue(first.matches("[0-9a-f-]{36}"));
        assertNotEquals(first, second);
    }
}

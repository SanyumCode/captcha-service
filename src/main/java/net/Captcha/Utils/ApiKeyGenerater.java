package net.Captcha.Utils;

import java.util.UUID;

public class ApiKeyGenerater {
    private ApiKeyGenerater(){

    }
    public static String getApiKey() {
        return UUID.randomUUID().toString();
    }

}

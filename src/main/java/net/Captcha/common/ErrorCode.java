package net.Captcha.common;

public enum ErrorCode {
    DECODE_FAIL("An error occurred during the decoding of Base64."),
    TYPE_FAIL("Incorrect image format.(Accepted format: png, jpeg, gif)"),
    NONE_DATA("Some fields are illegal, please check your request..."),
    USER_INDENTITY_FAIL("User verification failed. Please check the user and the key."),
    SERVER_ERROR("Some errors occured in Sever, Please try again later..."),
    NO_IMAGE_DATA("This image does not exist. Please check the uid or re-upload the image."),
    NO_UPDATE_YET("The content you requested has not been updated yet. Please try again later.");


    public final String message;

    ErrorCode(String content){
        this.message = content;
    }

    public String getMessage() {
        return message;
    }

}

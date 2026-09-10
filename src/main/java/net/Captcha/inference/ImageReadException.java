package net.Captcha.inference;

public class ImageReadException extends Exception {
    private static final long serialVersionUID = 1L;

    public ImageReadException() {
        super();
    }

    public ImageReadException(String message) {
        super(message);
    }

    public ImageReadException(String message, Throwable cause) {
        super(message, cause);
    }
}

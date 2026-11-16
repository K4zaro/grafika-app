package grafika.image;

import java.io.IOException;

public class ImageFormatException extends IOException {
    private static final long serialVersionUID = 1L;

    public ImageFormatException(String message) {
        super(message);
    }

    public ImageFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}

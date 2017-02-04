package uk.tanton.streaming.lambda.transcoder;

public class TranscodeException extends RuntimeException {
    public TranscodeException() {
    }

    public TranscodeException(Throwable cause) {
        super(cause);
    }

    public TranscodeException(String message) {
        super(message);
    }

    public TranscodeException(String message, Throwable cause) {
        super(message, cause);
    }

    public TranscodeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

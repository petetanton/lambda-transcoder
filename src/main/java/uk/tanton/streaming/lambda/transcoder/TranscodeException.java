package uk.tanton.streaming.lambda.transcoder;

public class TranscodeException extends RuntimeException {

    public TranscodeException(Throwable cause) {
        super(cause);
    }

    public TranscodeException(String message, Throwable cause) {
        super(message, cause);
    }

}

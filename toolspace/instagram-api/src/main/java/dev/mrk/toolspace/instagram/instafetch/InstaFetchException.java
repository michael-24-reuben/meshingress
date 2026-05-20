package dev.mrk.toolspace.instagram.instafetch;

public class InstaFetchException extends RuntimeException {
    public InstaFetchException(String message) {
        super(message);
    }

    public InstaFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}

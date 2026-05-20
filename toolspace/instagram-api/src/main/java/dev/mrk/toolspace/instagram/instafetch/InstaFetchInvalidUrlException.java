package dev.mrk.toolspace.instagram.instafetch;

public class InstaFetchInvalidUrlException extends InstaFetchException {
    public InstaFetchInvalidUrlException() {
        this("Instagram URL or shortcode is invalid");
    }

    public InstaFetchInvalidUrlException(String message) {
        super(message);
    }
}

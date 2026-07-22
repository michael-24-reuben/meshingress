package dev.mrk.toolspace.openinklibrary;

public final class SourceException extends RuntimeException {
    private final String code;

    public SourceException(String code, String message) {
        super(message);
        this.code = code;
    }

    public SourceException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}

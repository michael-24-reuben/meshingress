package dev.mrk.toolspace.youtube.data;

public final class YoutubeDataApiException extends RuntimeException {
    private final String code;

    YoutubeDataApiException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}

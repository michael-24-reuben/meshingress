package dev.mrk.toolspace.ytdlp;

final class YtDlpException extends Exception {
    private final String code;

    YtDlpException(String code, String message) {
        super(message);
        this.code = code;
    }

    String code() {
        return code;
    }
}

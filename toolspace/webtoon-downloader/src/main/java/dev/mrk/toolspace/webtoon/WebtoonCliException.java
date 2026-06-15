package dev.mrk.toolspace.webtoon;

final class WebtoonCliException extends Exception {

    private final WebtoonDownloaderCommandResult result;

    WebtoonCliException(String message, WebtoonDownloaderCommandResult result) {
        super(message);
        this.result = result;
    }

    WebtoonDownloaderCommandResult result() {
        return result;
    }
}

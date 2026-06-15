package dev.mrk.toolspace.webtoon;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

final class ProcessWebtoonDownloaderRunner implements WebtoonDownloaderRunner {

    @Override
    public WebtoonDownloaderCommandResult run(List<String> command, Path workingDirectory, Duration timeout)
            throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());

        Process process = builder.start();
        CompletableFuture<String> stdout = CompletableFuture.supplyAsync(() -> read(process.getInputStream()));
        CompletableFuture<String> stderr = CompletableFuture.supplyAsync(() -> read(process.getErrorStream()));
        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
        }

        int exitCode = finished ? process.exitValue() : -1;
        return new WebtoonDownloaderCommandResult(exitCode, get(stdout), get(stderr), !finished, List.copyOf(command));
    }

    private String read(java.io.InputStream stream) {
        try {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return exception.getMessage() == null ? "" : exception.getMessage();
        }
    }

    private String get(CompletableFuture<String> future) throws InterruptedException {
        try {
            return future.get();
        } catch (ExecutionException exception) {
            return exception.getMessage() == null ? "" : exception.getMessage();
        }
    }
}

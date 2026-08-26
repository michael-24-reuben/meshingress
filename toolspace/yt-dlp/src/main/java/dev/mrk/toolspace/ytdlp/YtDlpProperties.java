package dev.mrk.toolspace.ytdlp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "meshingress.ytdlp")
public record YtDlpProperties(
        String pythonExecutable,
        String nodeRuntime,
        String[] impersonateClients,
        String[] impersonateHosts,
        String moduleDirectory,
        String downloadDirectory,
        Long timeoutMs,
        Integer maxOutputChars
) {
    public YtDlpProperties {
        pythonExecutable = blankOr(pythonExecutable, "python");
        nodeRuntime = blankOr(nodeRuntime, "node");
        impersonateClients = blankOr(impersonateClients, new String[]{});
        impersonateHosts = blankOr(impersonateHosts, new String[]{});
        moduleDirectory = blankOr(moduleDirectory, "toolspace/x-yt-dlp");
        downloadDirectory = blankOr(downloadDirectory, "runtime/downloads");
        timeoutMs = timeoutMs == null || timeoutMs <= 0 ? 300_000L : Math.min(timeoutMs, 900_000L);
        maxOutputChars = maxOutputChars == null || maxOutputChars <= 0 ? 4_000_000 : Math.min(maxOutputChars, 16_000_000);
    }

    private static String blankOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
    private static String[] blankOr(String[] value, String[] fallback) {
        return value == null || value.length == 0 ? fallback : value;
    }
}

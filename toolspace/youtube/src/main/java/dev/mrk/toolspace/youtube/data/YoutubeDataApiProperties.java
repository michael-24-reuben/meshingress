package dev.mrk.toolspace.youtube.data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Local production configuration for API-key-backed public YouTube Data API operations. */
@ConfigurationProperties(prefix = "meshingress.youtube")
public record YoutubeDataApiProperties(
        String apiKey,
        String dataApiBaseUrl,
        Long requestTimeoutMs
) {
    public static final String DEFAULT_BASE_URL = "https://www.googleapis.com/youtube/v3";

    public YoutubeDataApiProperties {
        apiKey = apiKey == null ? "" : apiKey.strip();
        dataApiBaseUrl = normalizeBaseUrl(dataApiBaseUrl);
        requestTimeoutMs = requestTimeoutMs == null || requestTimeoutMs <= 0
                ? 30_000L
                : Math.min(requestTimeoutMs, 120_000L);
    }

    private static String normalizeBaseUrl(String value) {
        String normalized = value == null || value.isBlank() ? DEFAULT_BASE_URL : value.strip();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}

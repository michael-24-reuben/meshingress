package dev.mrk.toolspace.webtoon;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record WebtoonDownloadSeriesArgs(
        @McpInputField(value = "url", description = "Public WEBTOON series URL.", required = true)
        String url,

        @McpInputField(value = "outputSubdirectory", description = "Optional relative output folder under the configured Webtoon output root.", required = false)
        String outputSubdirectory,

        @McpInputField(value = "start", description = "Optional first chapter number.", required = false)
        Integer start,

        @McpInputField(value = "end", description = "Optional final chapter number.", required = false)
        Integer end,

        @McpInputField(value = "latest", description = "Download only the latest chapter. Cannot be combined with start or end.", required = false)
        Boolean latest,

        @McpInputField(value = "saveAs", description = "Storage format: images, zip, cbz, or pdf. Defaults to cbz.", required = false)
        String saveAs,

        @McpInputField(value = "separate", description = "Store each chapter in a separate folder. Valid only with saveAs=images.", required = false)
        Boolean separate,

        @McpInputField(value = "imageFormat", description = "Optional image format: jpg or png.", required = false)
        String imageFormat,

        @McpInputField(value = "quality", description = "Optional quality: 40, 50, 60, 70, 80, 90, or 100.", required = false)
        Integer quality,

        @McpInputField(value = "exportMetadata", description = "Also export metadata during the download.", required = false)
        Boolean exportMetadata,

        @McpInputField(value = "exportFormat", description = "Metadata format when exportMetadata is true: json, text, or all.", required = false)
        String exportFormat,

        @McpInputField(value = "retryStrategy", description = "Retry strategy: exponential, linear, fixed, or none. Defaults to exponential.", required = false)
        String retryStrategy,

        @McpInputField(value = "concurrentChapters", description = "Chapter concurrency, capped by configuration.", required = false)
        Integer concurrentChapters,

        @McpInputField(value = "concurrentPages", description = "Page concurrency, capped by configuration.", required = false)
        Integer concurrentPages,

        @McpInputField(value = "debug", description = "Enable upstream CLI debug logging.", required = false)
        Boolean debug
) {
}

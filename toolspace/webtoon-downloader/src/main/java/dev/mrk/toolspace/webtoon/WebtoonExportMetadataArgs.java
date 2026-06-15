package dev.mrk.toolspace.webtoon;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record WebtoonExportMetadataArgs(
        @McpInputField(value = "url", description = "Public WEBTOON series URL.", required = true)
        String url,

        @McpInputField(value = "outputSubdirectory", description = "Optional relative output folder under the configured Webtoon output root.", required = false)
        String outputSubdirectory,

        @McpInputField(value = "exportFormat", description = "Metadata format: json, text, or all. Defaults to json.", required = false)
        String exportFormat,

        @McpInputField(value = "start", description = "Optional first chapter number.", required = false)
        Integer start,

        @McpInputField(value = "end", description = "Optional final chapter number.", required = false)
        Integer end,

        @McpInputField(value = "latest", description = "Download/export only the latest chapter. Cannot be combined with start or end.", required = false)
        Boolean latest,

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

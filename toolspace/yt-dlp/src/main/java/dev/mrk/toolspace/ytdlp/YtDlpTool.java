package dev.mrk.toolspace.ytdlp;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.tools.annotation.McpConfigureMapping;
import dev.mrk.meshingress.api.tools.annotation.McpFunction;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.api.tools.annotation.McpToolScopes;
import dev.mrk.meshingress.scopes.McpToolScope;

@McpTool(value = "ytdlp", title = "yt-dlp", description = "Inspect and locally download media through the vendored yt-dlp runtime.")
@McpToolScopes({McpToolScope.PROCESS_EXECUTE, McpToolScope.NETWORK_OUTBOUND})
public final class YtDlpTool {
    private final YtDlpBackend backend;

    YtDlpTool(YtDlpBackend backend) {
        this.backend = backend;
    }

    @McpConfigureMapping(timeoutMs = 60_000)
    @McpFunction(value = "inspect", title = "Inspect media", description = "Return yt-dlp metadata for one HTTP or HTTPS media URL without downloading media bytes.")
    public DispatchExecutionResult inspect(YtDlpInspectArgs arguments, McpCallContext context) {
        try {
            return DispatchExecutionResult.builder()
                    .object(backend.inspect(arguments == null ? null : arguments.url()))
                    .status("completed")
                    .summary("yt-dlp metadata inspection completed.")
                    .build();
        } catch (YtDlpException exception) {
            return failure(exception);
        }
    }

    @McpConfigureMapping(timeoutMs = 60_000)
    @McpFunction(value = "formats", title = "List formats", description = "Return structured video and audio format entries for one media URL without downloading media bytes.")
    public DispatchExecutionResult formats(YtDlpInspectArgs arguments, McpCallContext context) {
        try {
            return completed(backend.formats(arguments == null ? null : arguments.url()), "yt-dlp format listing completed.");
        } catch (YtDlpException exception) {
            return failure(exception);
        }
    }

    @McpConfigureMapping(timeoutMs = 60_000)
    @McpFunction(value = "subtitles-list", title = "List subtitles", description = "Return available manual and automatic subtitle tracks without downloading media bytes.")
    public DispatchExecutionResult subtitles(YtDlpInspectArgs arguments, McpCallContext context) {
        try {
            return completed(backend.subtitles(arguments == null ? null : arguments.url()), "yt-dlp subtitle listing completed.");
        } catch (YtDlpException exception) {
            return failure(exception);
        }
    }

    @McpToolScopes(McpToolScope.FILES_WRITE)
    @McpConfigureMapping(timeoutMs = 300_000)
    @McpFunction(value = "subtitles-download", title = "Download subtitles", description = "Download selected subtitle tracks into the local x-yt-dlp runtime directory without downloading media bytes.")
    public DispatchExecutionResult downloadSubtitles(YtDlpSubtitleDownloadArgs arguments, McpCallContext context) {
        try {
            return completed(backend.downloadSubtitles(
                    arguments == null ? null : arguments.url(),
                    arguments == null ? null : arguments.languages(),
                    arguments == null ? null : arguments.format(),
                    arguments != null && Boolean.TRUE.equals(arguments.includeAutomaticCaptions())
            ), "yt-dlp subtitle download completed.");
        } catch (YtDlpException exception) {
            return failure(exception);
        }
    }

    @McpConfigureMapping(timeoutMs = 60_000)
    @McpFunction(value = "thumbnails-list", title = "List thumbnails", description = "Return available thumbnail entries without downloading media bytes.")
    public DispatchExecutionResult thumbnails(YtDlpInspectArgs arguments, McpCallContext context) {
        try {
            return completed(backend.thumbnails(arguments == null ? null : arguments.url()), "yt-dlp thumbnail listing completed.");
        } catch (YtDlpException exception) {
            return failure(exception);
        }
    }

    @McpToolScopes(McpToolScope.FILES_WRITE)
    @McpConfigureMapping(timeoutMs = 300_000)
    @McpFunction(value = "thumbnails-download", title = "Download thumbnails", description = "Download selected thumbnails into the local x-yt-dlp runtime directory without downloading media bytes.")
    public DispatchExecutionResult downloadThumbnails(YtDlpThumbnailDownloadArgs arguments, McpCallContext context) {
        try {
            return completed(backend.downloadThumbnails(
                    arguments == null ? null : arguments.url(),
                    arguments != null && Boolean.TRUE.equals(arguments.all())
            ), "yt-dlp thumbnail download completed.");
        } catch (YtDlpException exception) {
            return failure(exception);
        }
    }

    @McpToolScopes(McpToolScope.FILES_WRITE)
    @McpConfigureMapping(timeoutMs = 300_000)
    @McpFunction(value = "download", title = "Download media", description = "Download one media URL into the local x-yt-dlp runtime directory.")
    public DispatchExecutionResult download(YtDlpDownloadArgs arguments, McpCallContext context) {
        try {
            return DispatchExecutionResult.builder()
                    .object(backend.download(arguments == null ? null : arguments.url(), arguments == null ? null : arguments.format()))
                    .status("completed")
                    .summary("yt-dlp download completed.")
                    .build();
        } catch (YtDlpException exception) {
            return failure(exception);
        }
    }

    private static DispatchExecutionResult failure(YtDlpException exception) {
        return DispatchExecutionResult.builder()
                .error(exception.code(), exception.getMessage())
                .status("failed")
                .summary("yt-dlp request failed.")
                .build();
    }

    private static DispatchExecutionResult completed(tools.jackson.databind.JsonNode result, String summary) {
        return DispatchExecutionResult.builder()
                .object(result)
                .status("completed")
                .summary(summary)
                .build();
    }
}

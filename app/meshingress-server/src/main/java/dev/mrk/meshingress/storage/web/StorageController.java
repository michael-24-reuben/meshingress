package dev.mrk.meshingress.storage.web;

import dev.mrk.meshingress.storage.workspace.WorkspaceFileRecord;
import dev.mrk.meshingress.storage.workspace.WorkspaceRetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/v1/storage")
@ConditionalOnProperty(prefix = "meshingress.storage", name = "enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "Workspace storage", description = "Retrieve files published by a Meshingress tool workspace.")
public class StorageController {
    private final WorkspaceRetrievalService retrieval;

    public StorageController(WorkspaceRetrievalService retrieval) { this.retrieval = retrieval; }

    @GetMapping("/{sessionId}/{requestId}/files/{*relativePath}")
    @Operation(summary = "Download a workspace file", description = "Streams a published workspace file as an attachment. Byte-range retrieval is not supported.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File stream with content type, length, attachment filename, no-store cache policy, and nosniff headers."),
            @ApiResponse(responseCode = "404", description = "The workspace or requested file is unavailable."),
            @ApiResponse(responseCode = "416", description = "A Range header was supplied; partial-content retrieval is not supported.")
    })
    public ResponseEntity<StreamingResponseBody> get(
            @Parameter(description = "Workspace session identifier.") @PathVariable String sessionId,
            @Parameter(description = "Workspace request identifier.") @PathVariable String requestId,
            @Parameter(description = "Relative path of the file inside the workspace.") @PathVariable String relativePath,
            @Parameter(in = ParameterIn.HEADER, description = "Not supported. Supplying this header returns 416.") @RequestHeader(value = HttpHeaders.RANGE, required = false) String range
    ) {
        if (range != null && !range.isBlank()) throw new StorageRangeNotSupportedException();
        var handle = retrieval.open(sessionId, requestId, path(relativePath));
        WorkspaceFileRecord entry = handle.file();
        StreamingResponseBody body = output -> {
            boolean completed = false;
            try {
                handle.input().transferTo(output);
                completed = true;
            } finally {
                try { handle.input().close(); } finally { handle.close().run(); }
            }
        };
        return response(entry).body(body);
    }

    @RequestMapping(value = "/{sessionId}/{requestId}/files/{*relativePath}", method = RequestMethod.HEAD)
    @Operation(summary = "Inspect a workspace file", description = "Returns the file metadata headers without transferring the file body.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File metadata headers, including content type, length, and attachment filename."),
            @ApiResponse(responseCode = "404", description = "The workspace or requested file is unavailable.")
    })
    public ResponseEntity<Void> head(
            @Parameter(description = "Workspace session identifier.") @PathVariable String sessionId,
            @Parameter(description = "Workspace request identifier.") @PathVariable String requestId,
            @Parameter(description = "Relative path of the file inside the workspace.") @PathVariable String relativePath
    ) {
        return response(retrieval.inspect(sessionId, requestId, path(relativePath))).build();
    }

    private ResponseEntity.BodyBuilder response(WorkspaceFileRecord entry) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(entry.mimeType()))
                .contentLength(entry.byteSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + entry.relativePath().substring(entry.relativePath().lastIndexOf('/') + 1).replace("\"", "_") + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff");
    }

    private String path(String relativePath) {
        return relativePath != null && relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
    }
}

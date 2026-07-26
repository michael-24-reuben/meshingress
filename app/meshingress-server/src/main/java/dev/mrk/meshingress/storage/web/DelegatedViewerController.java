package dev.mrk.meshingress.storage.web;

import dev.mrk.meshingress.storage.workspace.DelegatedViewerService;
import dev.mrk.meshingress.storage.config.DelegatedSourceTargetConfiguredCondition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/v1/storage/delegated")
@Conditional(DelegatedSourceTargetConfiguredCondition.class)
public final class DelegatedViewerController {
    private final DelegatedViewerService viewer;

    public DelegatedViewerController(DelegatedViewerService viewer) { this.viewer = viewer; }

    @GetMapping("/{token}/files/{*relativePath}")
    public ResponseEntity<StreamingResponseBody> get(@PathVariable String token, @PathVariable String relativePath,
                                                       @RequestHeader(value = HttpHeaders.RANGE, required = false) String range) {
        if (range != null && !range.isBlank()) throw new StorageRangeNotSupportedException();
        DelegatedViewerService.OpenFile file = viewer.open(token, path(relativePath));
        StreamingResponseBody body = output -> {
            try (var input = file.input()) { input.transferTo(output); }
        };
        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.filename() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .header("X-Content-Type-Options", "nosniff");
        if (file.byteSize() >= 0) response.contentLength(file.byteSize());
        return response.body(body);
    }

    private static String path(String relativePath) { return relativePath != null && relativePath.startsWith("/") ? relativePath.substring(1) : relativePath; }
}

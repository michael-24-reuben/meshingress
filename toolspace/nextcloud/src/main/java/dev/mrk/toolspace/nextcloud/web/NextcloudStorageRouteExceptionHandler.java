package dev.mrk.toolspace.nextcloud.web;

import dev.mrk.meshingress.api.storage.ToolStorageException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = DelegatedViewerController.class)
public class NextcloudStorageRouteExceptionHandler {
    @ExceptionHandler(ToolStorageException.class)
    ResponseEntity<Void> unavailable() { return ResponseEntity.notFound().build(); }

    @ExceptionHandler(NextcloudStorageRangeNotSupportedException.class)
    ResponseEntity<Void> range() { return ResponseEntity.status(416).build(); }
}

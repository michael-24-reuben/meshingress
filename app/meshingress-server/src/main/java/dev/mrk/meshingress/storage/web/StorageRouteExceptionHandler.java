package dev.mrk.meshingress.storage.web;

import dev.mrk.meshingress.api.storage.ToolStorageException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = StorageController.class)
public class StorageRouteExceptionHandler {
    @ExceptionHandler(ToolStorageException.class)
    ResponseEntity<Void> unavailable() { return ResponseEntity.notFound().build(); }

    @ExceptionHandler(StorageRangeNotSupportedException.class)
    ResponseEntity<Void> range() { return ResponseEntity.status(416).build(); }
}

package dev.mrk.meshingress.controller.auth;

import dev.mrk.meshingress.auth.AuthStoreException;
import dev.mrk.meshingress.auth.AuthStoreRegistry;
import dev.mrk.meshingress.auth.FirstAdminRegistrationCommand;
import dev.mrk.meshingress.auth.FirstAdminRegistrationResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/bootstrap")
public class BootstrapAdminController {

    private final AuthStoreRegistry authStoreRegistry;

    public BootstrapAdminController(AuthStoreRegistry authStoreRegistry) {
        this.authStoreRegistry = authStoreRegistry;
    }

    @PostMapping("/admin")
    public ResponseEntity<FirstAdminRegistrationResult> registerFirstAdmin(
            @RequestBody FirstAdminRegistrationRequest request
    ) {
        FirstAdminRegistrationResult result = authStoreRegistry.registerFirstAdmin(new FirstAdminRegistrationCommand(
                request.username(),
                request.bootstrapPassword(),
                request.newPassword(),
                request.confirmationPassword(),
                request.email(),
                request.displayName()
        ));
        return ResponseEntity.status(201).body(result);
    }

    @ExceptionHandler(AuthStoreException.class)
    public ResponseEntity<ErrorResponse> handleAuthStoreException(AuthStoreException exception) {
        return ResponseEntity
                .status(exception.status())
                .body(new ErrorResponse(exception.getMessage()));
    }

    public record FirstAdminRegistrationRequest(
            String username,
            String bootstrapPassword,
            String newPassword,
            String confirmationPassword,
            String email,
            String displayName
    ) {
    }

    public record ErrorResponse(String error) {
    }
}

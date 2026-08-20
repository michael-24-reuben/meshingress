package dev.mrk.meshingress.controller.auth;

import dev.mrk.meshingress.config.MeshingressProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Temporary browser-to-server validation for the future native-login UI.
 *
 * <p>This deliberately does not authenticate a user or issue a session. A
 * production native credential verifier needs a password store, Argon2id,
 * throttling, recovery, and MFA before it can become an authentication route.</p>
 */
@RestController
@RequestMapping("/api/v1/auth/native")
public final class NativeLoginValidationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(NativeLoginValidationController.class);
    private final MeshingressProperties properties;

    public NativeLoginValidationController(MeshingressProperties properties) {
        this.properties = properties;
    }

    @PostMapping("/validate")
    public ResponseEntity<NativeLoginValidationResponse> validate(
            @RequestBody NativeLoginValidationRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        if (!"dev".equalsIgnoreCase(properties.security().mode())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (request == null || request.username() == null || request.username().isBlank()
                || request.password() == null || request.password().isBlank()) {
            return ResponseEntity.badRequest().body(new NativeLoginValidationResponse(
                    false,
                    "Username and password are required for the development validation.",
                    null,
                    null
            ));
        }

        String effectiveRequestId = requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
        String identifierFingerprint = fingerprint(request.username().trim());
        // Deliberately omit password and password-derived material from this audit event.
        LOGGER.info("Native login development validation accepted: identifierFingerprint={}, requestId={}",
                identifierFingerprint, effectiveRequestId);
        return ResponseEntity.ok(new NativeLoginValidationResponse(
                true,
                "Native credential submission reached Meshingress. No session was issued.",
                identifierFingerprint,
                effectiveRequestId
        ));
    }

    private static String fingerprint(String username) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(username.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 12);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record NativeLoginValidationRequest(String username, String password) {
    }

    public record NativeLoginValidationResponse(
            boolean accepted,
            String message,
            String identifierFingerprint,
            String requestId
    ) {
    }
}

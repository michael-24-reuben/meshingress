package dev.mrk.meshingress.auth;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.autoconfigure.SecurityProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class JsonAuthStoreRegistry implements AuthStoreRegistry {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ObjectMapper objectMapper;
    private final SecurityProperties securityProperties;
    private final Path storePath;
    private final PasswordEncoder passwordEncoder;

    public JsonAuthStoreRegistry(
            ObjectMapper objectMapper,
            SecurityProperties securityProperties,
            @Value("${meshingress.auth.store.path:data/mcp-auth-store.json}") String storePath
    ) {
        this.objectMapper = objectMapper;
        this.securityProperties = securityProperties;
        this.storePath = Path.of(storePath);
        this.passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @PostConstruct
    public void initialize() {
        synchronized (this) {
            ensureStore();
        }
    }

    @Override
    public synchronized BootstrapAdminState bootstrapAdmin() {
        JsonAuthStoreDocument document = readDocument();
        JsonAuthStoreDocument.Bootstrap bootstrap = document.getBootstrap();
        return new BootstrapAdminState(
                bootstrap.getUsername(),
                bootstrap.getPassword(),
                bootstrap.getSource(),
                bootstrap.getCreatedAt(),
                bootstrap.getConsumedAt()
        );
    }

    @Override
    public synchronized boolean hasAdmin() {
        return !readDocument().getAdmins().isEmpty();
    }

    @Override
    public synchronized FirstAdminRegistrationResult registerFirstAdmin(FirstAdminRegistrationCommand command) {
        JsonAuthStoreDocument document = readDocument();
        if (!document.getAdmins().isEmpty()) {
            throw new AuthStoreException(HttpStatus.CONFLICT, "First admin is already registered");
        }

        String username = requireText(command.username(), "username");
        String bootstrapPassword = requireText(command.bootstrapPassword(), "bootstrapPassword");
        JsonAuthStoreDocument.Bootstrap bootstrap = document.getBootstrap();
        if (bootstrap.getPassword() == null || !Objects.equals(bootstrap.getPassword(), bootstrapPassword)) {
            throw new AuthStoreException(HttpStatus.FORBIDDEN, "Invalid bootstrap password");
        }

        String finalPassword = bootstrapPassword;
        String passwordSource = "spring-security-generated";
        if (hasText(command.newPassword())) {
            if (!Objects.equals(command.newPassword(), command.confirmationPassword())) {
                throw new AuthStoreException(HttpStatus.BAD_REQUEST, "confirmationPassword must match newPassword");
            }
            finalPassword = command.newPassword();
            passwordSource = "user-provided";
        }

        String now = now();
        JsonAuthStoreDocument.AdminUser admin = new JsonAuthStoreDocument.AdminUser();
        admin.setId(UUID.randomUUID().toString());
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(finalPassword));
        admin.setEmail(blankToNull(command.email()));
        admin.setDisplayName(blankToNull(command.displayName()));
        admin.setCreatedAt(now);
        admin.setPasswordSource(passwordSource);

        GeneratedMcpAuth generatedAuth = new GeneratedMcpAuth(
                token("access"),
                token("secret"),
                token("auth"),
                "admin-" + username,
                username
        );

        JsonAuthStoreDocument.McpCredential credential = new JsonAuthStoreDocument.McpCredential();
        credential.setId(UUID.randomUUID().toString());
        credential.setAccessToken(generatedAuth.accessToken());
        credential.setSecretKey(generatedAuth.secretKey());
        credential.setAuthToken(generatedAuth.authToken());
        credential.setClientId(generatedAuth.clientId());
        credential.setSubject(generatedAuth.subject());
        credential.setCreatedAt(now);
        credential.setCreatedByAdminId(admin.getId());
        credential.setEnabled(true);
        credential.setAdmin(true);

        document.getAdmins().add(admin);
        document.getMcpCredentials().add(credential);
        bootstrap.setConsumedAt(now);
        bootstrap.setPassword(null);
        writeDocument(document);

        return new FirstAdminRegistrationResult(
                admin.getId(),
                admin.getUsername(),
                admin.getEmail(),
                admin.getDisplayName(),
                generatedAuth
        );
    }

    @Override
    public synchronized Optional<McpAuthenticatedSession> validateMcpCredentials(
            String authorization,
            String secretKey,
            String authToken
    ) {
        if (!hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String accessToken = authorization.substring("Bearer ".length());
        return readDocument().getMcpCredentials().stream()
                .filter(JsonAuthStoreDocument.McpCredential::isEnabled)
                .filter(credential -> Objects.equals(credential.getAccessToken(), accessToken))
                .filter(credential -> Objects.equals(credential.getSecretKey(), secretKey))
                .filter(credential -> Objects.equals(credential.getAuthToken(), authToken))
                .findFirst()
                .map(credential -> new McpAuthenticatedSession(
                        credential.getClientId(),
                        credential.getSubject()
                ));
    }

    @Override
    public synchronized boolean isAdminBearerToken(String authorization) {
        if (!hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return false;
        }
        String accessToken = authorization.substring("Bearer ".length());
        return readDocument().getMcpCredentials().stream()
                .filter(JsonAuthStoreDocument.McpCredential::isEnabled)
                .filter(JsonAuthStoreDocument.McpCredential::isAdmin)
                .anyMatch(credential -> Objects.equals(credential.getAccessToken(), accessToken));
    }

    private void ensureStore() {
        if (Files.exists(storePath)) {
            return;
        }
        JsonAuthStoreDocument document = new JsonAuthStoreDocument();
        JsonAuthStoreDocument.Bootstrap bootstrap = new JsonAuthStoreDocument.Bootstrap();
        bootstrap.setUsername(securityProperties.getUser().getName());
        bootstrap.setPassword(securityProperties.getUser().getPassword());
        bootstrap.setSource("spring-security-generated");
        bootstrap.setCreatedAt(now());
        document.setBootstrap(bootstrap);
        writeDocument(document);
    }

    private JsonAuthStoreDocument readDocument() {
        ensureStore();
        try {
            return objectMapper.readValue(storePath, JsonAuthStoreDocument.class);
        } catch (JacksonException exception) {
            throw new AuthStoreException(HttpStatus.INTERNAL_SERVER_ERROR, "Auth store JSON is invalid");
        }
    }

    private void writeDocument(JsonAuthStoreDocument document) {
        try {
            Path parent = storePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storePath, document);
        } catch (JacksonException exception) {
            throw new AuthStoreException(HttpStatus.INTERNAL_SERVER_ERROR, "Auth store JSON could not be written");
        } catch (IOException exception) {
            throw new AuthStoreException(HttpStatus.INTERNAL_SERVER_ERROR, "Auth store path could not be created");
        }
    }

    private static String requireText(String value, String name) {
        if (!hasText(value)) {
            throw new AuthStoreException(HttpStatus.BAD_REQUEST, name + " is required");
        }
        return value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static String token(String prefix) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return prefix + "-" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String now() {
        return OffsetDateTime.now().toString();
    }
}

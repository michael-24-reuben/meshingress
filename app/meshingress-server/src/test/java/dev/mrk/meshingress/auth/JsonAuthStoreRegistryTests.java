package dev.mrk.meshingress.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.security.autoconfigure.SecurityProperties;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonAuthStoreRegistryTests {

    @TempDir
    Path tempDir;

    @Test
    void initializeCreatesBootstrapStoreFromSpringSecurityPassword() throws Exception {
        Path storePath = tempDir.resolve("mcp-auth-store.json");

        JsonAuthStoreRegistry registry = registry(storePath);
        registry.initialize();

        assertThat(Files.exists(storePath)).isTrue();
        assertThat(registry.bootstrapAdmin().username()).isEqualTo("user");
        assertThat(registry.bootstrapAdmin().password()).isEqualTo("bootstrap-password");
        assertThat(registry.hasAdmin()).isFalse();
    }

    @Test
    void registerFirstAdminStoresGeneratedMcpCredentialsAndReturnsSecretsOnce() {
        JsonAuthStoreRegistry registry = registry(tempDir.resolve("mcp-auth-store.json"));
        registry.initialize();

        FirstAdminRegistrationResult result = registry.registerFirstAdmin(new FirstAdminRegistrationCommand(
                "root",
                "bootstrap-password",
                "new-password",
                "new-password",
                "root@example.test",
                "Root Admin"
        ));

        assertThat(result.username()).isEqualTo("root");
        assertThat(result.email()).isEqualTo("root@example.test");
        assertThat(result.generatedAuth().accessToken()).startsWith("access-");
        assertThat(result.generatedAuth().secretKey()).startsWith("secret-");
        assertThat(result.generatedAuth().authToken()).startsWith("auth-");
        assertThat(registry.hasAdmin()).isTrue();
        assertThat(registry.bootstrapAdmin().password()).isNull();
        assertThat(registry.validateMcpCredentials(
                "Bearer " + result.generatedAuth().accessToken(),
                result.generatedAuth().secretKey(),
                result.generatedAuth().authToken()
        )).isPresent();
        assertThat(registry.isAdminBearerToken("Bearer " + result.generatedAuth().accessToken())).isTrue();
    }

    @Test
    void registerFirstAdminRejectsWrongBootstrapPasswordAndDuplicates() {
        JsonAuthStoreRegistry registry = registry(tempDir.resolve("mcp-auth-store.json"));
        registry.initialize();

        assertThatThrownBy(() -> registry.registerFirstAdmin(new FirstAdminRegistrationCommand(
                "root",
                "wrong",
                null,
                null,
                null,
                null
        ))).isInstanceOf(AuthStoreException.class)
                .hasMessage("Invalid bootstrap password");

        registry.registerFirstAdmin(new FirstAdminRegistrationCommand(
                "root",
                "bootstrap-password",
                null,
                null,
                null,
                null
        ));

        assertThatThrownBy(() -> registry.registerFirstAdmin(new FirstAdminRegistrationCommand(
                "other",
                "bootstrap-password",
                null,
                null,
                null,
                null
        ))).isInstanceOf(AuthStoreException.class)
                .hasMessage("First admin is already registered");
    }

    private JsonAuthStoreRegistry registry(Path storePath) {
        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.getUser().setPassword("bootstrap-password");
        return new JsonAuthStoreRegistry(new ObjectMapper(), securityProperties, storePath.toString());
    }
}

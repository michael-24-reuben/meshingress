package dev.mrk.meshingress.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = BootstrapAdminControllerTests.StorePathInitializer.class)
class BootstrapAdminControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthStoreRegistry authStoreRegistry;

    @Test
    void firstAdminRegistrationReturnsGeneratedMcpAuthOnce() throws Exception {
        String bootstrapPassword = authStoreRegistry.bootstrapAdmin().password();
        System.out.println("bootstrapPassword = " + bootstrapPassword);

        mockMvc.perform(post("/api/v1/auth/bootstrap/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "root",
                                  "bootstrapPassword": "%s",
                                  "newPassword": "new-password",
                                  "confirmationPassword": "new-password",
                                  "email": "root@example.test",
                                  "displayName": "Root Admin"
                                }
                                """.formatted(bootstrapPassword)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("root"))
                .andExpect(jsonPath("$.email").value("root@example.test"))
                .andExpect(jsonPath("$.generatedAuth.accessToken", startsWith("access-")))
                .andExpect(jsonPath("$.generatedAuth.secretKey", startsWith("secret-")))
                .andExpect(jsonPath("$.generatedAuth.authToken", startsWith("auth-")));

        mockMvc.perform(post("/api/v1/auth/bootstrap/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "other",
                                  "bootstrapPassword": "%s"
                                }
                                """.formatted(bootstrapPassword)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("First admin is already registered"));
    }

    static class StorePathInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues.of(
                    "meshingress.auth.store.path=target/test-auth-store/bootstrap-controller-"
                            + UUID.randomUUID()
                            + ".json"
            ).applyTo(applicationContext);
        }
    }
}

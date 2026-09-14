package dev.mrk.toolspace.nextcloud.config;

import dev.mrk.meshingress.api.storage.ToolStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class NextcloudStorageAutoConfigurationTests {
    @Test
    void moduleResourceBindsTheProviderBeforeItsBeansAreCreated() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test-overrides", Map.of(
                    "meshingress.toolspace.nextcloud.storage.credential-ref", ""
            )));
            context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
            context.registerBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class));
            context.register(NextcloudStorageAutoConfiguration.class);
            context.refresh();

            NextcloudStorageProperties properties = context.getBean(NextcloudStorageProperties.class);
            ToolStorageService storage = context.getBean(ToolStorageService.class);

            assertEquals("https://alphasunny-xps-8700.tail93ea23.ts.net", properties.getEndpoint());
            assertNotNull(storage);
        }
    }
}

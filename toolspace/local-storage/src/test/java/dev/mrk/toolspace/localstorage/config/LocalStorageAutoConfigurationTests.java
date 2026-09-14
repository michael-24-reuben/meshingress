package dev.mrk.toolspace.localstorage.config;

import dev.mrk.meshingress.api.storage.ToolStorageService;
import dev.mrk.meshingress.api.tools.annotation.McpService;
import dev.mrk.toolspace.storage.config.LocalStorageAutoConfiguration;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalStorageAutoConfigurationTests {
    @Test
    void moduleResourceCreatesTheAnnotatedLocalStorageProvider() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:local_storage_auto_configuration;DB_CLOSE_DELAY=-1");
            context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
            context.registerBean(JdbcTemplate.class, () -> new JdbcTemplate(dataSource));
            context.registerBean(PlatformTransactionManager.class, () -> new DataSourceTransactionManager(dataSource));
            context.register(LocalStorageAutoConfiguration.class);
            context.refresh();

            ToolStorageService storage = context.getBean(ToolStorageService.class);
            LocalStorageProperties properties = context.getBean(LocalStorageProperties.class);

            assertNotNull(storage);
            assertEquals(".cache/meshingress/storage", properties.local().root());
            assertTrue(storage.getClass().isAnnotationPresent(McpService.class));
            assertEquals("local.storage.workspaces", storage.getClass().getAnnotation(McpService.class).value());
        }
    }
}

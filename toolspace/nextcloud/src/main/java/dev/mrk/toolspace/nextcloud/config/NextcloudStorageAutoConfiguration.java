package dev.mrk.toolspace.nextcloud.config;

import dev.mrk.toolspace.nextcloud.storage.DelegatedViewerCapabilityStore;
import dev.mrk.toolspace.nextcloud.storage.DelegatedViewerService;
import dev.mrk.toolspace.nextcloud.storage.NextcloudDelegatedStorageService;
import dev.mrk.toolspace.nextcloud.storage.NextcloudDelegatedWorkspaceClient;
import dev.mrk.toolspace.nextcloud.web.DelegatedViewerController;
import dev.mrk.toolspace.nextcloud.web.NextcloudStorageRouteExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@AutoConfiguration
@ConditionalOnProperty(prefix = "meshingress.toolspace.nextcloud.storage", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NextcloudStorageAutoConfiguration {
    @Bean
    NextcloudStorageProperties nextcloudStorageProperties(ConfigurableEnvironment environment) throws IOException {
        environment.getPropertySources().addLast(new ResourcePropertySource(
                "nextcloud-storage-module-defaults", new ClassPathResource("nextcloud-storage.properties")
        ));
        return Binder.get(environment)
                .bind("meshingress.toolspace.nextcloud.storage", Bindable.of(NextcloudStorageProperties.class))
                .orElseGet(NextcloudStorageProperties::new);
    }

    @Bean
    NextcloudDelegatedWorkspaceClient nextcloudDelegatedWorkspaceClient(NextcloudStorageProperties properties, ObjectMapper objectMapper) {
        properties.validate();
        return new NextcloudDelegatedWorkspaceClient(properties, NextcloudCredentialResolver.resolveAuthorization(properties.getCredentialRef()), objectMapper);
    }

    @Bean
    DelegatedViewerCapabilityStore delegatedViewerCapabilityStore(JdbcTemplate jdbcTemplate, NextcloudStorageProperties properties) {
        return new DelegatedViewerCapabilityStore(jdbcTemplate, properties);
    }

    @Bean
    NextcloudDelegatedStorageService nextcloudDelegatedStorageService(
            NextcloudDelegatedWorkspaceClient client,
            DelegatedViewerCapabilityStore viewers,
            NextcloudStorageProperties properties
    ) {
        return new NextcloudDelegatedStorageService(client, viewers, properties);
    }

    @Bean
    DelegatedViewerService delegatedViewerService(DelegatedViewerCapabilityStore capabilities, NextcloudDelegatedWorkspaceClient client) {
        return new DelegatedViewerService(capabilities, client);
    }

    @Bean
    DelegatedViewerController delegatedViewerController(DelegatedViewerService viewer) {
        return new DelegatedViewerController(viewer);
    }

    @Bean
    NextcloudStorageRouteExceptionHandler nextcloudStorageRouteExceptionHandler() {
        return new NextcloudStorageRouteExceptionHandler();
    }
}

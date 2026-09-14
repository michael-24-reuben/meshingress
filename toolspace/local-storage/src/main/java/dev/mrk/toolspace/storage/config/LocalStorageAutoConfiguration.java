package dev.mrk.toolspace.storage.config;

import dev.mrk.meshingress.api.storage.ToolStorageService;
import dev.mrk.toolspace.localstorage.config.LocalStorageProperties;
import dev.mrk.toolspace.storage.workspace.ToolWorkspaceStorageService;
import dev.mrk.toolspace.storage.workspace.WorkspaceFiles;
import dev.mrk.toolspace.storage.workspace.WorkspaceCleanupCoordinator;
import dev.mrk.toolspace.storage.workspace.WorkspaceMetadataStore;
import dev.mrk.toolspace.storage.workspace.WorkspacePathLayout;
import dev.mrk.toolspace.storage.workspace.WorkspaceRetrievalService;
import dev.mrk.toolspace.storage.workspace.AsyncExternalHandoffWorker;
import org.springframework.boot.ApplicationRunner;
import dev.mrk.toolspace.storage.web.StorageController;
import dev.mrk.toolspace.storage.web.StorageRouteExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@AutoConfiguration
@EnableScheduling
@ConditionalOnProperty(prefix = "meshingress.toolspace.local-storage", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LocalStorageAutoConfiguration {
    @Bean
    LocalStorageProperties localStorageProperties(ConfigurableEnvironment environment) throws IOException {
        environment.getPropertySources().addLast(new ResourcePropertySource(
                "local-storage-module-defaults", new ClassPathResource("local-storage.properties")
        ));
        return Binder.get(environment)
                .bind("meshingress.toolspace.local-storage", Bindable.of(LocalStorageProperties.class))
                .orElseThrow(() -> new IllegalStateException("Local storage module configuration is unavailable."));
    }

    @Bean
    WorkspacePathLayout workspacePathLayout(LocalStorageProperties properties) {
        return new WorkspacePathLayout(properties.local().root());
    }

    @Bean
    WorkspaceFiles workspaceFiles(WorkspacePathLayout layout) {
        return new WorkspaceFiles(layout);
    }

    @Bean
    WorkspaceMetadataStore workspaceMetadataStore(JdbcTemplate jdbc, PlatformTransactionManager manager, LocalStorageProperties properties) {
        return new WorkspaceMetadataStore(jdbc, new TransactionTemplate(manager), properties.metadata().sql());
    }

    @Bean
    StorageLifecyclePolicy storageLifecyclePolicy(LocalStorageProperties properties) {
        return new StorageLifecyclePolicy(properties);
    }

    @Bean
    ToolStorageService toolStorageService(LocalStorageProperties properties, WorkspaceMetadataStore metadata, WorkspaceFiles files,
                                          WorkspacePathLayout paths, ObjectMapper objectMapper, StorageLifecyclePolicy lifecyclePolicy) {
        return new ToolWorkspaceStorageService(properties, metadata, files, paths, objectMapper, lifecyclePolicy.publisher().orElse(null));
    }

    @Bean
    WorkspaceRetrievalService workspaceRetrievalService(LocalStorageProperties properties, WorkspaceMetadataStore metadata, WorkspaceFiles files, WorkspacePathLayout paths) {
        return new WorkspaceRetrievalService(properties, metadata, files, paths);
    }

    @Bean
    WorkspaceCleanupCoordinator workspaceCleanupCoordinator(LocalStorageProperties properties, WorkspaceMetadataStore metadata, WorkspaceFiles files) {
        return new WorkspaceCleanupCoordinator(properties, metadata, files);
    }

    @Bean
    @ConditionalOnProperty(prefix = "meshingress.toolspace.local-storage", name = "lifecycle", havingValue = "local-external")
    AsyncExternalHandoffWorker asyncExternalHandoffWorker(LocalStorageProperties properties, WorkspaceMetadataStore metadata, WorkspaceFiles files, StorageLifecyclePolicy lifecyclePolicy) {
        return new AsyncExternalHandoffWorker(properties, metadata, files,
                lifecyclePolicy.publisher().orElseThrow(() -> new IllegalStateException("No external handoff publisher is configured.")));
    }

    @Bean
    StorageScheduler storageScheduler(WorkspaceCleanupCoordinator cleanup, org.springframework.beans.factory.ObjectProvider<AsyncExternalHandoffWorker> handoffWorker) {
        return new StorageScheduler(cleanup, handoffWorker);
    }

    @Bean
    ApplicationRunner storageStartupCleanup(WorkspaceCleanupCoordinator cleanup, org.springframework.beans.factory.ObjectProvider<AsyncExternalHandoffWorker> handoffWorker) {
        return args -> { cleanup.runBounded(); handoffWorker.ifAvailable(AsyncExternalHandoffWorker::runBounded); };
    }

    @Bean
    StorageController storageController(WorkspaceRetrievalService retrieval) {
        return new StorageController(retrieval);
    }

    @Bean
    StorageRouteExceptionHandler storageRouteExceptionHandler() {
        return new StorageRouteExceptionHandler();
    }
}

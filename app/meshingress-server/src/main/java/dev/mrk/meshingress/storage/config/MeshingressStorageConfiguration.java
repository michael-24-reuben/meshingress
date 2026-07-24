package dev.mrk.meshingress.storage.config;

import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.api.storage.ToolStorageService;
import dev.mrk.meshingress.storage.workspace.ToolWorkspaceStorageService;
import dev.mrk.meshingress.storage.workspace.WorkspaceFiles;
import dev.mrk.meshingress.storage.workspace.WorkspaceCleanupCoordinator;
import dev.mrk.meshingress.storage.workspace.WorkspaceMetadataStore;
import dev.mrk.meshingress.storage.workspace.WorkspacePathLayout;
import dev.mrk.meshingress.storage.workspace.WorkspaceRetrievalService;
import dev.mrk.meshingress.storage.workspace.DelegatedViewerCapabilityStore;
import dev.mrk.meshingress.storage.workspace.DelegatedViewerService;
import dev.mrk.meshingress.storage.workspace.NextcloudDelegatedWorkspaceClient;
import dev.mrk.meshingress.storage.workspace.AsyncExternalHandoffWorker;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "meshingress.storage", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MeshingressStorageConfiguration {
    @Bean
    WorkspacePathLayout workspacePathLayout(MeshingressProperties properties) {
        return new WorkspacePathLayout(properties.storage().local().root());
    }

    @Bean
    WorkspaceFiles workspaceFiles(WorkspacePathLayout layout) {
        return new WorkspaceFiles(layout);
    }

    @Bean
    WorkspaceMetadataStore workspaceMetadataStore(JdbcTemplate jdbc, PlatformTransactionManager manager, MeshingressProperties properties) {
        return new WorkspaceMetadataStore(jdbc, new TransactionTemplate(manager), properties.storage().metadata().sql());
    }

    @Bean
    StorageLifecyclePolicy storageLifecyclePolicy(MeshingressProperties properties) {
        return new StorageLifecyclePolicy(properties.storage());
    }

    @Bean
    ToolStorageService toolStorageService(MeshingressProperties properties, WorkspaceMetadataStore metadata, WorkspaceFiles files,
                                          WorkspacePathLayout paths, ObjectMapper objectMapper, StorageLifecyclePolicy lifecyclePolicy, JdbcTemplate jdbc) {
        if (properties.storage().lifecycle() == MeshingressProperties.Storage.Lifecycle.DELEGATED_EXTERNAL) {
            var target = properties.storage().external().targets().get(properties.storage().external().defaultTarget());
            NextcloudDelegatedWorkspaceClient client = new NextcloudDelegatedWorkspaceClient(target, properties.storage().external().uploadTimeout(), StorageLifecyclePolicy.authorization(target.credentialRef()), objectMapper);
            DelegatedViewerCapabilityStore viewers = new DelegatedViewerCapabilityStore(jdbc, properties.storage().metadata().sql());
            return new dev.mrk.meshingress.storage.workspace.NextcloudDelegatedStorageService(client, viewers, properties.identity().publicBaseUrl().toString(), properties.storage());
        }
        return new ToolWorkspaceStorageService(properties.storage(), metadata, files, paths, objectMapper, lifecyclePolicy.publisher().orElse(null));
    }

    @Bean
    @ConditionalOnProperty(prefix = "meshingress.storage", name = "lifecycle", havingValue = "delegated-external")
    DelegatedViewerService delegatedViewerService(MeshingressProperties properties, ObjectMapper objectMapper, JdbcTemplate jdbc) {
        var target = properties.storage().external().targets().get(properties.storage().external().defaultTarget());
        NextcloudDelegatedWorkspaceClient client = new NextcloudDelegatedWorkspaceClient(target, properties.storage().external().uploadTimeout(), StorageLifecyclePolicy.authorization(target.credentialRef()), objectMapper);
        return new DelegatedViewerService(new DelegatedViewerCapabilityStore(jdbc, properties.storage().metadata().sql()), client);
    }

    @Bean
    WorkspaceRetrievalService workspaceRetrievalService(MeshingressProperties properties, WorkspaceMetadataStore metadata,
                                                        WorkspaceFiles files, WorkspacePathLayout paths) {
        return new WorkspaceRetrievalService(properties.storage(), metadata, files, paths);
    }

    @Bean
    WorkspaceCleanupCoordinator workspaceCleanupCoordinator(MeshingressProperties properties, WorkspaceMetadataStore metadata, WorkspaceFiles files) {
        return new WorkspaceCleanupCoordinator(properties.storage(), metadata, files);
    }

    @Bean
    @ConditionalOnProperty(prefix = "meshingress.storage", name = "lifecycle", havingValue = "local-async-external")
    AsyncExternalHandoffWorker asyncExternalHandoffWorker(MeshingressProperties properties, WorkspaceMetadataStore metadata,
                                                          WorkspaceFiles files, StorageLifecyclePolicy lifecyclePolicy) {
        return new AsyncExternalHandoffWorker(properties.storage(), metadata, files,
                lifecyclePolicy.publisher().orElseThrow(() -> new IllegalStateException("No external handoff publisher is configured.")));
    }

    @Bean
    StorageScheduler storageScheduler(WorkspaceCleanupCoordinator cleanup, ObjectProvider<AsyncExternalHandoffWorker> handoffWorker) {
        return new StorageScheduler(cleanup, handoffWorker);
    }

    @Bean
    ApplicationRunner storageStartupCleanup(WorkspaceCleanupCoordinator cleanup, ObjectProvider<AsyncExternalHandoffWorker> handoffWorker) {
        return args -> { cleanup.runBounded(); handoffWorker.ifAvailable(AsyncExternalHandoffWorker::runBounded); };
    }
}

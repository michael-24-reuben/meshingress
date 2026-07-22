package dev.mrk.meshingress.server.install;

import dev.mrk.meshingress.controller.roles.registration.ToolRegistrationRecord;
import dev.mrk.meshingress.controller.roles.registration.ToolRegistrationStore;
import dev.mrk.meshingress.controller.roles.registration.ToolSourceKind;
import dev.mrk.meshingress.artifact.storage.ProjectRootResolver;
import dev.mrk.meshingress.runtime.artifacts.LocalJarSource;
import dev.mrk.meshingress.runtime.loader.ToolRuntimeLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
class RuntimePublicationRegistrationReconciler implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimePublicationRegistrationReconciler.class);

    private final ToolRegistrationStore registrationStore;
    private final ToolRuntimeLoader runtimeLoader;
    private final Path projectRoot;

    @Autowired
    RuntimePublicationRegistrationReconciler(ToolRegistrationStore registrationStore, ToolRuntimeLoader runtimeLoader) {
        this(registrationStore, runtimeLoader,
                ProjectRootResolver.resolve(RuntimePublicationRegistrationReconciler.class, null));
    }

    RuntimePublicationRegistrationReconciler(
            ToolRegistrationStore registrationStore,
            ToolRuntimeLoader runtimeLoader,
            Path projectRoot
    ) {
        this.registrationStore = registrationStore;
        this.runtimeLoader = runtimeLoader;
        this.projectRoot = ProjectRootResolver.resolve(RuntimePublicationRegistrationReconciler.class, projectRoot);
    }

    @Override
    public void run(ApplicationArguments args) {
        reconcile();
    }

    RuntimePublicationReconciliationResult reconcile() {
        int candidates = 0;
        int reconciled = 0;
        int missingCache = 0;
        int failed = 0;
        for (ToolRegistrationRecord record : registrationStore.list()) {
            if (!isRuntimePublicationCandidate(record)) {
                continue;
            }
            candidates++;
            Path cachedJar = runtimeCachePath(record);
            if (cachedJar == null || !Files.isRegularFile(cachedJar)) {
                registrationStore.markStatus(record.registrationId(), "missing-cache");
                missingCache++;
                LOGGER.warn("Runtime publication registration {} missing cached artifact: {}",
                        record.registrationId(),
                        cachedJar);
                continue;
            }
            try {
                runtimeLoader.activate(new LocalJarSource(cachedJar));
                registrationStore.markStatus(record.registrationId(), "reconciled");
                reconciled++;
            } catch (RuntimeException exception) {
                registrationStore.markStatus(record.registrationId(), "reconcile-failed");
                failed++;
                LOGGER.warn("Runtime publication registration {} failed startup reconciliation.",
                        record.registrationId(),
                        exception);
            }
        }
        return new RuntimePublicationReconciliationResult(candidates, reconciled, missingCache, failed);
    }

    private boolean isRuntimePublicationCandidate(ToolRegistrationRecord record) {
        return record.sourceKind() == ToolSourceKind.PUBLICATION_RECORD && isActive(record.status());
    }

    private boolean isActive(String status) {
        return switch (status) {
            case "active", "reconciled" -> true;
            default -> false;
        };
    }

    private Path runtimeCachePath(ToolRegistrationRecord record) {
        String value = record.source().get("runtimeCachePath");
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ProjectRootResolver.resolveRelative(projectRoot, value);
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Runtime publication registration {} has an invalid project-relative cache path: {}",
                    record.registrationId(), value);
            return null;
        }
    }

    record RuntimePublicationReconciliationResult(int candidates, int reconciled, int missingCache, int failed) {
    }
}

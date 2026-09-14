package dev.mrk.toolspace.storage.config;

import dev.mrk.toolspace.storage.workspace.WorkspaceCleanupCoordinator;
import dev.mrk.toolspace.storage.workspace.AsyncExternalHandoffWorker;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;

public class StorageScheduler {
    private final WorkspaceCleanupCoordinator cleanup;
    private final ObjectProvider<AsyncExternalHandoffWorker> handoffWorker;

    public StorageScheduler(WorkspaceCleanupCoordinator cleanup, ObjectProvider<AsyncExternalHandoffWorker> handoffWorker) { this.cleanup = cleanup; this.handoffWorker = handoffWorker; }

    @Scheduled(fixedDelayString = "${meshingress.toolspace.local-storage.local.cleanup.interval:300000}")
    void cleanup() { cleanup.runBounded(); }

    @Scheduled(fixedDelayString = "${meshingress.toolspace.local-storage.external.async-handoff.worker-interval:5s}")
    void externalHandoff() { handoffWorker.ifAvailable(AsyncExternalHandoffWorker::runBounded); }
}

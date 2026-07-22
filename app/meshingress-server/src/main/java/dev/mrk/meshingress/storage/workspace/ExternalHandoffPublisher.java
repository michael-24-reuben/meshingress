package dev.mrk.meshingress.storage.workspace;

import dev.mrk.meshingress.api.storage.ToolStorageWorkspace;

import java.util.List;

/** Publishes already-local staging files without ever observing or mutating foreign objects. */
public interface ExternalHandoffPublisher {
    String target();
    boolean supportsExternalStaging();
    HandoffReceipt publish(ToolStorageWorkspace workspace, List<WorkspaceFileRecord> files, WorkspaceFiles stagingFiles);

    /** Called only after a create-only remote PUT has been accepted. */
    default HandoffReceipt publish(ToolStorageWorkspace workspace, List<WorkspaceFileRecord> files, WorkspaceFiles stagingFiles,
                                   HandoffProgress progress) {
        return publish(workspace, files, stagingFiles);
    }

    record HandoffReceipt(String target, String providerReceipt) { }
    @FunctionalInterface
    interface HandoffProgress { void accepted(String relativePath); }
}

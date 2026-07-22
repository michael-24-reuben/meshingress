package dev.mrk.meshingress.storage.workspace;

import dev.mrk.meshingress.config.MeshingressProperties;

import java.time.OffsetDateTime;

public final class WorkspaceCleanupCoordinator {
    private final MeshingressProperties.Storage properties;
    private final WorkspaceMetadataStore metadata;
    private final WorkspaceFiles files;

    public WorkspaceCleanupCoordinator(MeshingressProperties.Storage properties, WorkspaceMetadataStore metadata, WorkspaceFiles files) {
        this.properties = properties; this.metadata = metadata; this.files = files;
    }

    public void runBounded() {
        OffsetDateTime now = OffsetDateTime.now();
        for (WorkspaceRecord candidate : metadata.cleanupCandidates(now, now.minus(properties.local().staging().ttl()), properties.local().cleanup().batchSize())) {
            metadata.claimDeletion(candidate.sessionId(), candidate.requestId()).ifPresent(claimed -> {
                try {
                    files.deleteStaging(claimed.sessionId(), claimed.requestId());
                    files.deletePublished(claimed.sessionId(), claimed.requestId());
                    metadata.deleted(claimed.sessionId(), claimed.requestId());
                } catch (Exception exception) {
                    metadata.failed(claimed.sessionId(), claimed.requestId());
                }
            });
        }
    }
}

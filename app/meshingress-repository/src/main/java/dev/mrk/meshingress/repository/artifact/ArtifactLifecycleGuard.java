package dev.mrk.meshingress.repository.artifact;

import dev.mrk.meshingress.artifact.model.ArtifactCoordinate;
import dev.mrk.meshingress.repository.artifact.store.ArtifactMetadataStore;
import org.springframework.stereotype.Component;

@Component
public class ArtifactLifecycleGuard {

    private static final String DEFAULT_PACKAGING = "jar";

    private final ArtifactMetadataStore metadataStore;

    public ArtifactLifecycleGuard(ArtifactMetadataStore metadataStore) {
        this.metadataStore = metadataStore;
    }

    public void requireAssessedBeforeApproval(String groupId, String artifactId, String version) {
        ArtifactCoordinate coordinate = coordinate(groupId, artifactId, version);
        requireArtifact(coordinate);
        requireEvent(coordinate, "ASSESS", "artifact must be assessed before approval");
    }

    public void requireReadyBeforePublication(String groupId, String artifactId, String version) {
        ArtifactCoordinate coordinate = coordinate(groupId, artifactId, version);
        requireArtifact(coordinate);
        requireEvent(coordinate, "ASSESS", "artifact must be assessed before publication");
        requireEvent(coordinate, "APPROVE", "artifact must be approved before publication");
    }

    private void requireArtifact(ArtifactCoordinate coordinate) {
        if (metadataStore.findArtifact(coordinate).isEmpty()) {
            throw new RepositoryException("artifact not found: " + coordinate.display());
        }
    }

    private void requireEvent(ArtifactCoordinate coordinate, String eventType, String message) {
        if (!metadataStore.hasLifecycleEvent(coordinate, eventType)) {
            throw new RepositoryException(message);
        }
    }

    private ArtifactCoordinate coordinate(String groupId, String artifactId, String version) {
        return new ArtifactCoordinate(groupId, artifactId, version, null, DEFAULT_PACKAGING);
    }
}

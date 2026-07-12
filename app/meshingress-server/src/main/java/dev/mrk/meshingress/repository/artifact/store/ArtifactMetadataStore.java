package dev.mrk.meshingress.repository.artifact.store;

import dev.mrk.meshingress.artifact.model.ArtifactCoordinate;
import dev.mrk.meshingress.artifact.model.ArtifactPublicationRecord;
import dev.mrk.meshingress.artifact.model.ArtifactRecord;
import dev.mrk.meshingress.artifact.security.ScannerResult;
import dev.mrk.meshingress.repository.artifact.ArtifactReviewQueueItem;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface ArtifactMetadataStore {

    void saveArtifact(ArtifactRecord record, Path artifactPath);

    Optional<ArtifactMetadataEntry> findArtifact(ArtifactCoordinate coordinate);

    List<ArtifactMetadataEntry> findArtifactsByPackaging(String packaging);

    void saveAssessment(ArtifactCoordinate coordinate, List<ScannerResult> results);

    List<ScannerResult> findAssessment(ArtifactCoordinate coordinate);

    void savePublication(ArtifactPublicationRecord publication);

    Optional<ArtifactPublicationRecord> findPublication(ArtifactCoordinate coordinate);

    List<ArtifactReviewQueueItem> findPendingReviewArtifacts();

    boolean hasLifecycleEvent(ArtifactCoordinate coordinate, String eventType);

    int countLifecycleEvents(ArtifactCoordinate coordinate, String eventType);

    Optional<ArtifactLifecycleEvent> findLatestLifecycleEvent(ArtifactCoordinate coordinate, String eventType);

    void appendLifecycleEvent(ArtifactLifecycleEvent event);
}

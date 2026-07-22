package dev.mrk.meshingress.storage.workspace;

import dev.mrk.meshingress.api.storage.ToolStorageException;
import dev.mrk.meshingress.api.storage.ToolStorageWorkspace;
import dev.mrk.meshingress.config.MeshingressProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Bounded, durable handoff executor. Queue state is database-backed, so a stopped process
 * leaves only an expiring lease; the next process can safely reclaim it without remote reads.
 */
public final class AsyncExternalHandoffWorker implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncExternalHandoffWorker.class);
    private final MeshingressProperties.Storage properties;
    private final WorkspaceMetadataStore metadata;
    private final WorkspaceFiles files;
    private final ExternalHandoffPublisher publisher;
    private final ExecutorService workers;

    public AsyncExternalHandoffWorker(MeshingressProperties.Storage properties, WorkspaceMetadataStore metadata,
                                      WorkspaceFiles files, ExternalHandoffPublisher publisher) {
        this.properties = properties;
        this.metadata = metadata;
        this.files = files;
        this.publisher = publisher;
        this.workers = Executors.newFixedThreadPool(properties.external().maxConcurrentUploads(), Thread.ofPlatform().name("storage-handoff-", 0).factory());
    }

    /** Claims at most the configured concurrency limit. Callers may invoke this safely on a schedule and at startup. */
    public void runBounded() {
        List<Future<?>> submitted = new ArrayList<>();
        for (int slot = 0; slot < properties.external().maxConcurrentUploads(); slot++) {
            var claimed = metadata.claimHandoff(OffsetDateTime.now(), properties.external().asyncHandoff().leaseDuration());
            if (claimed.isEmpty()) break;
            submitted.add(workers.submit(() -> handoff(claimed.get())));
        }
        for (Future<?> future : submitted) {
            try { future.get(); }
            catch (Exception exception) { LOGGER.error("Async external handoff worker task ended unexpectedly.", exception); }
        }
    }

    private void handoff(WorkspaceMetadataStore.ClaimedHandoff claimed) {
        HandoffJob job = claimed.job();
        try {
            if (claimed.acceptedFiles().contains("manifest.json")) {
                metadata.completeHandoff(job, new ExternalHandoffPublisher.HandoffReceipt(job.target(), "recovered:manifest-recorded"), OffsetDateTime.now());
                files.deleteStaging(job.sessionId(), job.requestId());
                return;
            }
            List<WorkspaceFileRecord> remaining = claimed.files().stream()
                    .filter(file -> !claimed.acceptedFiles().contains(file.relativePath()))
                    .toList();
            ToolStorageWorkspace workspace = new ToolStorageWorkspace(claimed.workspace().sessionId(), claimed.workspace().requestId(),
                    claimed.workspace().toolId(), "", claimed.workspace().createdAt(), claimed.workspace().expiresAt(),
                    claimed.workspace().remainingRequests(), true, WorkspaceState.HANDOFF_IN_PROGRESS.name());
            ExternalHandoffPublisher.HandoffReceipt receipt = publisher.publish(workspace, remaining, files,
                    path -> metadata.handoffFileAccepted(job.sessionId(), job.requestId(), path, OffsetDateTime.now()));
            metadata.completeHandoff(job, receipt, OffsetDateTime.now());
            try {
                files.deleteStaging(job.sessionId(), job.requestId());
            } catch (Exception cleanupFailure) {
                LOGGER.warn("External handoff {} / {} completed but local staging cleanup will be retried by normal retention cleanup.", job.sessionId(), job.requestId(), cleanupFailure);
            }
            return;
        } catch (Exception exception) {
            boolean retryable = retryable(exception);
            String detail = detail(exception);
            metadata.handoffAttemptFailed(job, detail, retryable, properties.external().asyncHandoff().maxAttempts(),
                    retryDelay(job.attempts()), OffsetDateTime.now());
            LOGGER.warn("Async external handoff {} / {} {} after attempt {}: {}", job.sessionId(), job.requestId(),
                    retryable ? "will retry" : "failed permanently", job.attempts(), detail);
        }
    }

    private Duration retryDelay(int attempts) {
        Duration value = properties.external().asyncHandoff().initialRetryDelay();
        Duration maximum = properties.external().asyncHandoff().maxRetryDelay();
        for (int index = 1; index < attempts && value.compareTo(maximum) < 0; index++) {
            value = value.multipliedBy(2);
            if (value.compareTo(maximum) > 0) value = maximum;
        }
        return value;
    }

    private static boolean retryable(Exception exception) {
        String message = detail(exception);
        return !(message.contains("HTTP 401") || message.contains("HTTP 403") || message.contains("HTTP 404")
                || message.contains("HTTP 409") || message.contains("HTTP 412") || message.contains("configured base path")
                || message.contains("conflicted with an existing object"));
    }

    private static String detail(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return exception.getClass().getSimpleName();
        return message.length() <= 2048 ? message : message.substring(0, 2048);
    }

    @Override
    public void close() {
        workers.shutdownNow();
    }
}

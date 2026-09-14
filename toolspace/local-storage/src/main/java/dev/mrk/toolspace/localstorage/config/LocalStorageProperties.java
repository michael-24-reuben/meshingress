package dev.mrk.toolspace.localstorage.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Module-owned configuration for the local workspace storage provider.
 *
 * <p>This model binds {@code meshingress.toolspace.local-storage.*}; it is not
 * part of the application's general {@code meshingress.*} configuration.</p>
 */
public record LocalStorageProperties(
        boolean enabled,
        @NotNull Lifecycle lifecycle,
        @NotNull DataSize maxEntrySize,
        @Valid @NotNull Local local,
        @Valid @NotNull External external,
        @Valid @NotNull Metadata metadata
) {
    private static final String DEFAULT_STORAGE_ROOT = ".cache/meshingress/storage";

    public LocalStorageProperties {
        lifecycle = lifecycle == null ? Lifecycle.LOCAL_LOCAL : lifecycle;
        maxEntrySize = Objects.requireNonNullElse(maxEntrySize, DataSize.ofMegabytes(256));
        local = local == null ? Local.defaults() : local;
        external = external == null ? External.defaults() : external;
        metadata = metadata == null ? Metadata.defaults() : metadata;
    }

    /** Placement policy for bytes acquired by the local provider. */
    public enum Lifecycle { LOCAL_LOCAL, LOCAL_EXTERNAL }
    public enum AccessMode { WRITE_ONLY }
    public enum MutationPolicy { CREATE_ONLY }
    public enum ConflictPolicy { FAIL }
    public enum RetentionPolicy { PROVIDER_MANAGED }
    public enum Provider { WEBDAV, GOOGLE_DRIVE, ONEDRIVE }
    public enum StagingMode { DIRECT_FINAL_ONLY }

    public record Local(
            @NotBlank String root,
            @Min(1) int maxEntries,
            @Valid @NotNull Staging staging,
            @Valid @NotNull Published published,
            @Valid @NotNull Cleanup cleanup
    ) {
        public Local {
            root = defaultString(root, DEFAULT_STORAGE_ROOT);
            maxEntries = maxEntries <= 0 ? 1024 : maxEntries;
            staging = staging == null ? Staging.defaults() : staging;
            published = published == null ? Published.defaults() : published;
            cleanup = cleanup == null ? Cleanup.defaults() : cleanup;
        }

        static Local defaults() {
            return new Local(DEFAULT_STORAGE_ROOT, 1024, Staging.defaults(), Published.defaults(), Cleanup.defaults());
        }
    }

    public record Staging(@NotNull DataSize maxBytes, @NotNull Duration ttl) {
        public Staging {
            maxBytes = Objects.requireNonNullElse(maxBytes, DataSize.ofMegabytes(256));
            ttl = Objects.requireNonNullElse(ttl, Duration.ofMinutes(15));
        }

        static Staging defaults() { return new Staging(DataSize.ofMegabytes(256), Duration.ofMinutes(15)); }
    }

    public record Published(
            @NotNull DataSize maxBytes,
            @NotNull Duration defaultTtl,
            @NotNull Duration maxTtl,
            @Min(1) int defaultMaxRequests,
            @Min(1) int maxRequests,
            @Min(1) int maxConcurrentRetrievals
    ) {
        public Published {
            maxBytes = Objects.requireNonNullElse(maxBytes, DataSize.ofGigabytes(1));
            defaultTtl = Objects.requireNonNullElse(defaultTtl, Duration.ofMinutes(30));
            maxTtl = Objects.requireNonNullElse(maxTtl, Duration.ofHours(24));
            defaultMaxRequests = defaultMaxRequests <= 0 ? 1 : defaultMaxRequests;
            maxRequests = maxRequests <= 0 ? 50 : maxRequests;
            maxConcurrentRetrievals = maxConcurrentRetrievals <= 0 ? 32 : maxConcurrentRetrievals;
        }

        static Published defaults() { return new Published(DataSize.ofGigabytes(1), Duration.ofMinutes(30), Duration.ofHours(24), 1, 50, 32); }
    }

    public record Cleanup(@NotNull Duration interval, @Min(1) int batchSize) {
        public Cleanup {
            interval = Objects.requireNonNullElse(interval, Duration.ofMinutes(5));
            batchSize = batchSize <= 0 ? 100 : batchSize;
        }

        static Cleanup defaults() { return new Cleanup(Duration.ofMinutes(5), 100); }
    }

    public record External(
            String defaultTarget,
            @NotNull AccessMode accessMode,
            @NotNull MutationPolicy mutationPolicy,
            @NotNull ConflictPolicy conflictPolicy,
            @NotNull RetentionPolicy retentionPolicy,
            @Min(1) int maxConcurrentUploads,
            @NotNull Duration uploadTimeout,
            @Valid @NotNull AsyncHandoff asyncHandoff,
            @NotNull Map<String, Target> targets
    ) {
        public External {
            defaultTarget = defaultTarget == null ? "" : defaultTarget.trim();
            accessMode = accessMode == null ? AccessMode.WRITE_ONLY : accessMode;
            mutationPolicy = mutationPolicy == null ? MutationPolicy.CREATE_ONLY : mutationPolicy;
            conflictPolicy = conflictPolicy == null ? ConflictPolicy.FAIL : conflictPolicy;
            retentionPolicy = retentionPolicy == null ? RetentionPolicy.PROVIDER_MANAGED : retentionPolicy;
            maxConcurrentUploads = maxConcurrentUploads <= 0 ? 8 : maxConcurrentUploads;
            uploadTimeout = Objects.requireNonNullElse(uploadTimeout, Duration.ofMinutes(30));
            asyncHandoff = asyncHandoff == null ? AsyncHandoff.defaults() : asyncHandoff;
            targets = targets == null ? Map.of() : Map.copyOf(targets);
        }

        public static External defaults() {
            return new External("", AccessMode.WRITE_ONLY, MutationPolicy.CREATE_ONLY, ConflictPolicy.FAIL,
                    RetentionPolicy.PROVIDER_MANAGED, 8, Duration.ofMinutes(30), AsyncHandoff.defaults(), Map.of());
        }
    }

    /** Durable-worker policy for tool-requested queued local-byte handoffs. */
    public record AsyncHandoff(
            @NotNull Duration workerInterval,
            @NotNull Duration leaseDuration,
            @Min(1) int maxAttempts,
            @NotNull Duration initialRetryDelay,
            @NotNull Duration maxRetryDelay
    ) {
        public AsyncHandoff {
            workerInterval = Objects.requireNonNullElse(workerInterval, Duration.ofSeconds(5));
            leaseDuration = Objects.requireNonNullElse(leaseDuration, Duration.ofMinutes(35));
            maxAttempts = maxAttempts <= 0 ? 8 : maxAttempts;
            initialRetryDelay = Objects.requireNonNullElse(initialRetryDelay, Duration.ofSeconds(5));
            maxRetryDelay = Objects.requireNonNullElse(maxRetryDelay, Duration.ofMinutes(5));
        }

        static AsyncHandoff defaults() {
            return new AsyncHandoff(Duration.ofSeconds(5), Duration.ofMinutes(35), 8,
                    Duration.ofSeconds(5), Duration.ofMinutes(5));
        }
    }

    public record Target(
            @NotNull Provider provider,
            boolean enabled,
            String endpoint,
            String basePath,
            String credentialRef,
            String parentFolderId,
            String driveId,
            @NotNull StagingMode stagingMode
    ) {
        public Target {
            provider = provider == null ? Provider.WEBDAV : provider;
            endpoint = endpoint == null ? "" : endpoint.trim();
            basePath = basePath == null ? "" : basePath.trim();
            credentialRef = credentialRef == null ? "" : credentialRef.trim();
            parentFolderId = parentFolderId == null ? "" : parentFolderId.trim();
            driveId = driveId == null ? "" : driveId.trim();
            stagingMode = stagingMode == null ? StagingMode.DIRECT_FINAL_ONLY : stagingMode;
        }
    }

    public record Metadata(@Valid @NotNull Sql sql) {
        public Metadata { sql = sql == null ? Sql.defaults() : sql; }
        static Metadata defaults() { return new Metadata(Sql.defaults()); }
    }

    public record Sql(@NotBlank String schema, @NotBlank String tablePrefix, @Valid @NotNull Tables table, boolean initializeSchema) {
        public Sql {
            schema = defaultString(schema, "meshingress");
            tablePrefix = defaultString(tablePrefix, "storage_");
            table = table == null ? Tables.defaults(tablePrefix) : table.withDefaults(tablePrefix);
        }

        static Sql defaults() { return new Sql("meshingress", "storage_", Tables.defaults("storage_"), true); }
    }

    public record Tables(String entries, String usage, String events) {
        public Tables withDefaults(String prefix) {
            return new Tables(defaultString(entries, prefix + "entries"), defaultString(usage, prefix + "usage"),
                    defaultString(events, prefix + "events"));
        }

        static Tables defaults(String prefix) { return new Tables(prefix + "entries", prefix + "usage", prefix + "events"); }
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

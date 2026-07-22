package dev.mrk.meshingress.storage.workspace;

import dev.mrk.meshingress.api.storage.ToolStorageException;
import dev.mrk.meshingress.config.MeshingressProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class WorkspaceMetadataStore {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceMetadataStore.class);
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final String schema;
    private final String entries;
    private final String usage;
    private final String events;
    private final String handoffs;
    private final String handoffFiles;
    private final String entriesTable;
    private final String usageTable;
    private final String eventsTable;
    private final String handoffsTable;
    private final String handoffFilesTable;

    public WorkspaceMetadataStore(JdbcTemplate jdbc, TransactionTemplate transactions, MeshingressProperties.Storage.Sql sql) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.schema = id(sql.schema());
        this.entriesTable = id(sql.table().entries());
        this.usageTable = id(sql.table().usage());
        this.eventsTable = id(sql.table().events());
        this.handoffsTable = id(entriesTable + "_handoffs");
        this.handoffFilesTable = id(entriesTable + "_handoff_files");
        this.entries = schema + "." + entriesTable;
        this.usage = schema + "." + usageTable;
        this.events = schema + "." + eventsTable;
        this.handoffs = schema + "." + handoffsTable;
        this.handoffFiles = schema + "." + handoffFilesTable;
        if (sql.initializeSchema()) initializeSchema();
    }

    void create(WorkspaceRecord workspace, long maxEntries) {
        tx(() -> {
            Integer count = jdbc.queryForObject("select count(*) from " + entries + " where state <> 'DELETED'", Integer.class);
            if (count != null && count >= maxEntries) throw new ToolStorageException("The storage workspace limit has been reached.");
            jdbc.update("insert into " + entries + " (session_id, request_id, tool_id, state, byte_size, remaining_requests, active_streams, created_at, expires_at, updated_at) values (?, ?, ?, ?, 0, ?, 0, ?, ?, ?)",
                    workspace.sessionId(), workspace.requestId(), workspace.toolId(), WorkspaceState.STAGING.name(), workspace.remainingRequests(), workspace.createdAt(), workspace.expiresAt(), workspace.createdAt());
        });
    }

    long stagingBytes(String sessionId, String requestId) {
        Long value = jdbc.queryForObject("select coalesce(sum(byte_size), 0) from " + usage + " where session_id = ? and request_id = ?", Long.class, sessionId, requestId);
        return value == null ? 0 : value;
    }

    long totalStagingBytes() {
        Long value = jdbc.queryForObject("select coalesce(sum(f.byte_size), 0) from " + usage + " f join " + entries + " w on w.session_id = f.session_id and w.request_id = f.request_id where w.state in ('STAGING','HANDOFF_QUEUED','HANDOFF_IN_PROGRESS','HANDOFF_FAILED')", Long.class);
        return value == null ? 0 : value;
    }

    boolean isStaging(String sessionId, String requestId) {
        return tx(() -> locked(sessionId, requestId).map(workspace -> workspace.state() == WorkspaceState.STAGING).orElse(false));
    }

    void addFile(String sessionId, String requestId, WorkspaceFileRecord file) {
        tx(() -> jdbc.update("insert into " + usage + " (session_id, request_id, relative_path, mime_type, byte_size, checksum_sha256) values (?, ?, ?, ?, ?, ?)",
                sessionId, requestId, file.relativePath(), file.mimeType(), file.byteSize(), file.checksumSha256()));
    }

    List<WorkspaceFileRecord> files(String sessionId, String requestId) {
        return jdbc.query("select relative_path, mime_type, byte_size, checksum_sha256 from " + usage + " where session_id = ? and request_id = ? order by relative_path", this::file, sessionId, requestId);
    }

    WorkspaceRecord publish(String sessionId, String requestId, long maxPublishedBytes, OffsetDateTime now) {
        return tx(() -> {
            WorkspaceRecord workspace = locked(sessionId, requestId).orElseThrow(() -> unavailable());
            if (workspace.state() != WorkspaceState.STAGING) throw new ToolStorageException("The workspace is not awaiting publication.");
            long total = stagingBytes(sessionId, requestId);
            Long published = jdbc.queryForObject("select coalesce(sum(byte_size), 0) from " + entries + " where state = 'AVAILABLE'", Long.class);
            if ((published == null ? 0 : published) > maxPublishedBytes - total) throw new ToolStorageException("The storage published quota is full.");
            jdbc.update("update " + entries + " set state = ?, byte_size = ?, updated_at = ? where session_id = ? and request_id = ?", WorkspaceState.AVAILABLE.name(), total, now, sessionId, requestId);
            return locked(sessionId, requestId).orElseThrow(() -> unavailable());
        });
    }

    WorkspaceRecord handoff(String sessionId, String requestId, ExternalHandoffPublisher.HandoffReceipt receipt, OffsetDateTime now) {
        return tx(() -> {
            WorkspaceRecord workspace = locked(sessionId, requestId).orElseThrow(this::unavailable);
            if (workspace.state() != WorkspaceState.STAGING) throw new ToolStorageException("The workspace is not awaiting publication.");
            long total = stagingBytes(sessionId, requestId);
            jdbc.update("update " + entries + " set state = ?, byte_size = ?, updated_at = ? where session_id = ? and request_id = ?", WorkspaceState.HANDED_OFF.name(), total, now, sessionId, requestId);
            jdbc.update("insert into " + events + " (session_id, request_id, event_type, target, provider_receipt, created_at) values (?, ?, ?, ?, ?, ?)", sessionId, requestId, "HANDOFF_COMPLETED", receipt.target(), receipt.providerReceipt(), now);
            return locked(sessionId, requestId).orElseThrow(this::unavailable);
        });
    }

    /** Atomically turns complete local staging into durable async external work. */
    WorkspaceRecord enqueueHandoff(String sessionId, String requestId, String target, OffsetDateTime now) {
        return tx(() -> {
            WorkspaceRecord workspace = locked(sessionId, requestId).orElseThrow(this::unavailable);
            if (workspace.state() != WorkspaceState.STAGING) throw new ToolStorageException("The workspace is not awaiting publication.");
            long total = stagingBytes(sessionId, requestId);
            jdbc.update("update " + entries + " set state = ?, byte_size = ?, updated_at = ? where session_id = ? and request_id = ?",
                    WorkspaceState.HANDOFF_QUEUED.name(), total, now, sessionId, requestId);
            jdbc.update("insert into " + handoffs + " (session_id, request_id, target, state, attempts, next_attempt_at, lease_until, last_error, completed_at, provider_receipt, created_at, updated_at) values (?, ?, ?, ?, 0, ?, null, null, null, null, ?, ?)",
                    sessionId, requestId, target, HandoffJobState.QUEUED.name(), now, now, now);
            event(sessionId, requestId, "HANDOFF_QUEUED", target, null, now);
            return locked(sessionId, requestId).orElseThrow(this::unavailable);
        });
    }

    /** Claims one due job or an expired lease. The lease makes restart recovery safe and explicit. */
    Optional<ClaimedHandoff> claimHandoff(OffsetDateTime now, Duration leaseDuration) {
        return tx(() -> {
            List<HandoffJob> jobs = jdbc.query("select * from " + handoffs + " where (state = 'QUEUED' and next_attempt_at <= ?) or (state = 'IN_PROGRESS' and lease_until <= ?) order by next_attempt_at limit 1 for update", this::handoffJob, now, now);
            if (jobs.isEmpty()) return Optional.empty();
            HandoffJob job = jobs.getFirst();
            OffsetDateTime leaseUntil = now.plus(leaseDuration);
            int attempts = job.attempts() + 1;
            jdbc.update("update " + handoffs + " set state = ?, attempts = ?, lease_until = ?, updated_at = ? where session_id = ? and request_id = ?",
                    HandoffJobState.IN_PROGRESS.name(), attempts, leaseUntil, now, job.sessionId(), job.requestId());
            jdbc.update("update " + entries + " set state = ?, updated_at = ? where session_id = ? and request_id = ?",
                    WorkspaceState.HANDOFF_IN_PROGRESS.name(), now, job.sessionId(), job.requestId());
            event(job.sessionId(), job.requestId(), "HANDOFF_IN_PROGRESS", job.target(), null, now);
            WorkspaceRecord workspace = locked(job.sessionId(), job.requestId()).orElseThrow(this::unavailable);
            return Optional.of(new ClaimedHandoff(workspace, new HandoffJob(job.sessionId(), job.requestId(), job.target(), HandoffJobState.IN_PROGRESS, attempts, job.nextAttemptAt(), leaseUntil, job.lastError()), files(job.sessionId(), job.requestId()), acceptedHandoffFiles(job.sessionId(), job.requestId())));
        });
    }

    void handoffFileAccepted(String sessionId, String requestId, String relativePath, OffsetDateTime now) {
        tx(() -> {
            int updated = jdbc.update("update " + handoffFiles + " set accepted_at = ? where session_id = ? and request_id = ? and relative_path = ?", now, sessionId, requestId, relativePath);
            if (updated == 0) jdbc.update("insert into " + handoffFiles + " (session_id, request_id, relative_path, accepted_at) values (?, ?, ?, ?)", sessionId, requestId, relativePath, now);
        });
    }

    void completeHandoff(HandoffJob job, ExternalHandoffPublisher.HandoffReceipt receipt, OffsetDateTime now) {
        tx(() -> {
            Integer manifest = jdbc.queryForObject("select count(*) from " + handoffFiles + " where session_id = ? and request_id = ? and relative_path = 'manifest.json'", Integer.class, job.sessionId(), job.requestId());
            if (manifest == null || manifest == 0) throw new ToolStorageException("External handoff did not record the completion manifest.");
            jdbc.update("update " + handoffs + " set state = ?, lease_until = null, completed_at = ?, provider_receipt = ?, updated_at = ? where session_id = ? and request_id = ?",
                    HandoffJobState.COMPLETED.name(), now, receipt.providerReceipt(), now, job.sessionId(), job.requestId());
            jdbc.update("update " + entries + " set state = ?, updated_at = ? where session_id = ? and request_id = ?", WorkspaceState.HANDED_OFF.name(), now, job.sessionId(), job.requestId());
            event(job.sessionId(), job.requestId(), "HANDOFF_COMPLETED", receipt.target(), receipt.providerReceipt(), now);
        });
    }

    void handoffAttemptFailed(HandoffJob job, String detail, boolean retryable, int maxAttempts, Duration retryDelay, OffsetDateTime now) {
        tx(() -> {
            boolean retry = retryable && job.attempts() < maxAttempts;
            HandoffJobState state = retry ? HandoffJobState.QUEUED : HandoffJobState.FAILED;
            WorkspaceState workspaceState = retry ? WorkspaceState.HANDOFF_QUEUED : WorkspaceState.HANDOFF_FAILED;
            OffsetDateTime next = retry ? now.plus(retryDelay) : now;
            jdbc.update("update " + handoffs + " set state = ?, next_attempt_at = ?, lease_until = null, last_error = ?, updated_at = ? where session_id = ? and request_id = ?",
                    state.name(), next, detail, now, job.sessionId(), job.requestId());
            jdbc.update("update " + entries + " set state = ?, updated_at = ? where session_id = ? and request_id = ?", workspaceState.name(), now, job.sessionId(), job.requestId());
            event(job.sessionId(), job.requestId(), retry ? "HANDOFF_RETRY_SCHEDULED" : "HANDOFF_FAILED", job.target(), detail, now);
        });
    }

    Optional<HandoffJob> handoffStatus(String sessionId, String requestId) {
        return jdbc.query("select * from " + handoffs + " where session_id = ? and request_id = ?", this::handoffJob, sessionId, requestId).stream().findFirst();
    }

    Optional<WorkspaceRecord> workspaceStatus(String sessionId, String requestId) {
        return jdbc.query("select * from " + entries + " where session_id = ? and request_id = ?", this::workspace, sessionId, requestId).stream().findFirst();
    }

    WorkspaceRecord requeueFailedHandoff(String sessionId, String requestId, OffsetDateTime now) {
        return tx(() -> {
            WorkspaceRecord workspace = locked(sessionId, requestId).orElseThrow(this::unavailable);
            if (workspace.state() != WorkspaceState.HANDOFF_FAILED) throw new ToolStorageException("Only a retained failed async handoff can be retried.");
            int updated = jdbc.update("update " + handoffs + " set state = ?, attempts = 0, next_attempt_at = ?, lease_until = null, last_error = null, updated_at = ? where session_id = ? and request_id = ? and state = ?",
                    HandoffJobState.QUEUED.name(), now, now, sessionId, requestId, HandoffJobState.FAILED.name());
            if (updated != 1) throw new ToolStorageException("The failed async handoff is unavailable for retry.");
            jdbc.update("update " + entries + " set state = ?, updated_at = ? where session_id = ? and request_id = ?", WorkspaceState.HANDOFF_QUEUED.name(), now, sessionId, requestId);
            event(sessionId, requestId, "HANDOFF_REQUEUED", handoffStatus(sessionId, requestId).map(HandoffJob::target).orElse(null), null, now);
            return locked(sessionId, requestId).orElseThrow(this::unavailable);
        });
    }

    void handoffFailed(String sessionId, String requestId, String target) {
        tx(() -> {
            jdbc.update("update " + entries + " set state = ?, updated_at = ? where session_id = ? and request_id = ?", WorkspaceState.FAILED.name(), OffsetDateTime.now(), sessionId, requestId);
            jdbc.update("insert into " + events + " (session_id, request_id, event_type, target, provider_receipt, created_at) values (?, ?, ?, ?, ?, ?)", sessionId, requestId, "HANDOFF_FAILED", target, null, OffsetDateTime.now());
        });
    }

    Optional<WorkspaceRecord> inspect(String sessionId, String requestId, OffsetDateTime now) {
        return tx(() -> available(locked(sessionId, requestId), now));
    }

    Optional<WorkspaceFileRecord> inspectFile(String sessionId, String requestId, String relativePath, OffsetDateTime now) {
        return tx(() -> available(locked(sessionId, requestId), now).flatMap(ignored -> findFile(sessionId, requestId, relativePath)));
    }

    Optional<WorkspaceFileRecord> admitFile(String sessionId, String requestId, String relativePath, OffsetDateTime now, int maxStreams) {
        return tx(() -> {
            Optional<WorkspaceRecord> available = available(locked(sessionId, requestId), now);
            if (available.isEmpty()) return Optional.empty();
            WorkspaceRecord workspace = available.get();
            if (workspace.remainingRequests() <= 0 || workspace.activeStreams() >= maxStreams) return Optional.empty();
            Optional<WorkspaceFileRecord> file = findFile(sessionId, requestId, relativePath);
            if (file.isEmpty() && !relativePath.equals("manifest.json")) return Optional.empty();
            int remaining = workspace.remainingRequests() - 1;
            WorkspaceState state = remaining == 0 ? WorkspaceState.EXHAUSTED : WorkspaceState.AVAILABLE;
            jdbc.update("update " + entries + " set remaining_requests = ?, active_streams = active_streams + 1, state = ?, updated_at = ? where session_id = ? and request_id = ?", remaining, state.name(), now, sessionId, requestId);
            return file.or(() -> Optional.of(new WorkspaceFileRecord("manifest.json", "application/json", 0, null)));
        });
    }

    void completeStream(String sessionId, String requestId) {
        tx(() -> jdbc.update("update " + entries + " set active_streams = case when active_streams > 0 then active_streams - 1 else 0 end, updated_at = ? where session_id = ? and request_id = ?", OffsetDateTime.now(), sessionId, requestId));
    }

    List<WorkspaceRecord> cleanupCandidates(OffsetDateTime now, OffsetDateTime staleStagingBefore, int limit) {
        return tx(() -> {
            jdbc.update("update " + entries + " set state = ?, updated_at = ? where state = ? and created_at <= ?", WorkspaceState.FAILED.name(), now, WorkspaceState.STAGING.name(), staleStagingBefore);
            jdbc.update("update " + entries + " set state = ?, updated_at = ? where state = ? and expires_at <= ?", WorkspaceState.EXPIRED.name(), now, WorkspaceState.AVAILABLE.name(), now);
            return jdbc.query("select * from " + entries + " where (state in ('FAILED','EXPIRED','EXHAUSTED','HANDED_OFF') or (state = 'HANDOFF_FAILED' and expires_at <= ?)) and active_streams = 0 order by updated_at limit ?", this::workspace, now, limit);
        });
    }

    Optional<WorkspaceRecord> claimDeletion(String sessionId, String requestId) {
        return tx(() -> {
            Optional<WorkspaceRecord> found = locked(sessionId, requestId);
            if (found.isEmpty() || found.get().activeStreams() != 0 || !(found.get().state() == WorkspaceState.FAILED || found.get().state() == WorkspaceState.HANDOFF_FAILED || found.get().state() == WorkspaceState.EXPIRED || found.get().state() == WorkspaceState.EXHAUSTED || found.get().state() == WorkspaceState.HANDED_OFF)) return Optional.empty();
            jdbc.update("update " + entries + " set state = ?, updated_at = ? where session_id = ? and request_id = ?", WorkspaceState.DELETING.name(), OffsetDateTime.now(), sessionId, requestId);
            return locked(sessionId, requestId);
        });
    }

    void deleted(String sessionId, String requestId) { tx(() -> jdbc.update("update " + entries + " set state = ?, updated_at = ? where session_id = ? and request_id = ?", WorkspaceState.DELETED.name(), OffsetDateTime.now(), sessionId, requestId)); }
    void failed(String sessionId, String requestId) { tx(() -> jdbc.update("update " + entries + " set state = ?, updated_at = ? where session_id = ? and request_id = ?", WorkspaceState.FAILED.name(), OffsetDateTime.now(), sessionId, requestId)); }

    private Optional<WorkspaceRecord> available(Optional<WorkspaceRecord> found, OffsetDateTime now) {
        if (found.isEmpty()) return Optional.empty();
        WorkspaceRecord workspace = found.get();
        if (workspace.state() != WorkspaceState.AVAILABLE || !workspace.expiresAt().isAfter(now)) {
            if (workspace.state() == WorkspaceState.AVAILABLE && !workspace.expiresAt().isAfter(now)) jdbc.update("update " + entries + " set state = ?, updated_at = ? where session_id = ? and request_id = ?", WorkspaceState.EXPIRED.name(), now, workspace.sessionId(), workspace.requestId());
            return Optional.empty();
        }
        return Optional.of(workspace);
    }
    private Optional<WorkspaceRecord> locked(String sessionId, String requestId) { return jdbc.query("select * from " + entries + " where session_id = ? and request_id = ? for update", this::workspace, sessionId, requestId).stream().findFirst(); }
    private Optional<WorkspaceFileRecord> findFile(String sessionId, String requestId, String path) { return jdbc.query("select relative_path, mime_type, byte_size, checksum_sha256 from " + usage + " where session_id = ? and request_id = ? and relative_path = ?", this::file, sessionId, requestId, path).stream().findFirst(); }
    private Set<String> acceptedHandoffFiles(String sessionId, String requestId) { return Set.copyOf(jdbc.query("select relative_path from " + handoffFiles + " where session_id = ? and request_id = ?", (rs, row) -> rs.getString(1), sessionId, requestId)); }
    private WorkspaceRecord workspace(ResultSet rs, int row) throws java.sql.SQLException { return new WorkspaceRecord(rs.getString("session_id"), rs.getString("request_id"), rs.getString("tool_id"), WorkspaceState.valueOf(rs.getString("state")), rs.getLong("byte_size"), rs.getInt("remaining_requests"), rs.getInt("active_streams"), rs.getObject("created_at", OffsetDateTime.class), rs.getObject("expires_at", OffsetDateTime.class)); }
    private WorkspaceFileRecord file(ResultSet rs, int row) throws java.sql.SQLException { return new WorkspaceFileRecord(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getString(4)); }
    private HandoffJob handoffJob(ResultSet rs, int row) throws java.sql.SQLException { return new HandoffJob(rs.getString("session_id"), rs.getString("request_id"), rs.getString("target"), HandoffJobState.valueOf(rs.getString("state")), rs.getInt("attempts"), rs.getObject("next_attempt_at", OffsetDateTime.class), rs.getObject("lease_until", OffsetDateTime.class), rs.getString("last_error")); }
    private void event(String sessionId, String requestId, String eventType, String target, String receipt, OffsetDateTime now) { jdbc.update("insert into " + events + " (session_id, request_id, event_type, target, provider_receipt, created_at) values (?, ?, ?, ?, ?, ?)", sessionId, requestId, eventType, target, receipt, now); }
    private void initializeSchema() {
        jdbc.execute("create schema if not exists " + schema);
        ensureTable(entries, entriesTable,
                "create table if not exists " + entries + " (session_id varchar(128) not null, request_id varchar(128) not null, tool_id varchar(255) not null, state varchar(24) not null, byte_size bigint not null, remaining_requests integer not null, active_streams integer not null, created_at timestamp with time zone not null, expires_at timestamp with time zone not null, updated_at timestamp with time zone not null, primary key (session_id, request_id))",
                Set.of("session_id", "request_id", "tool_id", "state", "byte_size", "remaining_requests", "active_streams", "created_at", "expires_at", "updated_at"));
        ensureTable(usage, usageTable,
                "create table if not exists " + usage + " (session_id varchar(128) not null, request_id varchar(128) not null, relative_path varchar(1024) not null, mime_type varchar(255) not null, byte_size bigint not null, checksum_sha256 char(64), primary key (session_id, request_id, relative_path), foreign key (session_id, request_id) references " + entries + " (session_id, request_id))",
                Set.of("session_id", "request_id", "relative_path", "mime_type", "byte_size", "checksum_sha256"));
        ensureTable(events, eventsTable,
                "create table if not exists " + events + " (id bigint generated by default as identity primary key, session_id varchar(128) not null, request_id varchar(128) not null, event_type varchar(64) not null, target varchar(255), provider_receipt varchar(1024), created_at timestamp with time zone not null)",
                Set.of("id", "session_id", "request_id", "event_type", "target", "provider_receipt", "created_at"));
        ensureTable(handoffs, handoffsTable,
                "create table if not exists " + handoffs + " (session_id varchar(128) not null, request_id varchar(128) not null, target varchar(255) not null, state varchar(24) not null, attempts integer not null, next_attempt_at timestamp with time zone not null, lease_until timestamp with time zone, last_error varchar(2048), completed_at timestamp with time zone, provider_receipt varchar(1024), created_at timestamp with time zone not null, updated_at timestamp with time zone not null, primary key (session_id, request_id), foreign key (session_id, request_id) references " + entries + " (session_id, request_id))",
                Set.of("session_id", "request_id", "target", "state", "attempts", "next_attempt_at", "lease_until", "last_error", "completed_at", "provider_receipt", "created_at", "updated_at"));
        ensureTable(handoffFiles, handoffFilesTable,
                "create table if not exists " + handoffFiles + " (session_id varchar(128) not null, request_id varchar(128) not null, relative_path varchar(1024) not null, accepted_at timestamp with time zone not null, primary key (session_id, request_id, relative_path), foreign key (session_id, request_id) references " + entries + " (session_id, request_id))",
                Set.of("session_id", "request_id", "relative_path", "accepted_at"));
        jdbc.execute("create index if not exists " + id("storage_entries_state_expiry_idx") + " on " + entries + " (state, expires_at)");
        jdbc.execute("create index if not exists " + id("storage_handoffs_due_idx") + " on " + handoffs + " (state, next_attempt_at)");
    }
    private void ensureTable(String qualifiedTable, String table, String createSql, Set<String> expectedColumns) {
        jdbc.execute(createSql);
        Map<String, ColumnDefinition> columns = columns(table);
        boolean missingExpectedColumn = !columns.keySet().containsAll(expectedColumns);
        boolean hasRequiredLegacyColumn = columns.entrySet().stream().anyMatch(entry -> !expectedColumns.contains(entry.getKey()) && entry.getValue().required());
        if (!missingExpectedColumn && !hasRequiredLegacyColumn) return;
        String legacy = legacyTableName(table);
        jdbc.execute("alter table " + qualifiedTable + " rename to " + legacy);
        jdbc.execute(createSql);
        LOGGER.warn("Archived incompatible storage metadata table {} as {}.{}; existing rows were preserved but are not used by the current storage lifecycle.", qualifiedTable, schema, legacy);
    }
    private Map<String, ColumnDefinition> columns(String table) {
        return jdbc.execute((ConnectionCallback<Map<String, ColumnDefinition>>) connection -> {
            DatabaseMetaData metadata = connection.getMetaData();
            String metadataSchema = metadata.storesUpperCaseIdentifiers() ? schema.toUpperCase(java.util.Locale.ROOT) : schema;
            String metadataTable = metadata.storesUpperCaseIdentifiers() ? table.toUpperCase(java.util.Locale.ROOT) : table;
            Map<String, ColumnDefinition> result = new LinkedHashMap<>();
            try (ResultSet resultSet = metadata.getColumns(connection.getCatalog(), metadataSchema, metadataTable, null)) {
                while (resultSet.next()) {
                    String name = resultSet.getString("COLUMN_NAME").toLowerCase(java.util.Locale.ROOT);
                    result.put(name, new ColumnDefinition(resultSet.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls && resultSet.getString("COLUMN_DEF") == null));
                }
            }
            return result;
        });
    }
    private String legacyTableName(String table) {
        for (int suffix = 1; suffix <= 999; suffix++) {
            String candidate = id(table + "_legacy_" + suffix);
            if (columns(candidate).isEmpty()) return candidate;
        }
        throw new ToolStorageException("Unable to archive an incompatible storage metadata table.");
    }
    private record ColumnDefinition(boolean required) { }
    private static String id(String value) { if (value == null || !IDENTIFIER.matcher(value).matches()) throw new IllegalArgumentException("Invalid storage SQL identifier."); return value; }
    private ToolStorageException unavailable() { return new ToolStorageException("Storage workspace is unavailable."); }
    private void tx(Runnable work) {
        try {
            transactions.executeWithoutResult(status -> {
                try {
                    work.run();
                } catch (ToolStorageException exception) {
                    throw exception;
                } catch (Exception exception) {
                    throw new ToolStorageException("Storage metadata operation failed.", exception);
                }
            });
        } catch (ToolStorageException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ToolStorageException("Storage metadata operation failed.", exception);
        }
    }
    private <T> T tx(java.util.concurrent.Callable<T> work) { return transactions.execute(status -> { try { return work.call(); } catch (ToolStorageException exception) { throw exception; } catch (Exception exception) { throw new ToolStorageException("Storage metadata operation failed.", exception); } }); }

    record ClaimedHandoff(WorkspaceRecord workspace, HandoffJob job, List<WorkspaceFileRecord> files, Set<String> acceptedFiles) { }
}

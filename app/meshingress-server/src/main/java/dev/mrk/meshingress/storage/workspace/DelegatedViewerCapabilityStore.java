package dev.mrk.meshingress.storage.workspace;

import dev.mrk.meshingress.api.storage.ToolStorageException;
import dev.mrk.meshingress.config.MeshingressProperties;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

/** Persists opaque, bounded viewer capabilities without retaining copied workspace bytes. */
public final class DelegatedViewerCapabilityStore {
    public record Capability(String token, String requestId, String toolId, OffsetDateTime expiresAt, int remainingRequests) { }

    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;
    private final String table;

    public DelegatedViewerCapabilityStore(JdbcTemplate jdbc, MeshingressProperties.Storage.Sql sql) {
        this.jdbc = jdbc;
        String schema = identifier(sql.schema());
        this.table = schema + "." + identifier(sql.table().entries() + "_delegated_viewers");
        if (sql.initializeSchema()) {
            jdbc.execute("create table if not exists " + table + " (request_id varchar(128) primary key, tool_id varchar(128) not null, token_hash varchar(64) not null unique, expires_at timestamp with time zone not null, remaining_requests integer not null)");
        }
    }

    public Capability issue(String requestId, String toolId, OffsetDateTime expiresAt, int maxRequests) {
        if (requestId == null || requestId.isBlank() || toolId == null || toolId.isBlank() || expiresAt == null || maxRequests < 1) {
            throw new ToolStorageException("A valid delegated viewer capability is required.");
        }
        jdbc.update("delete from " + table + " where expires_at <= ?", OffsetDateTime.now());
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        try {
            jdbc.update("insert into " + table + " (request_id, tool_id, token_hash, expires_at, remaining_requests) values (?, ?, ?, ?, ?)",
                    requestId, toolId, hash(token), expiresAt, maxRequests);
            return new Capability(token, requestId, toolId, expiresAt, maxRequests);
        } catch (Exception exception) {
            throw new ToolStorageException("The delegated viewer capability could not be created.", exception);
        }
    }

    public Capability claim(String token) {
        if (token == null || token.isBlank()) throw unavailable();
        String tokenHash = hash(token);
        var rows = jdbc.query("select request_id, tool_id, expires_at, remaining_requests from " + table + " where token_hash = ?", (result, row) ->
                new Capability(token, result.getString(1), result.getString(2), result.getObject(3, OffsetDateTime.class), result.getInt(4)), tokenHash);
        Capability capability = rows.stream().findFirst().orElseThrow(this::unavailable);
        if (capability.expiresAt().isBefore(OffsetDateTime.now()) || capability.remainingRequests() < 1
                || jdbc.update("update " + table + " set remaining_requests = remaining_requests - 1 where token_hash = ? and expires_at > ? and remaining_requests > 0", tokenHash, OffsetDateTime.now()) != 1) {
            throw unavailable();
        }
        return capability;
    }

    public Optional<Capability> findByRequest(String requestId) {
        return jdbc.query("select token_hash, tool_id, expires_at, remaining_requests from " + table + " where request_id = ?", (result, row) ->
                new Capability("", requestId, result.getString(2), result.getObject(3, OffsetDateTime.class), result.getInt(4)), requestId).stream().findFirst();
    }

    private ToolStorageException unavailable() { return new ToolStorageException("The delegated viewer file is unavailable."); }
    private static String hash(String value) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException("SHA-256 is unavailable.", exception); }
    }
    private static String identifier(String value) {
        if (value == null || !value.matches("[A-Za-z_][A-Za-z0-9_]*")) throw new IllegalArgumentException("Invalid storage SQL identifier.");
        return value;
    }
}

package dev.mrk.meshingress.security.management;

import dev.mrk.meshingress.api.McpPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Durable policy lifecycle plus the evaluator shared by listing and tool calls. */
@Service
public final class ToolPolicyService {
    private static final String TABLE = "meshingress.security_tool_policies";
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public ToolPolicyService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this(jdbc, objectMapper, Clock.systemUTC());
    }

    ToolPolicyService(JdbcTemplate jdbc, ObjectMapper objectMapper, Clock clock) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.clock = clock;
        jdbc.execute("create schema if not exists meshingress");
        jdbc.execute("create table if not exists " + TABLE + " (policy_id varchar(36) primary key, tenant_id varchar(255) not null, tool_name varchar(512) not null, priority integer not null, enabled boolean not null, revision bigint not null, payload clob not null)");
        jdbc.execute("create index if not exists security_tool_policies_match_idx on " + TABLE + " (tenant_id, tool_name, enabled, priority)");
    }

    public PolicySnapshot create(String tenantId, String toolName, StoredToolPolicy.Effect effect, Set<String> requiredGrants, int priority, boolean enabled) {
        Instant now = Instant.now(clock);
        StoredToolPolicy policy = new StoredToolPolicy(UUID.randomUUID(), tenantId, toolName, effect, requiredGrants, priority, enabled, now, now);
        try {
            jdbc.update("insert into " + TABLE + " (policy_id, tenant_id, tool_name, priority, enabled, revision, payload) values (?, ?, ?, ?, ?, ?, ?)",
                    policy.policyId().toString(), policy.tenantId(), policy.toolName(), policy.priority(), policy.enabled(), 0L, objectMapper.writeValueAsString(policy));
            return new PolicySnapshot(policy, 0L);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to persist tool policy", exception);
        }
    }

    public Optional<PolicySnapshot> find(String tenantId, UUID policyId) {
        return jdbc.query("select revision, payload from " + TABLE + " where tenant_id = ? and policy_id = ?", (row, number) -> snapshot(row.getLong(1), row.getString(2)), tenantId, policyId.toString()).stream().findFirst();
    }

    public List<PolicySnapshot> list(String tenantId) {
        return List.copyOf(jdbc.query("select revision, payload from " + TABLE + " where tenant_id = ? order by tool_name, priority desc, policy_id", (row, number) -> snapshot(row.getLong(1), row.getString(2)), tenantId));
    }

    public PolicySnapshot update(String tenantId, UUID policyId, long expectedRevision, StoredToolPolicy.Effect effect, Set<String> requiredGrants, int priority, boolean enabled) {
        PolicySnapshot current = require(tenantId, policyId);
        if (current.revision() != expectedRevision) throw new PolicyRevisionConflictException(policyId, expectedRevision, current.revision());
        StoredToolPolicy existing = current.policy();
        StoredToolPolicy replacement = new StoredToolPolicy(existing.policyId(), existing.tenantId(), existing.toolName(), effect, requiredGrants, priority, enabled, existing.createdAt(), Instant.now(clock));
        try {
            int changed = jdbc.update("update " + TABLE + " set priority = ?, enabled = ?, revision = revision + 1, payload = ? where policy_id = ? and tenant_id = ? and revision = ?",
                    replacement.priority(), replacement.enabled(), objectMapper.writeValueAsString(replacement), policyId.toString(), tenantId, expectedRevision);
            if (changed == 1) return new PolicySnapshot(replacement, Math.addExact(expectedRevision, 1));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to update tool policy", exception);
        }
        PolicySnapshot actual = require(tenantId, policyId);
        throw new PolicyRevisionConflictException(policyId, expectedRevision, actual.revision());
    }

    public void delete(String tenantId, UUID policyId, long expectedRevision) {
        PolicySnapshot current = require(tenantId, policyId);
        if (current.revision() != expectedRevision) throw new PolicyRevisionConflictException(policyId, expectedRevision, current.revision());
        if (jdbc.update("delete from " + TABLE + " where policy_id = ? and tenant_id = ? and revision = ?", policyId.toString(), tenantId, expectedRevision) != 1) {
            PolicySnapshot actual = require(tenantId, policyId);
            throw new PolicyRevisionConflictException(policyId, expectedRevision, actual.revision());
        }
    }

    public ToolPolicyDecision evaluate(String toolName, McpPrincipal principal) {
        if (principal == null || principal.tenantId().isBlank()) return ToolPolicyDecision.allow(List.of());
        List<PolicySnapshot> matching = list(principal.tenantId()).stream()
                .filter(snapshot -> snapshot.policy().enabled() && snapshot.policy().toolName().equals(toolName))
                .toList();
        if (matching.isEmpty()) return ToolPolicyDecision.allow(List.of());
        int highest = matching.stream().map(snapshot -> snapshot.policy().priority()).max(Comparator.naturalOrder()).orElseThrow();
        List<PolicySnapshot> selected = matching.stream().filter(snapshot -> snapshot.policy().priority() == highest).toList();
        List<UUID> ids = selected.stream().map(snapshot -> snapshot.policy().policyId()).toList();
        if (selected.stream().anyMatch(snapshot -> snapshot.policy().effect() == StoredToolPolicy.Effect.DENY)) {
            return ToolPolicyDecision.deny(ids, "Tool execution is denied by policy.");
        }
        boolean allowed = selected.stream().anyMatch(snapshot -> principal.grants().containsAll(snapshot.policy().requiredGrants()));
        return allowed ? ToolPolicyDecision.allow(ids) : ToolPolicyDecision.deny(ids, "Tool execution requires a missing grant.");
    }

    private PolicySnapshot require(String tenantId, UUID policyId) {
        return find(tenantId, policyId).orElseThrow(() -> new IllegalArgumentException("unknown tool policy: " + policyId));
    }

    private PolicySnapshot snapshot(long revision, String payload) {
        try { return new PolicySnapshot(objectMapper.readValue(payload, StoredToolPolicy.class), revision); }
        catch (Exception exception) { throw new IllegalStateException("Stored tool policy payload is invalid", exception); }
    }

    public record PolicySnapshot(StoredToolPolicy policy, long revision) { }
    public record ToolPolicyDecision(boolean allowed, List<UUID> matchedPolicyIds, String reason) {
        static ToolPolicyDecision allow(List<UUID> ids) { return new ToolPolicyDecision(true, List.copyOf(ids), ""); }
        static ToolPolicyDecision deny(List<UUID> ids, String reason) { return new ToolPolicyDecision(false, List.copyOf(ids), reason); }
    }
    public static final class PolicyRevisionConflictException extends IllegalStateException {
        PolicyRevisionConflictException(UUID policyId, long expected, long actual) { super("tool policy revision conflict for " + policyId + ": expected " + expected + ", actual " + actual); }
    }
}

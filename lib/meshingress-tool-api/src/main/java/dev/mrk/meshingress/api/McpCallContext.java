package dev.mrk.meshingress.api;

import dev.mrk.meshingress.api.result.progress.McpProgressReporter;
import dev.mrk.meshingress.api.services.McpServiceResolver;

import java.util.Objects;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.UUID;

/**
 * Tool-facing MCP context. It contains verified identity and execution data,
 * never raw Authorization values, cookies, headers, or outbound credentials.
 */
public record McpCallContext(
        McpRequestIds requestIds,
        McpPrincipal principal,
        McpClientMetadata client,
        McpCallLineage lineage,
        McpExecutionControl execution,
        McpServiceResolver services,
        Map<String, Object> serviceRequirements
) {
    public McpCallContext {
        requestIds = Objects.requireNonNullElseGet(requestIds, () -> new McpRequestIds("", "", UUID.randomUUID().toString(), ""));
        principal = Objects.requireNonNullElseGet(principal, McpPrincipal::anonymous);
        client = Objects.requireNonNullElseGet(client, McpClientMetadata::unknown);
        lineage = Objects.requireNonNullElseGet(lineage, McpCallLineage::root);
        execution = Objects.requireNonNullElseGet(execution, McpExecutionControl::none);
        services = Objects.requireNonNullElseGet(services, McpServiceResolver::none);
        serviceRequirements = serviceRequirements == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(serviceRequirements));
    }

    public McpCallContext(McpRequestIds requestIds, McpPrincipal principal, McpClientMetadata client, McpCallLineage lineage, McpExecutionControl execution) {
        this(requestIds, principal, client, lineage, execution, McpServiceResolver.none(), Map.of());
    }

    /** Source-compatible constructor for callers that supply a service resolver. */
    public McpCallContext(McpRequestIds requestIds, McpPrincipal principal, McpClientMetadata client, McpCallLineage lineage, McpExecutionControl execution, McpServiceResolver services) {
        this(requestIds, principal, client, lineage, execution, services, Map.of());
    }

    /** Source-compatible test/module constructor. Header values are intentionally discarded. */
    @Deprecated
    public McpCallContext(String ignoredAuthorization, String ignoredRole, String sessionId, String requestId) {
        this(new McpRequestIds(sessionId, requestId, UUID.randomUUID().toString(), ""), McpPrincipal.anonymous(), McpClientMetadata.unknown(), McpCallLineage.root(), McpExecutionControl.none());
    }

    /** Source-compatible test/module constructor. Header values are intentionally discarded. */
    @Deprecated
    public McpCallContext(String ignoredAuthorization, String ignoredRole, String sessionId, String requestId, McpProgressReporter reporter) {
        this(new McpRequestIds(sessionId, requestId, UUID.randomUUID().toString(), ""), McpPrincipal.anonymous(), McpClientMetadata.unknown(), McpCallLineage.root(), new McpExecutionControl(null, null, reporter));
    }

    public String sessionId() { return requestIds.sessionId(); }
    public String requestId() { return requestIds.correlationId(); }
    public String invocationId() { return requestIds.invocationId(); }
    public McpProgressReporter progressReporter() { return execution.progressReporter(); }
    public McpCallContext withExecution(McpExecutionControl next) { return new McpCallContext(requestIds, principal, client, lineage, next, services, serviceRequirements); }
    public McpCallContext withServices(McpServiceResolver next) { return new McpCallContext(requestIds, principal, client, lineage, execution, next, serviceRequirements); }
    public McpCallContext withServiceRequirements(Map<String, Object> next) { return new McpCallContext(requestIds, principal, client, lineage, execution, services, next); }

    public McpCallContext deriveWorkflowChild(String runId, String nodeId, int attempt) {
        return new McpCallContext(
                new McpRequestIds(sessionId(), requestId(), UUID.randomUUID().toString(), ""),
                principal, client,
                new McpCallLineage(invocationId(), runId, nodeId, attempt),
                McpExecutionControl.none(), services, serviceRequirements
        );
    }
}

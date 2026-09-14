package dev.mrk.meshingress.security;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.McpCallLineage;
import dev.mrk.meshingress.api.McpClientMetadata;
import dev.mrk.meshingress.api.McpExecutionControl;
import dev.mrk.meshingress.api.McpPrincipal;
import dev.mrk.meshingress.api.McpRequestIds;
import dev.mrk.meshingress.api.services.McpServiceResolver;
import dev.mrk.meshingress.mcp.McpInvocation;
import dev.mrk.meshingress.mcp.McpInvocationFactory;
import dev.mrk.meshingress.mcp.services.McpServiceRegistry;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

import java.util.UUID;
import java.util.Map;

/** The only server entry point that turns transport evidence into tool-facing context. */
@Component
public class McpTransportContextFactory {
    private final McpPrincipalResolver principalResolver;
    private final McpServiceResolver services;

    @Autowired
    public McpTransportContextFactory(McpPrincipalResolver principalResolver, McpServiceRegistry services) {
        this.principalResolver = principalResolver;
        this.services = services;
    }

    /** Compatibility constructor for isolated web/controller tests. */
    public McpTransportContextFactory(McpPrincipalResolver principalResolver) {
        this.principalResolver = principalResolver;
        this.services = McpServiceResolver.none();
    }

    public McpInvocationFactory http(McpTransportEvidence evidence) {
        McpPrincipal principal = principalResolver.resolve(evidence);
        return request -> McpInvocation.http(build(evidence, principal, request));
    }

    public McpCallContext create(McpTransportEvidence evidence, JsonNode request) {
        return build(evidence, principalResolver.resolve(evidence), request);
    }

    /** Captures the handshake-verified WebSocket principal for later message dispatch. */
    public McpCallContext create(McpTransportEvidence evidence, McpPrincipal principal, JsonNode request) {
        return build(evidence, principal == null ? McpPrincipal.anonymous() : principal, request);
    }

    public McpPrincipal resolve(McpTransportEvidence evidence) { return principalResolver.resolve(evidence); }

    public boolean authenticated(McpTransportEvidence evidence) {
        return resolve(evidence).authenticated();
    }

    private McpCallContext build(McpTransportEvidence evidence, McpPrincipal principal, JsonNode request) {
        String callId = request == null || !request.has("id") ? "" : request.path("id").asString("");
        return new McpCallContext(
                new McpRequestIds(evidence.sessionId(), evidence.correlationId(), UUID.randomUUID().toString(), callId),
                principal, McpClientMetadata.unknown(), McpCallLineage.root(), McpExecutionControl.none(), services, Map.of()
        );
    }
}

package dev.mrk.meshingress.security;

import dev.mrk.meshingress.api.McpPrincipal;

/** Verifies server-only transport evidence and returns a credential-safe principal. */
@FunctionalInterface
public interface McpPrincipalResolver {
    McpPrincipal resolve(McpTransportEvidence evidence);
}

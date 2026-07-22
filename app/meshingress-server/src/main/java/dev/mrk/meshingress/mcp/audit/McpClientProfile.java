package dev.mrk.meshingress.mcp.audit;

import java.time.OffsetDateTime;

/**
 * Client metadata reported in an MCP initialize request.
 *
 * <p>This is trace metadata, not an authenticated identity. Authentication is
 * added separately and must bind a verified principal to an audit event.</p>
 */
public record McpClientProfile(
        String name,
        String version,
        String protocolVersion,
        OffsetDateTime observedAt
) {
}

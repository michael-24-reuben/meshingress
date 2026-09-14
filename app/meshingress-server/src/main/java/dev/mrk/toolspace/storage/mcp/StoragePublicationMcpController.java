package dev.mrk.toolspace.storage.mcp;

import dev.mrk.meshingress.api.storage.ToolStorageException;
import dev.mrk.meshingress.api.storage.ToolStoragePublicationStatus;
import dev.mrk.meshingress.api.storage.ToolStorageService;
import dev.mrk.meshingress.api.services.McpServiceResolver;
import dev.mrk.meshingress.route.annotations.McpDispatchMapping;
import dev.mrk.meshingress.route.annotations.McpDispatchMethod;
import dev.mrk.meshingress.route.annotations.McpDispatchParam;
import dev.mrk.meshingress.route.api.McpDispatchErrorCodes;
import dev.mrk.meshingress.route.api.McpDispatchException;
import org.springframework.stereotype.Component;

/** MCP dispatch operations for publication state that is shared by tool workspaces. */
@Component
@McpDispatchMapping("storage")
public class StoragePublicationMcpController {

    private final McpServiceResolver services;

    public StoragePublicationMcpController(McpServiceResolver services) {
        this.services = services;
    }

    @McpDispatchMethod("publication-status")
    public ToolStoragePublicationStatus publicationStatus(
            @McpDispatchParam("params") StoragePublicationStatusParams params
    ) {
        if (params == null || isBlank(params.sessionId()) || isBlank(params.requestId())) {
            throw new McpDispatchException(
                    McpDispatchErrorCodes.INVALID_PARAMS,
                    "sessionId and requestId are required."
            );
        }
        try {
            return publicationStatus(params.sessionId().trim(), params.requestId().trim());
        } catch (ToolStorageException exception) {
            throw new McpDispatchException(
                    McpDispatchErrorCodes.INTERNAL_ERROR,
                    "Storage publication status is unavailable."
            );
        }
    }

    /**
     * Preserves the provider-neutral public status method while storage providers
     * remain independently registered MCP services.
     */
    private ToolStoragePublicationStatus publicationStatus(String sessionId, String requestId) {
        ToolStorageException lastFailure = null;
        for (ToolStorageService provider : services.findAll(ToolStorageService.class)) {
            try {
                return provider.publicationStatus(sessionId, requestId);
            } catch (ToolStorageException exception) {
                lastFailure = exception;
            }
        }
        throw lastFailure == null ? new ToolStorageException("Storage workspace is unavailable.") : lastFailure;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

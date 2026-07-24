package dev.mrk.meshingress.storage.mcp;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.storage.ToolStorageException;
import dev.mrk.meshingress.api.storage.ToolStorageFile;
import dev.mrk.meshingress.api.storage.ToolStorageFileRequest;
import dev.mrk.meshingress.api.storage.ToolStoragePublicationStatus;
import dev.mrk.meshingress.api.storage.ToolStorageService;
import dev.mrk.meshingress.api.storage.ToolStorageWorkspace;
import dev.mrk.meshingress.api.storage.ToolStorageWorkspaceRequest;
import dev.mrk.meshingress.route.api.McpDispatchErrorCodes;
import dev.mrk.meshingress.route.api.McpDispatchException;
import dev.mrk.meshingress.route.framework.dispatch.McpDispatchMethodScanner;
import dev.mrk.meshingress.route.framework.dispatch.McpDispatchRegistry;
import dev.mrk.meshingress.route.framework.dispatch.McpHandlerMethodInvoker;
import dev.mrk.meshingress.route.framework.dispatch.McpReturnValueAdapter;
import dev.mrk.meshingress.route.framework.dispatch.resolver.McpCallContextArgumentResolver;
import dev.mrk.meshingress.route.framework.dispatch.resolver.McpDispatchParamArgumentResolver;
import dev.mrk.meshingress.route.framework.dispatch.resolver.TypedJsonArgumentBinder;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StoragePublicationMcpControllerTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void exposesPublicationStatusAsStorageDispatchMethod() throws Exception {
        McpDispatchRegistry registry = registry((sessionId, requestId) -> new ToolStoragePublicationStatus(
                sessionId, requestId, "HANDOFF_QUEUED", "dav", 1, OffsetDateTime.now(), null
        ));

        JsonNode result = invoker().invoke(
                registry.find("storage/publication-status").orElseThrow(),
                objectMapper.readTree("""
                        { "sessionId": " session-1 ", "requestId": " req-test " }
                        """),
                new McpCallContext(null, null, null, null)
        );

        assertEquals("session-1", result.path("sessionId").asString());
        assertEquals("req-test", result.path("requestId").asString());
        assertEquals("HANDOFF_QUEUED", result.path("state").asString());
        assertEquals("dav", result.path("target").asString());
    }

    @Test
    void rejectsMissingPublicationWorkspaceIdentifiers() {
        McpDispatchRegistry registry = registry((sessionId, requestId) -> null);

        McpDispatchException exception = assertThrows(McpDispatchException.class, () -> invoker().invoke(
                registry.find("storage/publication-status").orElseThrow(),
                objectMapper.createObjectNode(),
                new McpCallContext(null, null, null, null)
        ));

        assertEquals(McpDispatchErrorCodes.INVALID_PARAMS, exception.code());
    }

    @Test
    void hidesStorageImplementationFailures() {
        McpDispatchRegistry registry = registry((sessionId, requestId) -> {
            throw new ToolStorageException("No workspace matches request");
        });

        McpDispatchException exception = assertThrows(McpDispatchException.class, () -> invoker().invoke(
                registry.find("storage/publication-status").orElseThrow(),
                objectMapper.readTree("{ \"sessionId\": \"session-1\", \"requestId\": \"req-test\" }"),
                new McpCallContext(null, null, null, null)
        ));

        assertEquals(McpDispatchErrorCodes.INTERNAL_ERROR, exception.code());
        assertEquals("Storage publication status is unavailable.", exception.getMessage());
    }

    private McpDispatchRegistry registry(PublicationStatusLookup lookup) {
        ToolStorageService storage = new ToolStorageService() {
            @Override
            public ToolStorageWorkspace openWorkspace(String toolId, McpCallContext context, ToolStorageWorkspaceRequest request) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ToolStorageFile writeFile(ToolStorageWorkspace workspace, String relativePath, InputStream content, ToolStorageFileRequest request) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ToolStorageWorkspace publish(ToolStorageWorkspace workspace) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ToolStoragePublicationStatus publicationStatus(String sessionId, String requestId) {
                return lookup.find(sessionId, requestId);
            }
        };
        return new McpDispatchMethodScanner().scan(List.of(new StoragePublicationMcpController(storage)));
    }

    private McpHandlerMethodInvoker invoker() {
        return new McpHandlerMethodInvoker(
                objectMapper,
                List.of(
                        new McpCallContextArgumentResolver(),
                        new McpDispatchParamArgumentResolver(new TypedJsonArgumentBinder())
                ),
                new McpReturnValueAdapter()
        );
    }

    @FunctionalInterface
    private interface PublicationStatusLookup {
        ToolStoragePublicationStatus find(String sessionId, String requestId);
    }
}

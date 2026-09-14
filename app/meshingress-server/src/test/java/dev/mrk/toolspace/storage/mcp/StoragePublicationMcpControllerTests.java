package dev.mrk.toolspace.storage.mcp;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.storage.ToolStorageException;
import dev.mrk.meshingress.api.storage.ToolStorageFile;
import dev.mrk.meshingress.api.storage.ToolStorageFileRequest;
import dev.mrk.meshingress.api.storage.ToolStoragePublicationStatus;
import dev.mrk.meshingress.api.storage.ToolStorageService;
import dev.mrk.meshingress.api.storage.ToolStorageWorkspace;
import dev.mrk.meshingress.api.storage.ToolStorageWorkspaceRequest;
import dev.mrk.meshingress.api.services.McpServiceResolver;
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
import java.util.Optional;

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

    @Test
    void findsPublicationStatusFromARegisteredProvider() throws Exception {
        ToolStorageService delegatedStorage = storage((sessionId, requestId) -> new ToolStoragePublicationStatus(
                sessionId, requestId, "PUBLISHED", "nextcloud", 2, OffsetDateTime.now(), null
        ));
        McpServiceResolver services = resolver(delegatedStorage);
        McpDispatchRegistry registry = new McpDispatchMethodScanner().scan(List.of(
                new StoragePublicationMcpController(services)
        ));

        JsonNode result = invoker().invoke(
                registry.find("storage/publication-status").orElseThrow(),
                objectMapper.readTree("{ \"sessionId\": \"session-1\", \"requestId\": \"req-test\" }"),
                new McpCallContext(null, null, null, null)
        );

        assertEquals("PUBLISHED", result.path("state").asString());
        assertEquals("nextcloud", result.path("target").asString());
    }

    private McpDispatchRegistry registry(PublicationStatusLookup lookup) {
        return new McpDispatchMethodScanner().scan(List.of(new StoragePublicationMcpController(
                resolver(storage(lookup))
        )));
    }

    private ToolStorageService storage(PublicationStatusLookup lookup) {
        return new ToolStorageService() {
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
    }

    private McpServiceResolver resolver(ToolStorageService provider) {
        return new McpServiceResolver() {
            @Override
            public <T> Optional<T> find(Class<T> contract) {
                return contract.isInstance(provider) ? Optional.of(contract.cast(provider)) : Optional.empty();
            }

            @Override
            public <T> List<T> findAll(Class<T> contract) {
                return contract.isInstance(provider) ? List.of(contract.cast(provider)) : List.of();
            }
        };
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

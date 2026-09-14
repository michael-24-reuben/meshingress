package dev.mrk.meshingress;

import dev.mrk.meshingress.api.services.McpServiceLifecycle;
import dev.mrk.meshingress.api.tools.ToolVisibility;
import dev.mrk.meshingress.api.tools.annotation.McpService;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import dev.mrk.meshingress.mcp.services.McpServiceRegistry;
import dev.mrk.meshingress.mcp.tools.registry.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MeshingressApplicationDocumentationSnapshotTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void snapshotsPublicToolFunctionsAndServicesUsingTheirDiscoveryShapes() {
        ToolRegistry toolRegistry = mock(ToolRegistry.class);
        ObjectNode annotations = objectMapper.createObjectNode();
        annotations.putArray("requires")
                .addObject()
                .put("type", ExampleStorage.class.getName())
                .put("value", "example.storage")
                .put("title", "Example storage")
                .put("description", "Stores example output.");
        McpFunctionDescriptor function = new McpFunctionDescriptor(
                "example.catalog.search",
                "Search catalog",
                "Searches the example catalog.",
                1,
                true,
                ToolVisibility.PUBLIC,
                "example.catalog.search",
                objectMapper.createObjectNode(),
                null,
                annotations,
                false
        );
        when(toolRegistry.listPublicEnabledFunctions()).thenReturn(List.of(function));
        when(toolRegistry.findOwningModuleToolId(function.name())).thenReturn(Optional.of("cp-example"));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(ExampleStorageService.class);
            context.refresh();
            McpServiceRegistry serviceRegistry = new McpServiceRegistry(context, objectMapper);

            ObjectNode snapshot = MeshingressApplication.toolsAndServicesSnapshot(
                    objectMapper,
                    toolRegistry,
                    serviceRegistry
            );

            assertEquals("example.catalog.search", snapshot.path("tools").path(0).path("name").asString());
            assertEquals("cp-example", snapshot.path("tools").path(0).path("moduleToolId").asString());
            assertEquals("example.storage", snapshot.path("services").path(0).path("name").asString());
            assertTrue(snapshot.path("services").path(0).path("provides").isArray());
            assertTrue(snapshot.path("tools").path(0).path("requirements").isObject());
            assertEquals("string", snapshot.path("tools").path(0).path("requirements").path("example.storage").path("inputSchema").path("properties").path("workspace").path("type").asString());
        }
    }

    interface ExampleStorage extends McpServiceLifecycle {
    }

    @McpService(value = "example.storage", title = "Example storage", provides = ExampleStorage.class)
    static class ExampleStorageService implements ExampleStorage {
        void init(ExampleStorageInput input) {
        }
    }

    record ExampleStorageInput(String workspace) {
    }
}

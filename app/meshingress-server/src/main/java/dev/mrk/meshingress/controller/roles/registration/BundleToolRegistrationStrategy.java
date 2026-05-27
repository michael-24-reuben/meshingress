package dev.mrk.meshingress.controller.roles.registration;

import dev.mrk.meshingress.api.tools.McpToolDescriptor;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import dev.mrk.meshingress.mcp.tools.ToolRegistry;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
class BundleToolRegistrationStrategy implements ToolRegistrationStrategy {

    private final ToolRegistry toolRegistry;
    private final ToolRegistrationStore store;

    BundleToolRegistrationStrategy(ToolRegistry toolRegistry, ToolRegistrationStore store) {
        this.toolRegistry = toolRegistry;
        this.store = store;
    }

    @Override
    public ToolRegistrationPhase phase() {
        return ToolRegistrationPhase.BUNDLE;
    }

    @Override
    public ToolRegistrationResult register(ToolRegistrationRequest request, ToolRegistrationContext context) {
        McpToolDescriptor tool = toolRegistry.findTool(request.toolId()).orElse(null);
        McpFunctionDescriptor function = toolRegistry.findEnabledFunction(request.toolId()).orElse(null);
        if (tool == null && function == null) {
            throw ToolRegistrationErrors.invalidParams(
                    "Requested bundled tool is not present on the server classpath.",
                    "BUNDLE_TOOL_NOT_PRESENT"
            );
        }

        Map<String, String> source = new LinkedHashMap<>();
        source.put("bundleId", request.bundle().bundleId());
        source.put("lookup", tool == null ? "function" : "tool");
        List<String> functions = tool == null
                ? List.of(function.name())
                : tool.functions().stream().map(McpFunctionDescriptor::name).toList();
        ToolRegistrationRecord record = new ToolRegistrationRecord(
                ToolRegistrationIds.registrationId(request),
                request.toolId(),
                phase(),
                ToolSourceKind.CLASSPATH_BUNDLE,
                "reconciled",
                source,
                context.actor(),
                context.call().requestId(),
                context.requestedAt(),
                null,
                null,
                functions
        );
        store.saveActive(record);
        return new ToolRegistrationResult(record, toolRegistry.registryVersion());
    }
}

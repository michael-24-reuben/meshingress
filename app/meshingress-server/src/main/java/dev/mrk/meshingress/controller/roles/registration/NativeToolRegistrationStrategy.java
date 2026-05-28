package dev.mrk.meshingress.controller.roles.registration;

import dev.mrk.meshingress.api.tools.McpToolDescriptor;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import dev.mrk.meshingress.mcp.tools.registry.ToolRegistry;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
class NativeToolRegistrationStrategy implements ToolRegistrationStrategy {

    private final ToolRegistry toolRegistry;
    private final ToolRegistrationStore store;

    NativeToolRegistrationStrategy(ToolRegistry toolRegistry, ToolRegistrationStore store) {
        this.toolRegistry = toolRegistry;
        this.store = store;
    }

    @Override
    public ToolRegistrationPhase phase() {
        return ToolRegistrationPhase.NATIVE;
    }

    @Override
    public ToolRegistrationResult register(ToolRegistrationRequest request, ToolRegistrationContext context) {
        if (!isReservedNative(request.toolId()) && !isReservedNative(request.nativeTool().namespace())) {
            throw ToolRegistrationErrors.forbidden(
                    "Native tool registration is restricted to server-native namespaces.",
                    "TOOL_REGISTRATION_NATIVE_NAMESPACE_DENIED"
            );
        }

        McpToolDescriptor tool = toolRegistry.findTool(request.toolId()).orElse(null);
        McpFunctionDescriptor function = toolRegistry.findEnabledFunction(request.toolId()).orElse(null);
        if (tool == null && function == null) {
            throw ToolRegistrationErrors.invalidParams(
                    "Requested native tool is not present in the server registry.",
                    "NATIVE_TOOL_NOT_PRESENT"
            );
        }

        Map<String, String> source = new LinkedHashMap<>();
        source.put("namespace", request.nativeTool().namespace());
        source.put("lookup", tool == null ? "function" : "tool");
        List<String> functions = tool == null
                ? List.of(function.name())
                : tool.functions().stream().map(McpFunctionDescriptor::name).toList();
        ToolRegistrationRecord record = new ToolRegistrationRecord(
                ToolRegistrationIds.registrationId(request),
                request.toolId(),
                phase(),
                ToolSourceKind.SERVER_NATIVE,
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

    private boolean isReservedNative(String value) {
        return value != null
                && (value.startsWith("meshingress.") || value.startsWith("system.") || value.startsWith("runtime.")
                || value.equals("meshingress") || value.equals("system") || value.equals("runtime"));
    }
}

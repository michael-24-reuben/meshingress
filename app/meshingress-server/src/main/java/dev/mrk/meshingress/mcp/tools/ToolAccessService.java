package dev.mrk.meshingress.mcp.tools;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.tools.ToolVisibility;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.scopes.McpToolScope;
import dev.mrk.meshingress.security.management.ToolPolicyService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

/** Single source of truth for whether a function may be seen or executed. */
@Service
public class ToolAccessService {
    private final MeshingressProperties properties;
    private final ToolPolicyService toolPolicyService;

    public ToolAccessService(MeshingressProperties properties, ToolPolicyService toolPolicyService) {
        this.properties = properties;
        this.toolPolicyService = toolPolicyService;
    }

    public ToolAccessDecision evaluate(McpFunctionDescriptor function, McpCallContext context) {
        if (function == null || !function.enabled()) return ToolAccessDecision.hidden("Tool function is not available.");
        if (function.visibility() != ToolVisibility.PUBLIC) return ToolAccessDecision.hidden("Tool function is not available.");
        for (String scopeName : scopeNames(function)) {
            McpToolScope scope;
            try { scope = McpToolScope.valueOf(scopeName); }
            catch (IllegalArgumentException ignored) {
                if (properties.security().enabled() && properties.security().denyUnknownScopes()) return ToolAccessDecision.denied("Tool function requires an unknown scope.");
                continue;
            }
            if (scope == McpToolScope.SHELL_EXECUTE && !properties.scopes().allowShellExecute()) return ToolAccessDecision.denied("Tool function requires a disabled scope.");
            if (scope == McpToolScope.FILES_DELETE && !properties.scopes().allowFilesDelete()) return ToolAccessDecision.denied("Tool function requires a disabled scope.");
            if (scope == McpToolScope.NETWORK_INBOUND && !properties.scopes().allowNetworkInbound()) return ToolAccessDecision.denied("Tool function requires a disabled scope.");
        }
        ToolPolicyService.ToolPolicyDecision policy = toolPolicyService.evaluate(function.name(), context == null ? null : context.principal());
        if (!policy.allowed()) return ToolAccessDecision.denied(policy.reason());
        // Annotation scan-time availability is represented by enabled(). Argument-bound
        // conditions are deliberately not inferred from descriptor JSON; a future runtime
        // policy adapter must explicitly implement them and fail closed.
        return ToolAccessDecision.allow();
    }

    private java.util.List<String> scopeNames(McpFunctionDescriptor function) {
        JsonNode scopes = function.annotations() == null ? null : function.annotations().path("scopes");
        if (scopes == null || !scopes.isArray()) return java.util.List.of();
        java.util.List<String> names = new java.util.ArrayList<>();
        for (JsonNode scope : scopes) if (!scope.asString("").isBlank()) names.add(scope.asString());
        return names;
    }
}

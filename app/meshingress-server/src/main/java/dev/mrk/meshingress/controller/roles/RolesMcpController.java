package dev.mrk.meshingress.controller.roles;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.controller.McpMethodController;
import dev.mrk.meshingress.mcp.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.JsonRpcException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.Set;

@Component
public class RolesMcpController implements McpMethodController {

    private static final Set<String> METHODS = Set.of(
            "roles/tools/check",
            "roles/tools/register",
            "roles/tools/update",
            "roles/tools/delete",
            "roles/tools/list",
            "roles/tools/reload"
    );

    private final RoleToolService roleToolService;

    public RolesMcpController(RoleToolService roleToolService) {
        this.roleToolService = roleToolService;
    }

    @Override
    public Set<String> supportedMethods() {
        return METHODS;
    }

    @Override
    public boolean supports(String method) {
        return METHODS.contains(method);
    }

    @Override
    public JsonNode dispatch(String method, JsonNode params, McpCallContext context) {
        return switch (method) {
            case "roles/tools/check" -> roleToolService.check(context, params);
            case "roles/tools/register" -> roleToolService.register(context, params);
            case "roles/tools/update" -> roleToolService.update(context, params);
            case "roles/tools/delete" -> roleToolService.delete(context, params);
            case "roles/tools/list" -> roleToolService.list(context, params);
            case "roles/tools/reload" -> roleToolService.reload(context);
            default -> throw new JsonRpcException(JsonRpcErrorCodes.METHOD_NOT_FOUND, "Method not found");
        };
    }
}

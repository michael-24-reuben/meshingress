package dev.mrk.meshingress.controller.roles;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.route.annotations.McpDispatchMapping;
import dev.mrk.meshingress.route.annotations.McpDispatchMethod;
import dev.mrk.meshingress.route.annotations.McpDispatchParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import dev.mrk.meshingress.controller.roles.params.RolesToolAliasParams;
import dev.mrk.meshingress.controller.roles.params.RolesToolCheckParams;
import dev.mrk.meshingress.controller.roles.params.RolesToolDeleteParams;
import dev.mrk.meshingress.controller.roles.params.RolesToolInstallPublicationParams;
import dev.mrk.meshingress.controller.roles.params.RolesToolListParams;
import dev.mrk.meshingress.controller.roles.params.RolesToolContributionListParams;
import dev.mrk.meshingress.controller.roles.params.RolesToolContributionUpdateParams;
import dev.mrk.meshingress.controller.roles.params.RolesToolUpdateParams;
import dev.mrk.meshingress.controller.roles.registration.ToolRegistrationParams;

/**
 * Admin-only MCP dispatch controller for registry maintenance, tool publication installs,
 * and runtime visibility of role-scoped tools.
 */
@Component
@McpDispatchMapping("roles/tools")
public class RolesMcpController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RolesMcpController.class);

    private final RoleToolService roleToolService;
    private final ObjectMapper objectMapper;

    public RolesMcpController(RoleToolService roleToolService, ObjectMapper objectMapper) {
        this.roleToolService = roleToolService;
        this.objectMapper = objectMapper;
    }

    /**
     * Validates a proposed tool descriptor against the registry rules.
     * <p>
     * The incoming {@code tool} payload is converted into an internal descriptor, the
     * request is forced into dynamic mode, and {@code mode=update} switches the
     * underlying registry check into update semantics.
     * </p>
     *
     * @param params  the proposed tool descriptor and optional check mode
     * @param context the current MCP call context
     * @return the check result as JSON, including validity, errors, warnings, and
     * normalized descriptor data
     */
    @McpDispatchMethod("check")
    public JsonNode check(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        LOGGER.info("MCP roles/tools/check: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return roleToolService.check(context, toParams(params, RolesToolCheckParams.class));
    }

    /**
     * Registers a tool using the phase-aware registration workflow.
     * <p>
     * This method only accepts registration payloads that declare an explicit phase or
     * other phase-registration fields; otherwise it rejects the request as invalid.
     * </p>
     *
     * @param params  phase-aware tool registration parameters
     * @param context the current MCP call context
     * @return the registration outcome returned by the registration service
     */
    @McpDispatchMethod("register")
    public JsonNode register(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        LOGGER.info("MCP roles/tools/register: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return roleToolService.register(context, toParams(params, ToolRegistrationParams.class));
    }

    /**
     * Installs a signed artifact publication and registers the resulting tool.
     * <p>
     * The publication payload is required; the optional {@code toolId} overrides the
     * tool identifier chosen during installation.
     * </p>
     *
     * @param params  the publication to install and optional tool identifier
     * @param context the current MCP call context
     * @return the installation result serialized as JSON
     */
    @McpDispatchMethod("installPublication")
    public JsonNode installPublication(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        LOGGER.info("MCP roles/tools/installPublication: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return roleToolService.installPublication(context, toParams(params, RolesToolInstallPublicationParams.class));
    }

    /**
     * Registers an alias for the supplied tool descriptor.
     * <p>
     * The descriptor is normalized, registered in dynamic mode, and the response
     * reports the alias operation together with the resulting tool and registry versions.
     * </p>
     *
     * @param params  the alias target descriptor
     * @param context the current MCP call context
     * @return a JSON object describing the aliased tool and registry version
     */
    @McpDispatchMethod("alias")
    public JsonNode alias(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        LOGGER.info("MCP roles/tools/alias: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return roleToolService.alias(context, toParams(params, RolesToolAliasParams.class));
    }

    /**
     * Applies a patch to an existing registry tool entry.
     * <p>
     * The tool name is required, the patch is converted into a registry patch object,
     * and the response includes the prior version, the updated version, and the new
     * registry version.
     * </p>
     *
     * @param params  the tool name and patch payload
     * @param context the current MCP call context
     * @return a JSON object describing the update outcome
     */
    @McpDispatchMethod("update")
    public JsonNode update(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        LOGGER.info("MCP roles/tools/update: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return roleToolService.update(context, toParams(params, RolesToolUpdateParams.class));
    }

    /**
     * Removes or disables a tool entry.
     * <p>
     * Only {@code mode=disable} is supported in the MVP. If the tool has an active
     * phase registration, deletion is delegated to the registration service; otherwise
     * the registry entry is disabled in-place and the response reflects that no hard
     * delete occurred.
     * </p>
     *
     * @param params  the tool name and delete mode
     * @param context the current MCP call context
     * @return a JSON object describing the delete/disable result
     */
    @McpDispatchMethod("delete")
    public JsonNode delete(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        LOGGER.info("MCP roles/tools/delete: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return roleToolService.delete(context, toParams(params, RolesToolDeleteParams.class));
    }

    /**
     * Lists role-visible tools together with audit history and active registrations.
     * <p>
     * The optional flags control whether disabled tools and private tools are included
     * in the registry listing.
     * </p>
     *
     * @param params  optional visibility filters for the listing
     * @param context the current MCP call context
     * @return a JSON object containing tools, audit events, registrations, and the
     * current registry version
     */
    @McpDispatchMethod("list")
    public JsonNode list(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        LOGGER.info("MCP roles/tools/list: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return roleToolService.list(context, toParams(params, RolesToolListParams.class));
    }

    @McpDispatchMethod("contributions/list")
    public JsonNode listContributions(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        LOGGER.info("MCP roles/tools/contributions/list: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return roleToolService.listContributions(context, toParams(params, RolesToolContributionListParams.class));
    }

    @McpDispatchMethod("contributions/update")
    public JsonNode updateContribution(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        LOGGER.info("MCP roles/tools/contributions/update: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return roleToolService.updateContribution(context, toParams(params, RolesToolContributionUpdateParams.class));
    }

    /**
     * Returns the current runtime reload status for tool registrations.
     * <p>
     * The current implementation does not perform a reload; it only reports whether
     * reload is supported and includes the current registration/runtime module state.
     * </p>
     *
     * @param context the current MCP call context
     * @return a JSON object describing reload support and runtime module status
     */
    @McpDispatchMethod("reload")
    public JsonNode reload(McpCallContext context) {
        LOGGER.info("MCP roles/tools/reload: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return roleToolService.reload(context);
    }

    private <T> T toParams(JsonNode params, Class<T> type) {
        if (params == null || params.isMissingNode() || params.isNull()) {
            return objectMapper.convertValue(objectMapper.createObjectNode(), type);
        }
        return objectMapper.convertValue(params, type);
    }
}

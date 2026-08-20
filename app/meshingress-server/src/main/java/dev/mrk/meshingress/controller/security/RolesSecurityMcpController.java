package dev.mrk.meshingress.controller.security;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.route.annotations.McpDispatchMapping;
import dev.mrk.meshingress.route.annotations.McpDispatchMethod;
import dev.mrk.meshingress.route.annotations.McpDispatchParam;
import dev.mrk.meshingress.security.management.SecurityManagementService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Privileged, tenant-scoped Aegis profile, binding, and tool-policy management routes.
 */
@Component
@McpDispatchMapping("roles/security")
public final class RolesSecurityMcpController {
    private final SecurityManagementService security;

    public RolesSecurityMcpController(SecurityManagementService security) {
        this.security = security;
    }

    @McpDispatchMethod("profiles/list")
    public ObjectNode profilesList(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        return security.profilesList(context, params);
    }

    @McpDispatchMethod("profiles/get")
    public ObjectNode profilesGet(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        return security.profilesGet(context, params);
    }

    @McpDispatchMethod("profiles/create")
    public ObjectNode profilesCreate(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        return security.profilesCreate(context, params);
    }

    @McpDispatchMethod("profiles/update")
    public ObjectNode profilesUpdate(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        return security.profilesUpdate(context, params);
    }

    @McpDispatchMethod("profiles/activate")
    public ObjectNode profilesActivate(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        return security.profilesActivate(context, params);
    }

    @McpDispatchMethod("profiles/disable")
    public ObjectNode profilesDisable(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        return security.profilesDisable(context, params);
    }

    @McpDispatchMethod("profiles/revoke")
    public ObjectNode profilesRevoke(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        return security.profilesRevoke(context, params);
    }

    @McpDispatchMethod("profiles/identities/link")
    public ObjectNode identitiesLink(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        return security.identitiesLink(context, params);
    }

    @McpDispatchMethod("profiles/identities/unlink")
    public ObjectNode identitiesUnlink(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        return security.identitiesUnlink(context, params);
    }

    @McpDispatchMethod("profiles/credentials/bind")
    public ObjectNode credentialsBind(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        return security.credentialsBind(context, params);
    }

    @McpDispatchMethod("profiles/credentials/replace")
    public ObjectNode credentialsReplace(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        return security.credentialsReplace(context, params);
    }

    @McpDispatchMethod("profiles/credentials/revoke")
    public ObjectNode credentialsRevoke(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        return security.credentialsRevoke(context, params);
    }

    @McpDispatchMethod("profiles/credentials/unbind")
    public ObjectNode credentialsUnbind(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        return security.credentialsUnbind(context, params);
    }

    @McpDispatchMethod("credential-bindings/list")
    public ObjectNode bindingsList(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        return security.credentialBindingsList(context, params);
    }

    @McpDispatchMethod("credential-bindings/get")
    public ObjectNode bindingsGet(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        return security.credentialBindingsGet(context, params);
    }

    @McpDispatchMethod("profiles/limits/get")
    public ObjectNode profileLimitsGet(@McpDispatchParam("params") JsonNode params, McpCallContext context) { return security.profileLimitsGet(context, params); }

    @McpDispatchMethod("profiles/limits/update")
    public ObjectNode profileLimitsUpdate(@McpDispatchParam("params") JsonNode params, McpCallContext context) { return security.profileLimitsUpdate(context, params); }

    @McpDispatchMethod("profiles/usage/list")
    public ObjectNode profileUsageList(@McpDispatchParam("params") JsonNode params, McpCallContext context) { return security.profileUsageList(context, params); }

    @McpDispatchMethod("tool-policies/list")
    public ObjectNode policiesList(McpCallContext context) {
        return security.policiesList(context);
    }

    @McpDispatchMethod("tool-policies/get")
    public ObjectNode policiesGet(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        return security.policiesGet(context, params);
    }

    @McpDispatchMethod("tool-policies/create")
    public ObjectNode policiesCreate(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        return security.policiesCreate(context, params);
    }

    @McpDispatchMethod("tool-policies/update")
    public ObjectNode policiesUpdate(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        return security.policiesUpdate(context, params);
    }

    @McpDispatchMethod("tool-policies/delete")
    public ObjectNode policiesDelete(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        return security.policiesDelete(context, params);
    }

    @McpDispatchMethod("tool-policies/evaluate")
    public ObjectNode policiesEvaluate(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        return security.policiesEvaluate(context, params);
    }
}

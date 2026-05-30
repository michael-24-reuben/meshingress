package dev.mrk.meshingress.server.install;

import dev.mrk.meshingress.artifact.model.ArtifactPublicationRecord;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import dev.mrk.meshingress.scopes.McpToolScope;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class InstallPolicyEvaluator {

    private final MeshingressProperties properties;

    public InstallPolicyEvaluator(MeshingressProperties properties) {
        this.properties = properties;
    }

    public void requireInstallable(ArtifactPublicationRecord publication) {
        if (!publication.trustStatus().installable()) {
            throw forbidden("Publication record is not in an installable trust state.");
        }
        if (publication.revoked()) {
            throw forbidden("Publication record is revoked.");
        }
        for (String scopeName : publication.scopePolicy().approvedScopes()) {
            McpToolScope scope;
            try {
                scope = McpToolScope.valueOf(scopeName);
            } catch (IllegalArgumentException exception) {
                if (properties.security().denyUnknownScopes()) {
                    throw forbidden("Publication record approves an unknown scope: " + scopeName);
                }
                continue;
            }
            if (scope == McpToolScope.SHELL_EXECUTE && !properties.scopes().allowShellExecute()) {
                throw forbidden("Publication record approves disabled scope: SHELL_EXECUTE");
            }
            if (scope == McpToolScope.FILES_DELETE && !properties.scopes().allowFilesDelete()) {
                throw forbidden("Publication record approves disabled scope: FILES_DELETE");
            }
            if (scope == McpToolScope.NETWORK_INBOUND && !properties.scopes().allowNetworkInbound()) {
                throw forbidden("Publication record approves disabled scope: NETWORK_INBOUND");
            }
        }
    }

    public void requireApprovedScopes(ArtifactPublicationRecord publication, List<McpFunctionDescriptor> functions) {
        if (functions == null || functions.isEmpty()) {
            return;
        }
        Set<String> approved = new HashSet<>(publication.scopePolicy().approvedScopes());
        for (McpFunctionDescriptor function : functions) {
            for (String scopeName : scopeNames(function)) {
                if (!approved.contains(scopeName)) {
                    throw forbidden("Tool function " + function.name() + " scope not approved in publication: " + scopeName);
                }
            }
        }
    }

    private List<String> scopeNames(McpFunctionDescriptor function) {
        JsonNode scopes = function.annotations() == null ? null : function.annotations().path("scopes");
        if (scopes == null || !scopes.isArray()) {
            return List.of();
        }
        List<String> names = new java.util.ArrayList<>();
        for (JsonNode scope : (ArrayNode) scopes) {
            String name = scope.asString("").trim();
            if (!name.isBlank()) {
                names.add(name);
            }
        }
        return names;
    }

    private JsonRpcException forbidden(String message) {
        return new JsonRpcException(JsonRpcErrorCodes.FORBIDDEN, message);
    }
}

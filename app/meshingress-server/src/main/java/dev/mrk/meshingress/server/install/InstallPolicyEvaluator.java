package dev.mrk.meshingress.server.install;

import dev.mrk.meshingress.artifact.model.ArtifactPublicationRecord;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import dev.mrk.meshingress.scopes.McpToolScope;
import org.springframework.stereotype.Service;

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

    private JsonRpcException forbidden(String message) {
        return new JsonRpcException(JsonRpcErrorCodes.FORBIDDEN, message);
    }
}

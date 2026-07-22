package dev.mrk.meshingress.server.install;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.tools.McpToolDescriptor;
import dev.mrk.meshingress.api.tools.McpToolHandler;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import dev.mrk.meshingress.artifact.model.ArtifactPublicationRecord;
import dev.mrk.meshingress.artifact.storage.ProjectRootResolver;
import dev.mrk.meshingress.controller.roles.registration.ToolRegistrationRecord;
import dev.mrk.meshingress.controller.roles.registration.ToolRegistrationStore;
import dev.mrk.meshingress.controller.roles.registration.ToolRegistrationPhase;
import dev.mrk.meshingress.controller.roles.registration.ToolSourceKind;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import dev.mrk.meshingress.mcp.tools.registry.ToolRegistry;
import dev.mrk.meshingress.runtime.artifacts.LocalJarSource;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleHandle;
import dev.mrk.meshingress.runtime.loader.ToolRuntimeLoader;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ArtifactInstaller {

    private final Path projectRoot;

    private final PublicationRecordVerifier verifier;
    private final InstallPolicyEvaluator policyEvaluator;
    private final RuntimeToolCache runtimeToolCache;
    private final ToolRuntimeLoader runtimeLoader;
    private final ToolRegistrationStore registrationStore;
    private final ToolRegistry toolRegistry;

    public ArtifactInstaller(
            PublicationRecordVerifier verifier,
            InstallPolicyEvaluator policyEvaluator,
            RuntimeToolCache runtimeToolCache,
            ToolRuntimeLoader runtimeLoader,
            ToolRegistrationStore registrationStore,
            ToolRegistry toolRegistry
    ) {
        this.verifier = verifier;
        this.policyEvaluator = policyEvaluator;
        this.runtimeToolCache = runtimeToolCache;
        this.runtimeLoader = runtimeLoader;
        this.registrationStore = registrationStore;
        this.toolRegistry = toolRegistry;
        this.projectRoot = ProjectRootResolver.resolve(ArtifactInstaller.class, null);
    }

    public ToolPublicationInstallResult install(ArtifactPublicationRecord publication, String requestedToolId, McpCallContext context) {
        verifier.verifySignature(publication);
        policyEvaluator.requireInstallable(publication);
        Path cachedJar = runtimeToolCache.install(publication);
        ToolModuleHandle handle = null;
        ToolRegistrationRecord savedRecord = null;
        try {
            handle = runtimeLoader.activate(new LocalJarSource(cachedJar));
            List<McpFunctionDescriptor> installedFunctions = resolveInstalledFunctions(handle.registeredFunctions());
            policyEvaluator.requireApprovedScopes(publication, installedFunctions);
            String toolId = requestedToolId == null || requestedToolId.isBlank()
                    ? handle.registeredFunctions().stream().findFirst().orElse(publication.coordinate().artifactId())
                    : requestedToolId.trim();
            Map<String, String> source = new LinkedHashMap<>();
            source.put("coordinate", publication.coordinate().display());
            source.put("artifactUri", publication.artifactUri());
            source.put("artifactSha256", publication.artifactChecksum().value());
            source.put("trustStatus", publication.trustStatus().name());
            source.put("runtimeCachePath", ProjectRootResolver.relativize(projectRoot, cachedJar));
            source.put("signatureKeyId", publication.signatureKeyId());
            source.put("signatureAlgorithm", publication.signatureAlgorithm());
            source.put("approvedScopes", String.join(",", publication.scopePolicy().approvedScopes()));

            ToolRegistrationRecord record = new ToolRegistrationRecord(
                    "publication:" + publication.coordinate().artifactId() + ":" + Long.toUnsignedString(System.nanoTime(), 36),
                    toolId,
                    ToolRegistrationPhase.STAGING,
                    ToolSourceKind.PUBLICATION_RECORD,
                    "active",
                    source,
                    context == null ? "unknown" : actor(context),
                    context == null ? null : context.requestId(),
                    OffsetDateTime.now(),
                    null,
                    handle.moduleId().value(),
                    handle.registeredFunctions()
            );
            savedRecord = registrationStore.saveActive(record);
            return new ToolPublicationInstallResult(savedRecord, toolRegistry.registryVersion());
        } catch (RuntimeException exception) {
            if (savedRecord != null) {
                markRegistrationRolledBackAfterInstallFailure(savedRecord, exception);
            }
            if (handle != null) {
                deactivateAfterInstallFailure(handle, exception);
            }
            removeCachedJarAfterInstallFailure(cachedJar, exception);
            throw exception;
        }
    }

    private void markRegistrationRolledBackAfterInstallFailure(ToolRegistrationRecord record, RuntimeException exception) {
        try {
            registrationStore.markStatus(record.registrationId(), "install-rolled-back");
        } catch (RuntimeException suppressed) {
            exception.addSuppressed(suppressed);
        }
    }

    private void deactivateAfterInstallFailure(ToolModuleHandle handle, RuntimeException exception) {
        try {
            runtimeLoader.deactivate(handle.moduleId());
        } catch (RuntimeException suppressed) {
            exception.addSuppressed(suppressed);
        }
    }

    private void removeCachedJarAfterInstallFailure(Path cachedJar, RuntimeException exception) {
        try {
            runtimeToolCache.remove(cachedJar);
        } catch (RuntimeException suppressed) {
            exception.addSuppressed(suppressed);
        }
    }

    private List<McpFunctionDescriptor> resolveInstalledFunctions(List<String> functionNames) {
        Map<String, McpFunctionDescriptor> lookup = new LinkedHashMap<>();
        for (McpToolDescriptor descriptor : toolRegistry.listRoleVisibleTools(true, true)) {
            for (McpFunctionDescriptor function : descriptor.functions()) {
                lookup.putIfAbsent(function.name(), function);
            }
        }
        List<McpFunctionDescriptor> resolved = new ArrayList<>();
        for (String functionName : functionNames) {
            McpFunctionDescriptor function = lookup.get(functionName);
            if (function == null) {
                throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS,
                        "Installed tool function is not registered: " + functionName);
            }
            McpFunctionDescriptor resolvedFunction = resolveHandlerFunction(functionName, function);
            resolved.add(resolvedFunction);
        }
        return resolved;
    }

    private McpFunctionDescriptor resolveHandlerFunction(String functionName, McpFunctionDescriptor fallback) {
        String handlerKey = fallback.handlerKey();
        if (handlerKey == null || handlerKey.isBlank()) {
            return fallback;
        }
        return toolRegistry.findHandler(handlerKey)
                .map(McpToolHandler::descriptor)
                .flatMap(descriptor -> descriptor.functions().stream()
                        .filter(function -> functionName.equals(function.name()))
                        .findFirst())
                .orElse(fallback);
    }

    private String actor(McpCallContext context) {
        if (context.authorizationHeader() != null && context.authorizationHeader().startsWith("Bearer ")) {
            return "bearer-role-admin";
        }
        if (context.roleHeader() != null && !context.roleHeader().isBlank()) {
            return "role-" + context.roleHeader();
        }
        return "anonymous";
    }
}

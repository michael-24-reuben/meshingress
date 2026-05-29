package dev.mrk.meshingress.server.install;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.artifact.model.ArtifactPublicationRecord;
import dev.mrk.meshingress.controller.roles.registration.ToolRegistrationRecord;
import dev.mrk.meshingress.controller.roles.registration.ToolRegistrationStore;
import dev.mrk.meshingress.controller.roles.registration.ToolRegistrationPhase;
import dev.mrk.meshingress.controller.roles.registration.ToolSourceKind;
import dev.mrk.meshingress.mcp.tools.registry.ToolRegistry;
import dev.mrk.meshingress.runtime.artifacts.LocalJarSource;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleHandle;
import dev.mrk.meshingress.runtime.loader.ToolRuntimeLoader;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ArtifactInstaller {

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
    }

    public ToolPublicationInstallResult install(ArtifactPublicationRecord publication, String requestedToolId, McpCallContext context) {
        verifier.verifySignature(publication);
        policyEvaluator.requireInstallable(publication);
        Path cachedJar = runtimeToolCache.install(publication);
        ToolModuleHandle handle = runtimeLoader.activate(new LocalJarSource(cachedJar));

        String toolId = requestedToolId == null || requestedToolId.isBlank()
                ? handle.registeredFunctions().stream().findFirst().orElse(publication.coordinate().artifactId())
                : requestedToolId.trim();
        Map<String, String> source = new LinkedHashMap<>();
        source.put("coordinate", publication.coordinate().display());
        source.put("artifactUri", publication.artifactUri());
        source.put("artifactSha256", publication.artifactChecksum().value());
        source.put("trustStatus", publication.trustStatus().name());
        source.put("runtimeCachePath", cachedJar.toString());
        source.put("signatureAlgorithm", publication.signatureAlgorithm());

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
        registrationStore.saveActive(record);
        return new ToolPublicationInstallResult(record, toolRegistry.registryVersion());
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

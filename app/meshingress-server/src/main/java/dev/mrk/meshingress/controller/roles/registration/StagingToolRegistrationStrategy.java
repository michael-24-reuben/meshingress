package dev.mrk.meshingress.controller.roles.registration;

import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.tools.ToolRegistry;
import dev.mrk.meshingress.runtime.artifacts.MavenCoordinatesSource;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleHandle;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleId;
import dev.mrk.meshingress.runtime.loader.ToolRuntimeLoader;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
class StagingToolRegistrationStrategy implements ToolRegistrationStrategy {

    private final MeshingressProperties properties;
    private final ToolRuntimeLoader runtimeLoader;
    private final ToolRegistry toolRegistry;
    private final ToolRegistrationStore store;

    StagingToolRegistrationStrategy(
            MeshingressProperties properties,
            ToolRuntimeLoader runtimeLoader,
            ToolRegistry toolRegistry,
            ToolRegistrationStore store
    ) {
        this.properties = properties;
        this.runtimeLoader = runtimeLoader;
        this.toolRegistry = toolRegistry;
        this.store = store;
    }

    @Override
    public ToolRegistrationPhase phase() {
        return ToolRegistrationPhase.STAGING;
    }

    @Override
    public ToolRegistrationResult register(ToolRegistrationRequest request, ToolRegistrationContext context) {
        MavenCoordinatesSpec maven = request.maven();
        if (maven == null) {
            throw ToolRegistrationErrors.invalidParams("maven is required.", "TOOL_REGISTRATION_MAVEN_REQUIRED");
        }
        if (maven.groupId().isBlank() || maven.artifactId().isBlank() || maven.version().isBlank()) {
            throw ToolRegistrationErrors.invalidParams(
                    "maven.groupId, maven.artifactId, and maven.version are required.",
                    "TOOL_REGISTRATION_MAVEN_COORDINATES_REQUIRED"
            );
        }
        MeshingressProperties.Tools.Registration config = properties.tools().registration();
        if (config.requireMavenVersionPin() && (maven.version().equalsIgnoreCase("LATEST") || maven.version().equalsIgnoreCase("RELEASE"))) {
            throw ToolRegistrationErrors.invalidParams(
                    "maven.version must be pinned.",
                    "TOOL_REGISTRATION_MAVEN_VERSION_PIN_REQUIRED"
            );
        }
        if (!config.allowOverrideNative() && isReservedNative(request.toolId())) {
            throw ToolRegistrationErrors.forbidden(
                    "Staging registration cannot override server-native namespaces.",
                    "TOOL_REGISTRATION_NATIVE_OVERRIDE_DENIED"
            );
        }
        if (!config.allowStagingOverrideBundle()
                && (toolRegistry.findTool(request.toolId()).isPresent() || toolRegistry.findEnabledFunction(request.toolId()).isPresent())
                && store.findActive(request.toolId(), ToolRegistrationPhase.STAGING).isEmpty()) {
            throw ToolRegistrationErrors.forbidden(
                    "Staging registration cannot override a bundled/classpath tool by default.",
                    "TOOL_REGISTRATION_BUNDLE_OVERRIDE_DENIED"
            );
        }

        Optional<ToolRegistrationRecord> previous = store.findActive(request.toolId(), ToolRegistrationPhase.STAGING);
        if (previous.isPresent() && !request.replace()) {
            throw ToolRegistrationErrors.invalidParams(
                    "A staging registration is already active for this tool.",
                    "TOOL_REGISTRATION_CONFLICT"
            );
        }
        previous.ifPresent(record -> {
            if (record.runtimeModuleId() != null) {
                runtimeLoader.deactivate(new ToolModuleId(record.runtimeModuleId()));
            }
            store.markReplaced(record.registrationId());
        });

        ToolModuleHandle handle = runtimeLoader.activate(new MavenCoordinatesSource(
                maven.groupId(),
                maven.artifactId(),
                maven.version(),
                maven.repositories()
        ));

        Map<String, String> source = new LinkedHashMap<>();
        source.put("groupId", maven.groupId());
        source.put("artifactId", maven.artifactId());
        source.put("version", maven.version());
        ToolRegistrationRecord record = new ToolRegistrationRecord(
                ToolRegistrationIds.registrationId(request),
                request.toolId(),
                phase(),
                ToolSourceKind.MAVEN_COORDINATES,
                "active",
                source,
                context.actor(),
                context.call().requestId(),
                context.requestedAt(),
                previous.map(ToolRegistrationRecord::registrationId).orElse(null),
                handle.moduleId().value(),
                handle.registeredFunctions()
        );
        store.saveActive(record);
        return new ToolRegistrationResult(record, toolRegistry.registryVersion());
    }

    private boolean isReservedNative(String toolId) {
        return toolId.startsWith("meshingress.") || toolId.startsWith("system.") || toolId.startsWith("runtime.");
    }
}

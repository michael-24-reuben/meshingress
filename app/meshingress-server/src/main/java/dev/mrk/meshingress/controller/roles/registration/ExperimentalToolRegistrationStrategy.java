package dev.mrk.meshingress.controller.roles.registration;

import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.tools.registry.ToolRegistry;
import dev.mrk.meshingress.runtime.artifacts.LocalJarSource;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleHandle;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleId;
import dev.mrk.meshingress.runtime.loader.ToolRuntimeLoader;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
class ExperimentalToolRegistrationStrategy implements ToolRegistrationStrategy {

    private final MeshingressProperties properties;
    private final ToolRuntimeLoader runtimeLoader;
    private final ToolRegistry toolRegistry;
    private final ToolRegistrationStore store;

    ExperimentalToolRegistrationStrategy(
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
        return ToolRegistrationPhase.EXPERIMENTAL;
    }

    @Override
    public ToolRegistrationResult register(ToolRegistrationRequest request, ToolRegistrationContext context) {
        LocalJarSpec localJar = request.localJar();
        if (localJar == null || localJar.path() == null || localJar.path().isBlank()) {
            throw ToolRegistrationErrors.invalidParams("localJar.path is required.", "TOOL_REGISTRATION_LOCAL_JAR_PATH_REQUIRED");
        }
        MeshingressProperties.Tools.Registration config = properties.tools().registration();
        if (config.requireLocalJarChecksum() && (localJar.checksumSha256() == null || localJar.checksumSha256().isBlank())) {
            throw ToolRegistrationErrors.invalidParams(
                    "localJar.checksumSha256 is required.",
                    "TOOL_REGISTRATION_CHECKSUM_REQUIRED"
            );
        }
        rejectNativeOverride(request.toolId(), config);
        rejectStagingOverride(request.toolId(), config);

        Optional<ToolRegistrationRecord> previous = store.findActive(request.toolId(), ToolRegistrationPhase.EXPERIMENTAL);
        if (previous.isEmpty()) {
            rejectBundleOverride(request.toolId(), config);
        } else if (!request.replace()) {
            throw ToolRegistrationErrors.invalidParams(
                    "An experimental registration is already active for this tool.",
                    "TOOL_REGISTRATION_CONFLICT"
            );
        }

        previous.ifPresent(record -> {
            if (record.runtimeModuleId() != null) {
                runtimeLoader.deactivate(new ToolModuleId(record.runtimeModuleId()));
            }
            store.markReplaced(record.registrationId());
        });

        Path jarPath = resolveJar(localJar.path(), config.localJarRoot());
        verifyChecksum(jarPath, localJar.checksumSha256());
        ToolModuleHandle handle = runtimeLoader.activate(new LocalJarSource(jarPath));

        Map<String, String> source = new LinkedHashMap<>();
        source.put("path", jarPath.toString());
        source.put("checksumSha256", localJar.checksumSha256() == null ? "" : localJar.checksumSha256());
        ToolRegistrationRecord record = new ToolRegistrationRecord(
                ToolRegistrationIds.registrationId(request),
                request.toolId(),
                phase(),
                ToolSourceKind.LOCAL_JAR,
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

    private Path resolveJar(String jarPath, String root) {
        Path rootPath = Path.of(root).toAbsolutePath().normalize();
        Path requested = Path.of(jarPath);
        Path resolved = requested.isAbsolute() ? requested.toAbsolutePath().normalize() : rootPath.resolve(requested).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw ToolRegistrationErrors.invalidParams(
                    "localJar.path must resolve under meshingress.tools.registration.local-jar-root.",
                    "TOOL_REGISTRATION_LOCAL_JAR_OUTSIDE_ROOT"
            );
        }
        if (!Files.isRegularFile(resolved) || !resolved.getFileName().toString().endsWith(".jar")) {
            throw ToolRegistrationErrors.invalidParams(
                    "localJar.path must point to an existing .jar file.",
                    "TOOL_REGISTRATION_LOCAL_JAR_NOT_FOUND"
            );
        }
        return resolved;
    }

    private void verifyChecksum(Path sourceJar, String expectedSha256) {
        if (expectedSha256 == null || expectedSha256.isBlank()) {
            return;
        }
        String actual = sha256(sourceJar);
        if (!actual.equalsIgnoreCase(expectedSha256)) {
            throw ToolRegistrationErrors.invalidParams(
                    "localJar.checksumSha256 does not match the local jar.",
                    "TOOL_REGISTRATION_CHECKSUM_MISMATCH"
            );
        }
    }

    private String sha256(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new DigestInputStream(Files.newInputStream(path), digest)) {
                input.transferTo(java.io.OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw ToolRegistrationErrors.invalidParams(
                    "Unable to calculate local jar checksum: " + exception.getMessage(),
                    "TOOL_REGISTRATION_CHECKSUM_FAILED"
            );
        }
    }

    private void rejectNativeOverride(String toolId, MeshingressProperties.Tools.Registration config) {
        if (!config.allowOverrideNative() && isReservedNative(toolId)) {
            throw ToolRegistrationErrors.forbidden(
                    "Experimental registration cannot override server-native namespaces.",
                    "TOOL_REGISTRATION_NATIVE_OVERRIDE_DENIED"
            );
        }
    }

    private void rejectBundleOverride(String toolId, MeshingressProperties.Tools.Registration config) {
        boolean present = toolRegistry.findTool(toolId).isPresent() || toolRegistry.findEnabledFunction(toolId).isPresent();
        if (present && !config.allowExperimentalOverrideBundle()) {
            throw ToolRegistrationErrors.forbidden(
                    "Experimental registration cannot override a bundled/classpath tool by default.",
                    "TOOL_REGISTRATION_BUNDLE_OVERRIDE_DENIED"
            );
        }
    }

    private void rejectStagingOverride(String toolId, MeshingressProperties.Tools.Registration config) {
        if (!config.allowExperimentalOverrideStaging() && store.findActive(toolId, ToolRegistrationPhase.STAGING).isPresent()) {
            throw ToolRegistrationErrors.forbidden(
                    "Experimental registration cannot override an active staging registration.",
                    "TOOL_REGISTRATION_STAGING_OVERRIDE_DENIED"
            );
        }
    }

    private boolean isReservedNative(String toolId) {
        return toolId.startsWith("meshingress.") || toolId.startsWith("system.") || toolId.startsWith("runtime.");
    }
}

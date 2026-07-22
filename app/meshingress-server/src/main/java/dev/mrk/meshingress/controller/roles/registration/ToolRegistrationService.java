package dev.mrk.meshingress.controller.roles.registration;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.artifact.storage.ProjectRootResolver;
import dev.mrk.meshingress.mcp.tools.registry.ToolRegistry;
import dev.mrk.meshingress.server.install.RuntimeToolCache;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleId;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleStatus;
import dev.mrk.meshingress.runtime.loader.ToolRuntimeLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ToolRegistrationService {

    private final ObjectMapper objectMapper;
    private final MeshingressProperties properties;
    private final ToolRegistrationStore store;
    private final ToolRuntimeLoader runtimeLoader;
    private final RuntimeToolCache runtimeToolCache;
    private final ToolRegistry toolRegistry;
    private final Map<ToolRegistrationPhase, ToolRegistrationStrategy> strategies;
    private final Path projectRoot;
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    @Autowired
    public ToolRegistrationService(
            ObjectMapper objectMapper,
            MeshingressProperties properties,
            ToolRegistrationStore store,
            ToolRuntimeLoader runtimeLoader,
            RuntimeToolCache runtimeToolCache,
            ToolRegistry toolRegistry,
            List<ToolRegistrationStrategy> strategies
    ) {
        this(objectMapper, properties, store, runtimeLoader, runtimeToolCache, toolRegistry, strategies,
                ProjectRootResolver.resolve(ToolRegistrationService.class, null));
    }

    ToolRegistrationService(
            ObjectMapper objectMapper,
            MeshingressProperties properties,
            ToolRegistrationStore store,
            ToolRuntimeLoader runtimeLoader,
            RuntimeToolCache runtimeToolCache,
            ToolRegistry toolRegistry,
            List<ToolRegistrationStrategy> strategies,
            Path projectRoot
    ) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.store = store;
        this.runtimeLoader = runtimeLoader;
        this.runtimeToolCache = runtimeToolCache;
        this.toolRegistry = toolRegistry;
        this.projectRoot = ProjectRootResolver.resolve(ToolRegistrationService.class, projectRoot);
        this.strategies = new EnumMap<>(ToolRegistrationPhase.class);
        for (ToolRegistrationStrategy strategy : strategies) {
            this.strategies.put(strategy.phase(), strategy);
        }
    }

    public boolean isPhaseRegistration(ToolRegistrationParams params) {
        return params != null
                && (hasText(params.phase())
                || hasText(params.toolId())
                || params.localJar() != null
                || params.maven() != null
                || params.bundle() != null
                || params.nativeTool() != null);
    }

    public boolean hasActiveRegistration(String toolId) {
        return !store.findActive(toolId).isEmpty();
    }

    public ObjectNode register(McpCallContext callContext, ToolRegistrationParams params) {
        ToolRegistrationRequest request = requestFromParams(params);
        validatePhaseEnabled(request.phase());
        ToolRegistrationStrategy strategy = strategies.get(request.phase());
        if (strategy == null) {
            throw ToolRegistrationErrors.invalidParams(
                    "No registration strategy is available for phase.",
                    "TOOL_REGISTRATION_STRATEGY_MISSING"
            );
        }

        Object lock = locks.computeIfAbsent(request.toolId(), ignored -> new Object());
        synchronized (lock) {
            return strategy.register(request, ToolRegistrationContext.from(callContext)).toJson(objectMapper);
        }
    }

    public ObjectNode delete(McpCallContext callContext, String toolId, String mode) {
        List<ToolRegistrationRecord> activeRecords = store.findActive(toolId);
        if (activeRecords.isEmpty()) {
            throw ToolRegistrationErrors.invalidParams(
                    "Tool has no active phase registration.",
                    "TOOL_REGISTRATION_NOT_ACTIVE"
            );
        }

        Object lock = locks.computeIfAbsent(toolId, ignored -> new Object());
        synchronized (lock) {
            ObjectNode result = objectMapper.createObjectNode();
            result.put("deleted", true);
            result.put("disabled", false);
            result.put("toolId", toolId);
            result.put("mode", mode);

            ArrayNode registrations = objectMapper.createArrayNode();
            boolean restartRequired = false;
            int runtimeDeactivated = 0;
            int runtimeCacheRemoved = 0;
            int bundleDependenciesRemoved = 0;
            for (ToolRegistrationRecord record : activeRecords) {
                DeleteOutcome outcome = deleteRecord(record);
                if (outcome.runtimeDeactivated()) {
                    runtimeDeactivated++;
                }
                if (outcome.runtimeCacheRemoved()) {
                    runtimeCacheRemoved++;
                }
                if (outcome.bundleDependencyRemoved()) {
                    bundleDependenciesRemoved++;
                }
                restartRequired = restartRequired || outcome.restartRequired();

                ToolRegistrationRecord updated = store.markStatus(record.registrationId(), outcome.status());
                ObjectNode recordJson = recordToJson(updated);
                recordJson.put("runtimeDeactivated", outcome.runtimeDeactivated());
                recordJson.put("runtimeCacheRemoved", outcome.runtimeCacheRemoved());
                recordJson.put("bundleDependencyRemoved", outcome.bundleDependencyRemoved());
                recordJson.put("restartRequired", outcome.restartRequired());
                if (hasText(outcome.message())) {
                    recordJson.put("message", outcome.message());
                }
                registrations.add(recordJson);
            }

            result.put("runtimeDeactivated", runtimeDeactivated);
            result.put("runtimeCacheRemoved", runtimeCacheRemoved);
            result.put("bundleDependenciesRemoved", bundleDependenciesRemoved);
            result.put("restartRequired", restartRequired);
            result.put("registryVersion", toolRegistry.registryVersion());
            result.set("registrations", registrations);
            return result;
        }
    }

    public ArrayNode registrationsToJson() {
        ArrayNode registrations = objectMapper.createArrayNode();
        for (ToolRegistrationRecord record : store.list()) {
            registrations.add(recordToJson(record));
        }
        return registrations;
    }

    public ObjectNode reloadStatus() {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("reloaded", false);
        result.put("supported", false);
        result.put("registryVersion", toolRegistry.registryVersion());
        result.put("message", "Runtime reload is not supported; bundle and native changes require rebuild/restart.");

        ArrayNode runtimeModules = objectMapper.createArrayNode();
        for (ToolModuleStatus status : runtimeLoader.list()) {
            ObjectNode statusJson = objectMapper.createObjectNode();
            statusJson.put("moduleId", status.moduleId().value());
            statusJson.put("state", status.state().name());
            if (status.activatedAt() != null) {
                statusJson.put("activatedAt", status.activatedAt().toString());
            }
            statusJson.put("updatedAt", status.updatedAt().toString());
            if (status.message() != null) {
                statusJson.put("message", status.message());
            }
            ArrayNode functions = objectMapper.createArrayNode();
            status.registeredFunctions().forEach(functions::add);
            statusJson.set("registeredFunctions", functions);
            runtimeModules.add(statusJson);
        }
        result.set("runtimeModules", runtimeModules);
        result.set("registrations", registrationsToJson());
        return result;
    }

    private DeleteOutcome deleteRecord(ToolRegistrationRecord record) {
        if (record.runtimeModuleId() != null && !record.runtimeModuleId().isBlank()) {
            runtimeLoader.deactivate(new ToolModuleId(record.runtimeModuleId()));
            boolean cacheRemoved = removePublicationRuntimeCache(record);
            return new DeleteOutcome(
                    "deleted",
                    true,
                    cacheRemoved,
                    false,
                    false,
                    cacheRemoved
                            ? "Runtime module deactivated and runtime cache artifact removed."
                            : "Runtime module deactivated."
            );
        }

        return switch (record.sourceKind()) {
            case MAVEN_BUNDLE -> deleteMavenBundleRecord(record);
            case CLASSPATH_BUNDLE -> new DeleteOutcome(
                    "reconciled-deleted",
                    false,
                    false,
                    false,
                    true,
                    "Classpath bundle tool remains available until the server is rebuilt/restarted without it."
            );
            case SERVER_NATIVE -> new DeleteOutcome(
                    "reconciled-deleted",
                    false,
                    false,
                    false,
                    true,
                    "Native server tool cannot be removed from the current JVM; rebuild/restart is required."
            );
            case LOCAL_JAR, MAVEN_COORDINATES, PUBLICATION_RECORD -> new DeleteOutcome(
                    "deleted",
                    false,
                    false,
                    false,
                    false,
                    "Registration record deleted; no active runtime module was attached."
            );
        };
    }

    private boolean removePublicationRuntimeCache(ToolRegistrationRecord record) {
        if (record.sourceKind() != ToolSourceKind.PUBLICATION_RECORD) {
            return false;
        }
        String runtimeCachePath = record.source().getOrDefault("runtimeCachePath", "");
        if (runtimeCachePath.isBlank()) {
            return false;
        }
        try {
            runtimeToolCache.remove(ProjectRootResolver.resolveRelative(projectRoot, runtimeCachePath));
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private DeleteOutcome deleteMavenBundleRecord(ToolRegistrationRecord record) {
        Map<String, String> source = record.source();
        boolean dependencyAdded = Boolean.parseBoolean(source.getOrDefault("dependencyAdded", "false"));
        if (!dependencyAdded) {
            return new DeleteOutcome(
                    "deleted-restart-required",
                    false,
                    false,
                    false,
                    true,
                    "Bundle dependency was not removed because it was already present before registration."
            );
        }

        String bundlePomPath = source.getOrDefault("bundlePomPath", properties.tools().registration().bundlePomPath());
        String groupId = source.getOrDefault("groupId", "");
        String artifactId = source.getOrDefault("artifactId", "");
        String version = source.getOrDefault("version", "");
        boolean removed = removeBundleDependency(Path.of(bundlePomPath).toAbsolutePath().normalize(), groupId, artifactId, version);
        return new DeleteOutcome(
                "deleted-restart-required",
                false,
                false,
                removed,
                true,
                removed
                        ? "Bundle dependency removed from the bundle POM; rebuild/restart is required."
                        : "Bundle dependency was not found in the bundle POM; rebuild/restart may still be required."
        );
    }

    private boolean removeBundleDependency(Path bundlePom, String groupId, String artifactId, String version) {
        if (groupId.isBlank() || artifactId.isBlank() || version.isBlank()) {
            throw ToolRegistrationErrors.invalidParams(
                    "Bundle registration record is missing Maven coordinates.",
                    "BUNDLE_REGISTRATION_COORDINATES_MISSING"
            );
        }
        if (!Files.isRegularFile(bundlePom)) {
            throw ToolRegistrationErrors.invalidParams(
                    "Bundle POM does not exist.",
                    "BUNDLE_POM_NOT_FOUND"
            );
        }
        try {
            String content = Files.readString(bundlePom, StandardCharsets.UTF_8);
            String dependencyPattern = "(?s)\\R?[ \\t]*<dependency>\\s*\\R"
                    + "\\s*<groupId>" + Pattern.quote(groupId) + "</groupId>\\s*\\R"
                    + "\\s*<artifactId>" + Pattern.quote(artifactId) + "</artifactId>\\s*\\R"
                    + "\\s*<version>" + Pattern.quote(version) + "</version>\\s*\\R"
                    + "\\s*</dependency>\\s*\\R?";
            Matcher matcher = Pattern.compile(dependencyPattern).matcher(content);
            if (!matcher.find()) {
                return false;
            }
            Files.writeString(bundlePom, matcher.replaceFirst(System.lineSeparator()), StandardCharsets.UTF_8);
            return true;
        } catch (Exception exception) {
            throw ToolRegistrationErrors.invalidParams(
                    "Unable to remove dependency from bundle POM: " + exception.getMessage(),
                    "BUNDLE_POM_UPDATE_FAILED"
            );
        }
    }

    private ObjectNode recordToJson(ToolRegistrationRecord record) {
        ObjectNode recordJson = objectMapper.createObjectNode();
        recordJson.put("registrationId", record.registrationId());
        recordJson.put("toolId", record.toolId());
        recordJson.put("phase", record.phase().wireName());
        recordJson.put("sourceKind", record.sourceKind().name());
        recordJson.put("status", record.status());
        recordJson.put("actor", record.actor());
        recordJson.put("registeredAt", record.registeredAt().toString());
        if (record.requestId() != null) {
            recordJson.put("requestId", record.requestId());
        }
        if (record.replacedRegistrationId() != null) {
            recordJson.put("replacedRegistrationId", record.replacedRegistrationId());
        }
        if (record.runtimeModuleId() != null) {
            recordJson.put("runtimeModuleId", record.runtimeModuleId());
        }

        ObjectNode source = objectMapper.createObjectNode();
        record.source().forEach(source::put);
        recordJson.set("source", source);

        ArrayNode functions = objectMapper.createArrayNode();
        record.registeredFunctions().forEach(functions::add);
        recordJson.set("registeredFunctions", functions);
        return recordJson;
    }

    private ToolRegistrationRequest requestFromParams(ToolRegistrationParams params) {
        if (params == null) {
            throw ToolRegistrationErrors.invalidParams(
                    "roles/tools/register params must be an object.",
                    "TOOL_REGISTRATION_PARAMS_REQUIRED"
            );
        }
        if (!hasText(params.phase())) {
            throw ToolRegistrationErrors.invalidParams(
                    "roles/tools/register params.phase is required for phase-aware registration.",
                    "TOOL_REGISTRATION_PHASE_REQUIRED"
            );
        }

        ToolRegistrationPhase phase = ToolRegistrationPhase.fromWire(textOrEmpty(params.phase()));
        String toolId = ToolRegistrationIds.canonicalToolId(firstText(
                params.toolId(),
                params.name(),
                params.tool() == null ? null : params.tool().name()
        ));
        return new ToolRegistrationRequest(
                toolId,
                phase,
                localJarFrom(params.localJar()),
                mavenFrom(params.maven()),
                bundleFrom(params.bundle()),
                nativeFrom(params.nativeTool(), toolId),
                params.replace() == null ? defaultReplace(phase) : params.replace()
        );
    }

    private void validatePhaseEnabled(ToolRegistrationPhase phase) {
        MeshingressProperties.Tools.Registration registration = properties.tools().registration();
        if (!registration.enabled()) {
            throw ToolRegistrationErrors.forbidden(
                    "Tool registration is disabled.",
                    "TOOL_REGISTRATION_DISABLED"
            );
        }
        boolean allowed = switch (phase) {
            case EXPERIMENTAL -> registration.allowExperimental();
            case STAGING -> registration.allowStaging();
            case BUNDLE -> registration.allowBundle();
            case NATIVE -> registration.allowNativeHttp();
        };
        if (!allowed) {
            throw ToolRegistrationErrors.forbidden(
                    "Tool registration phase is disabled.",
                    "TOOL_REGISTRATION_PHASE_DISABLED"
            );
        }
    }

    private boolean defaultReplace(ToolRegistrationPhase phase) {
        return switch (phase) {
            case EXPERIMENTAL -> properties.tools().registration().experimentalReplaceExisting();
            case STAGING -> properties.tools().registration().stagingConflictPolicy()
                    == MeshingressProperties.Tools.StagingConflictPolicy.REPLACE_EXISTING;
            case BUNDLE, NATIVE -> false;
        };
    }

    private LocalJarSpec localJarFrom(ToolRegistrationLocalJarParams localJar) {
        if (localJar == null) {
            return null;
        }
        return new LocalJarSpec(
                firstText(localJar.path(), localJar.jarPath()),
                firstText(localJar.checksumSha256(), localJar.sha256()),
                localJar.pomPath()
        );
    }

    private MavenCoordinatesSpec mavenFrom(ToolRegistrationMavenParams maven) {
        if (maven == null) {
            return null;
        }
        List<URI> repositories = new ArrayList<>();
        List<String> repositoryValues = maven.repositories();
        if (repositoryValues != null) {
            for (String repository : repositoryValues) {
                if (repository != null && !repository.isBlank()) {
                    repositories.add(URI.create(repository));
                }
            }
        }
        return new MavenCoordinatesSpec(
                textOrEmpty(maven.groupId()),
                textOrEmpty(maven.artifactId()),
                textOrEmpty(maven.version()),
                repositories
        );
    }

    private BundleSpec bundleFrom(ToolRegistrationBundleParams bundle) {
        if (bundle == null) {
            return new BundleSpec("meshingress-tool-bundle");
        }
        String bundleId = firstText(bundle.bundleId(), bundle.artifactId());
        return new BundleSpec(bundleId == null || bundleId.isBlank() ? "meshingress-tool-bundle" : bundleId);
    }

    private NativeSpec nativeFrom(ToolRegistrationNativeParams nativeTool, String toolId) {
        if (nativeTool == null) {
            return new NativeSpec(inferNamespace(toolId));
        }
        String namespace = textOrEmpty(nativeTool.namespace());
        return new NativeSpec(namespace.isBlank() ? inferNamespace(toolId) : namespace);
    }

    private String inferNamespace(String toolId) {
        int dot = toolId.indexOf('.');
        return dot < 0 ? toolId : toolId.substring(0, dot);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record DeleteOutcome(
            String status,
            boolean runtimeDeactivated,
            boolean runtimeCacheRemoved,
            boolean bundleDependencyRemoved,
            boolean restartRequired,
            String message
    ) {
    }
}

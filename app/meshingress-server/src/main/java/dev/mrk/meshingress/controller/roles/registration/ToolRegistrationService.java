package dev.mrk.meshingress.controller.roles.registration;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.config.MeshingressProperties;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ToolRegistrationService {

    private final ObjectMapper objectMapper;
    private final MeshingressProperties properties;
    private final Map<ToolRegistrationPhase, ToolRegistrationStrategy> strategies;
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    public ToolRegistrationService(
            ObjectMapper objectMapper,
            MeshingressProperties properties,
            List<ToolRegistrationStrategy> strategies
    ) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.strategies = new EnumMap<>(ToolRegistrationPhase.class);
        for (ToolRegistrationStrategy strategy : strategies) {
            this.strategies.put(strategy.phase(), strategy);
        }
    }

    public boolean isPhaseRegistration(JsonNode params) {
        return params != null
                && params.isObject()
                && (params.has("phase")
                || params.has("toolId")
                || params.has("localJar")
                || params.has("maven")
                || params.has("bundle")
                || params.has("nativeTool"));
    }

    public ObjectNode register(McpCallContext callContext, JsonNode params) {
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

    private ToolRegistrationRequest requestFromParams(JsonNode params) {
        if (params == null || !params.isObject()) {
            throw ToolRegistrationErrors.invalidParams(
                    "roles/tools/register params must be an object.",
                    "TOOL_REGISTRATION_PARAMS_REQUIRED"
            );
        }
        if (!params.has("phase")) {
            throw ToolRegistrationErrors.invalidParams(
                    "roles/tools/register params.phase is required for phase-aware registration.",
                    "TOOL_REGISTRATION_PHASE_REQUIRED"
            );
        }

        ToolRegistrationPhase phase = ToolRegistrationPhase.fromWire(params.path("phase").asString(""));
        String toolId = ToolRegistrationIds.canonicalToolId(firstText(
                params.path("toolId"),
                params.path("name"),
                params.path("tool").path("name")
        ));
        return new ToolRegistrationRequest(
                toolId,
                phase,
                localJarFrom(params.path("localJar")),
                mavenFrom(params.path("maven")),
                bundleFrom(params.path("bundle")),
                nativeFrom(params.path("nativeTool"), toolId),
                params.path("replace").asBoolean(defaultReplace(phase))
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

    private LocalJarSpec localJarFrom(JsonNode localJar) {
        if (localJar == null || localJar.isMissingNode() || localJar.isNull()) {
            return null;
        }
        if (!localJar.isObject()) {
            throw ToolRegistrationErrors.invalidParams("localJar must be an object.", "TOOL_REGISTRATION_LOCAL_JAR_INVALID");
        }
        return new LocalJarSpec(
                firstText(localJar.path("path"), localJar.path("jarPath")),
                firstText(localJar.path("checksumSha256"), localJar.path("sha256"))
        );
    }

    private MavenCoordinatesSpec mavenFrom(JsonNode maven) {
        if (maven == null || maven.isMissingNode() || maven.isNull()) {
            return null;
        }
        if (!maven.isObject()) {
            throw ToolRegistrationErrors.invalidParams("maven must be an object.", "TOOL_REGISTRATION_MAVEN_INVALID");
        }
        List<URI> repositories = new ArrayList<>();
        JsonNode repositoriesNode = maven.path("repositories");
        if (repositoriesNode.isArray()) {
            for (JsonNode repository : repositoriesNode) {
                repositories.add(URI.create(repository.asString()));
            }
        }
        return new MavenCoordinatesSpec(
                maven.path("groupId").asString(""),
                maven.path("artifactId").asString(""),
                maven.path("version").asString(""),
                repositories
        );
    }

    private BundleSpec bundleFrom(JsonNode bundle) {
        if (bundle == null || bundle.isMissingNode() || bundle.isNull()) {
            return new BundleSpec("meshingress-tool-bundle");
        }
        if (!bundle.isObject()) {
            throw ToolRegistrationErrors.invalidParams("bundle must be an object.", "TOOL_REGISTRATION_BUNDLE_INVALID");
        }
        String bundleId = firstText(bundle.path("bundleId"), bundle.path("artifactId"));
        return new BundleSpec(bundleId == null || bundleId.isBlank() ? "meshingress-tool-bundle" : bundleId);
    }

    private NativeSpec nativeFrom(JsonNode nativeTool, String toolId) {
        if (nativeTool == null || nativeTool.isMissingNode() || nativeTool.isNull()) {
            return new NativeSpec(inferNamespace(toolId));
        }
        if (!nativeTool.isObject()) {
            throw ToolRegistrationErrors.invalidParams("nativeTool must be an object.", "TOOL_REGISTRATION_NATIVE_INVALID");
        }
        String namespace = nativeTool.path("namespace").asString("");
        return new NativeSpec(namespace.isBlank() ? inferNamespace(toolId) : namespace);
    }

    private String inferNamespace(String toolId) {
        int dot = toolId.indexOf('.');
        return dot < 0 ? toolId : toolId.substring(0, dot);
    }

    private String firstText(JsonNode... values) {
        for (JsonNode value : values) {
            if (value != null && !value.isMissingNode() && !value.isNull()) {
                String text = value.asString("");
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return "";
    }
}

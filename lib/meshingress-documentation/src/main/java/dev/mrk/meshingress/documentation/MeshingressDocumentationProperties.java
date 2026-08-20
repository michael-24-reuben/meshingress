package dev.mrk.meshingress.documentation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Objects;

@ConfigurationProperties(prefix = "meshingress.documentation")
public record MeshingressDocumentationProperties(
        boolean enabled,
        Dispatch dispatch,
        OpenApi openapi,
        Refresh refresh
) {
    public MeshingressDocumentationProperties {
        dispatch = dispatch == null ? Dispatch.defaults() : dispatch;
        openapi = openapi == null ? OpenApi.defaults() : openapi;
        refresh = refresh == null ? Refresh.defaults() : refresh;
    }

    public record Dispatch(
            boolean enabled,
            String jsonPath,
            String yamlPath,
            List<String> targetModules,
            List<String> scanBasePackages,
            String title,
            String version
    ) {
        public Dispatch {
            jsonPath = defaultString(jsonPath, "/v3/dispatch-docs");
            yamlPath = defaultString(yamlPath, "/v3/dispatch-docs.yaml");
            targetModules = normalizeList(targetModules, List.of("app/meshingress-server"));
            scanBasePackages = normalizeList(scanBasePackages, List.of("dev.mrk.meshingress"));
            title = defaultString(title, "Meshingress MCP Dispatch API");
            version = defaultString(version, "v0");
        }

        static Dispatch defaults() {
            return new Dispatch(
                    true,
                    "/v3/dispatch-docs",
                    "/v3/dispatch-docs.yaml",
                    List.of("app/meshingress-server"),
                    List.of("dev.mrk.meshingress"),
                    "Meshingress MCP Dispatch API",
                    "v0"
            );
        }
    }

    public record OpenApi(
            boolean enabled,
            String jsonPath,
            String yamlPath
    ) {
        public OpenApi {
            jsonPath = defaultString(jsonPath, "/v3/openapi-docs");
            yamlPath = defaultString(yamlPath, "/v3/openapi-docs.yaml");
        }

        static OpenApi defaults() {
            return new OpenApi(true, "/v3/openapi-docs", "/v3/openapi-docs.yaml");
        }
    }

    public record Refresh(
            boolean enabled,
            String dispatchOutput,
            String openapiOutput
    ) {
        public Refresh {
            dispatchOutput = defaultString(dispatchOutput, "docs/meshingress-dispatch.yaml");
            openapiOutput = defaultString(openapiOutput, "docs/meshingress-openapi.yaml");
        }

        static Refresh defaults() {
            return new Refresh(true, "docs/meshingress-dispatch.yaml", "docs/meshingress-openapi.yaml");
        }
    }

    private static String defaultString(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static List<String> normalizeList(List<String> values, List<String> fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        List<String> normalized = values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
        return normalized.isEmpty() ? fallback : normalized;
    }
}

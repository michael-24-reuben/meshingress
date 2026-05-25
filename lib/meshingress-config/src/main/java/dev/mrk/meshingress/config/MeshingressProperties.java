package dev.mrk.meshingress.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

@ConfigurationProperties(prefix = "meshingress")
public record MeshingressProperties(
        @Valid @NotNull Identity identity,
        @Valid @NotNull Mcp mcp,
        @Valid @NotNull Tools tools,
        @Valid @NotNull Dispatch dispatch,
        @Valid @NotNull Security security,
        @Valid @NotNull Scopes scopes,
        @Valid @NotNull Audit audit,
        @Valid @NotNull Secrets secrets
) {
    public MeshingressProperties {
        identity = identity == null ? Identity.defaults() : identity;
        mcp = mcp == null ? Mcp.defaults() : mcp;
        tools = tools == null ? Tools.defaults() : tools;
        dispatch = dispatch == null ? Dispatch.defaults() : dispatch;
        security = security == null ? Security.defaults() : security;
        scopes = scopes == null ? Scopes.defaults() : scopes;
        audit = audit == null ? Audit.defaults() : audit;
        secrets = secrets == null ? Secrets.defaults() : secrets;
    }

    public record Identity(
            @NotBlank String name,
            @NotBlank String instanceId,
            @NotBlank String environment,
            @NotNull URI publicBaseUrl,
            @NotBlank String nodeRole
    ) {
        public Identity {
            name = defaultString(name, "meshingress");
            instanceId = defaultString(instanceId, "dev-node-01");
            environment = defaultString(environment, "dev");
            publicBaseUrl = Objects.requireNonNullElse(publicBaseUrl, URI.create("http://localhost:8080"));
            nodeRole = defaultString(nodeRole, "edge-ingress");
        }

        static Identity defaults() {
            return new Identity("meshingress", "dev-node-01", "dev", URI.create("http://localhost:8080"), "edge-ingress");
        }
    }

    public record Mcp(
            @Valid @NotNull WebSocket websocket
    ) {
        public Mcp {
            websocket = websocket == null ? WebSocket.defaults() : websocket;
        }

        static Mcp defaults() {
            return new Mcp(WebSocket.defaults());
        }

        public record WebSocket(
                boolean enabled,
                @NotBlank String path,
                @NotNull List<String> allowedOrigins,
                @NotNull DataSize maxMessageSize,
                @NotNull Duration sendTimeout,
                @NotNull Duration idleTimeout,
                boolean requireAuth
        ) {
            public WebSocket {
                path = defaultString(path, "/mcp/ws");
                allowedOrigins = normalizeList(allowedOrigins, List.of("*"));
                maxMessageSize = Objects.requireNonNullElse(maxMessageSize, DataSize.ofMegabytes(1));
                sendTimeout = Objects.requireNonNullElse(sendTimeout, Duration.ofSeconds(30));
                idleTimeout = Objects.requireNonNullElse(idleTimeout, Duration.ofMinutes(5));
            }

            static WebSocket defaults() {
                return new WebSocket(true, "/mcp/ws", List.of("*"), DataSize.ofMegabytes(1), Duration.ofSeconds(30), Duration.ofMinutes(5), false);
            }
        }
    }

    public record Tools(
            @Valid @NotNull Registry registry,
            List<String> allowList,
            List<String> denyList,
            @NotNull Duration defaultTimeout,
            boolean defaultAudit,
            boolean defaultDebugTrace
    ) {
        public Tools {
            registry = registry == null ? Registry.defaults() : registry;
            allowList = normalizeList(allowList, List.of());
            denyList = normalizeList(denyList, List.of());
            defaultTimeout = Objects.requireNonNullElse(defaultTimeout, Duration.ofSeconds(30));
        }

        static Tools defaults() {
            return new Tools(Registry.defaults(), List.of(), List.of(), Duration.ofSeconds(30), true, false);
        }

        public record Registry(
                boolean enabled,
                boolean failOnDuplicateToolId,
                boolean failOnInvalidToolId,
                boolean includeDisabled,
                boolean scanOnStartup,
                boolean exposePrivateTools
        ) {
            static Registry defaults() {
                return new Registry(true, true, true, false, true, false);
            }
        }
    }

    public record Dispatch(
            @NotNull Duration defaultTimeout,
            @Min(1) int maxConcurrentCalls,
            @Min(0) int queueCapacity,
            boolean rejectWhenSaturated,
            boolean includeStacktrace,
            boolean includeGeneratedAt,
            boolean redactErrors
    ) {
        public Dispatch {
            defaultTimeout = Objects.requireNonNullElse(defaultTimeout, Duration.ofSeconds(30));
            maxConcurrentCalls = maxConcurrentCalls <= 0 ? 32 : maxConcurrentCalls;
        }

        static Dispatch defaults() {
            return new Dispatch(Duration.ofSeconds(30), 32, 256, true, false, true, true);
        }
    }

    public record Security(
            boolean enabled,
            @NotBlank String mode,
            boolean requireAuthentication,
            boolean requireToolApproval,
            boolean requireApprovalForPrivileged,
            boolean requireApprovalForCritical,
            boolean denyUnknownScopes,
            boolean defaultDeny
    ) {
        public Security {
            mode = defaultString(mode, "dev");
        }

        static Security defaults() {
            return new Security(true, "dev", false, false, true, true, true, true);
        }
    }

    public record Scopes(
            boolean auditRequiredForHighRisk,
            boolean explicitApprovalForCritical,
            boolean allowShellExecute,
            boolean allowFilesDelete,
            boolean allowNetworkInbound
    ) {
        static Scopes defaults() {
            return new Scopes(true, true, false, false, true);
        }
    }

    public record Audit(
            boolean enabled,
            boolean logToolCalls,
            boolean logToolResults,
            boolean logArguments,
            boolean redactSecrets,
            @NotBlank String storage,
            @Min(0) int maxArgumentLength
    ) {
        public Audit {
            storage = defaultString(storage, "log");
            maxArgumentLength = Math.max(0, maxArgumentLength);
        }

        static Audit defaults() {
            return new Audit(true, true, false, true, true, "log", 8192);
        }
    }

    public record Secrets(
            boolean enabled,
            boolean allowEnv,
            boolean allowFile,
            boolean allowInline,
            @NotBlank String redactionPlaceholder,
            boolean failOnMissing
    ) {
        public Secrets {
            redactionPlaceholder = defaultString(redactionPlaceholder, "****");
        }

        static Secrets defaults() {
            return new Secrets(true, true, false, false, "****", true);
        }
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static List<String> normalizeList(List<String> values, List<String> fallback) {
        if (values == null) {
            return List.copyOf(fallback);
        }
        List<String> normalized = values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        return normalized.isEmpty() ? List.copyOf(fallback) : normalized;
    }
}

package dev.mrk.meshingress.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

@ConfigurationProperties(prefix = "meshingress")
public record MeshingressProperties(
        @Valid @NotNull Identity identity,
        @Valid @NotNull Mcp mcp,
        @Valid @NotNull Tools tools,
        @Valid @NotNull Dispatch dispatch,
        @Valid @NotNull Cache cache,
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
        cache = cache == null ? Cache.defaults() : cache;
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
            @Valid @NotNull Registration registration,
            List<String> allowList,
            List<String> denyList,
            @NotNull Duration defaultTimeout,
            boolean defaultAudit,
            boolean defaultDebugTrace
    ) {
        public Tools {
            registry = registry == null ? Registry.defaults() : registry;
            registration = registration == null ? Registration.defaults() : registration;
            allowList = normalizeList(allowList, List.of());
            denyList = normalizeList(denyList, List.of());
            defaultTimeout = Objects.requireNonNullElse(defaultTimeout, Duration.ofSeconds(30));
        }

        static Tools defaults() {
            return new Tools(Registry.defaults(), Registration.defaults(), List.of(), List.of(), Duration.ofSeconds(30), true, false);
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

        public record Registration(
                boolean enabled,
                boolean allowExperimental,
                boolean allowStaging,
                boolean allowBundle,
                boolean allowNativeHttp,
                boolean experimentalReplaceExisting,
                boolean allowExperimentalOverrideBundle,
                boolean allowExperimentalOverrideStaging,
                boolean allowStagingOverrideBundle,
                boolean allowOverrideNative,
                @NotNull StagingConflictPolicy stagingConflictPolicy,
                @NotBlank String localJarRoot,
                @NotBlank String localMavenRepositoryPath,
                @NotBlank String bundlePomPath,
                boolean requireLocalJarChecksum,
                boolean requireMavenVersionPin,
                boolean requireApprovalForDynamicPhases
        ) {
            public Registration {
                stagingConflictPolicy = stagingConflictPolicy == null ? StagingConflictPolicy.REPLACE_EXISTING : stagingConflictPolicy;
                localJarRoot = defaultString(localJarRoot, "tools/lib");
                localMavenRepositoryPath = defaultString(localMavenRepositoryPath, defaultLocalMavenRepositoryPath());
                bundlePomPath = defaultString(bundlePomPath, "app/meshingress-tool-bundle/pom.xml");
            }

            static Registration defaults() {
                return new Registration(
                        true,
                        true,
                        true,
                        true,
                        false,
                        true,
                        false,
                        true,
                        false,
                        false,
                        StagingConflictPolicy.REPLACE_EXISTING,
                        "tools/lib",
                        defaultLocalMavenRepositoryPath(),
                        "app/meshingress-tool-bundle/pom.xml",
                        true,
                        true,
                        true
                );
            }
        }

        public enum StagingConflictPolicy {
            REJECT,
            REPLACE_EXISTING
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

    public record Cache(
            boolean enabled,
            @NotBlank String defaultStorage,
            @NotBlank String directory,
            @NotNull Duration defaultTtl,
            @NotNull Duration maxTtl,
            @NotNull DataSize maxEntrySize,
            @NotNull DataSize maxTotalSize,
            boolean createDirectories,
            boolean cleanupOnStartup
    ) {
        public Cache {
            defaultStorage = defaultString(defaultStorage, "file");
            directory = defaultString(directory, ".cache/meshingress/cache");
            defaultTtl = Objects.requireNonNullElse(defaultTtl, Duration.ofMinutes(5));
            maxTtl = Objects.requireNonNullElse(maxTtl, Duration.ofHours(1));
            maxEntrySize = Objects.requireNonNullElse(maxEntrySize, DataSize.ofMegabytes(1));
            maxTotalSize = Objects.requireNonNullElse(maxTotalSize, DataSize.ofMegabytes(256));
        }

        static Cache defaults() {
            return new Cache(
                    true,
                    "file",
                    ".cache/meshingress/cache",
                    Duration.ofMinutes(5),
                    Duration.ofHours(1),
                    DataSize.ofMegabytes(1),
                    DataSize.ofMegabytes(256),
                    true,
                    true
            );
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

    private static String defaultLocalMavenRepositoryPath() {
        return Path.of(System.getProperty("user.home"), ".m2", "repository").toString();
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

package dev.mrk.meshingress.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
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
        @Valid @NotNull Cache cache,
        @Valid @NotNull Security security,
        @Valid @NotNull Scopes scopes,
        @Valid @NotNull Audit audit,
        @Valid @NotNull Secrets secrets,
        @Valid @NotNull Repository repository
) {
    private static final String DEFAULT_CACHE_LOCATION = ".cache/meshingress";
    private static final String DEFAULT_CACHE_DIRECTORY = DEFAULT_CACHE_LOCATION + "/cache";
    private static final String DEFAULT_RUNTIME_CACHE_ROOT = DEFAULT_CACHE_LOCATION + "/runtime-tools";

    @ConstructorBinding
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
        repository = repository == null ? Repository.defaults() : repository;
    }

    /**
     * Source-compatible bridge for callers compiled against the former local-storage component.
     * The provider-owned configuration model moved to {@code toolspace/local-storage} and this
     * argument is intentionally ignored.
     */
    @Deprecated(forRemoval = true)
    public MeshingressProperties(Identity identity, Mcp mcp, Tools tools, Dispatch dispatch, Cache cache,
                                 Security security, Scopes scopes, Audit audit, Secrets secrets,
                                 Repository repository, Object removedLocalStorageConfiguration) {
        this(identity, mcp, tools, dispatch, cache, security, scopes, audit, secrets, repository);
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
                @NotBlank String bundlePomPath,
                boolean requireLocalJarChecksum,
                boolean requireMavenVersionPin,
                boolean requireApprovalForDynamicPhases
        ) {
            public Registration {
                stagingConflictPolicy = stagingConflictPolicy == null ? StagingConflictPolicy.REPLACE_EXISTING : stagingConflictPolicy;
                localJarRoot = defaultString(localJarRoot, "tools/lib");
                bundlePomPath = defaultString(bundlePomPath, "distribution/meshingress-tool-distribution/pom.xml");
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
                        "distribution/meshingress-tool-distribution/pom.xml",
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
            @NotBlank String location,
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
            location = defaultString(location, DEFAULT_CACHE_LOCATION);
            defaultStorage = defaultString(defaultStorage, "file");
            directory = defaultString(directory, location + "/cache");
            defaultTtl = Objects.requireNonNullElse(defaultTtl, Duration.ofMinutes(5));
            maxTtl = Objects.requireNonNullElse(maxTtl, Duration.ofHours(1));
            maxEntrySize = Objects.requireNonNullElse(maxEntrySize, DataSize.ofMegabytes(1));
            maxTotalSize = Objects.requireNonNullElse(maxTotalSize, DataSize.ofMegabytes(256));
        }

        static Cache defaults() {
            return new Cache(
                    true,
                    DEFAULT_CACHE_LOCATION,
                    "file",
                    DEFAULT_CACHE_DIRECTORY,
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
            boolean legacyDevelopmentTokenEnabled,
            String developmentAdminToken,
            boolean requireToolApproval,
            boolean requireApprovalForPrivileged,
            boolean requireApprovalForCritical,
            boolean denyUnknownScopes,
            boolean defaultDeny,
            @Valid @NotNull Oidc oidc,
            @Valid @NotNull Admission admission,
            @Valid @NotNull Limits limits
    ) {
        @ConstructorBinding
        public Security {
            mode = defaultString(mode, "dev");
            developmentAdminToken = developmentAdminToken == null ? "" : developmentAdminToken.trim();
            oidc = oidc == null ? Oidc.defaults() : oidc;
            admission = admission == null ? Admission.defaults() : admission;
            limits = limits == null ? Limits.defaults() : limits;
        }

        /** Compatibility constructor for callers compiled before OIDC settings were added. */
        public Security(boolean enabled, String mode, boolean requireAuthentication, boolean legacyDevelopmentTokenEnabled,
                        String developmentAdminToken, boolean requireToolApproval, boolean requireApprovalForPrivileged,
                        boolean requireApprovalForCritical, boolean denyUnknownScopes, boolean defaultDeny) {
            this(enabled, mode, requireAuthentication, legacyDevelopmentTokenEnabled, developmentAdminToken,
                    requireToolApproval, requireApprovalForPrivileged, requireApprovalForCritical, denyUnknownScopes,
                    defaultDeny, Oidc.defaults(), Admission.defaults(), Limits.defaults());
        }

        /** Compatibility constructor for callers compiled before admission and limit settings were added. */
        public Security(boolean enabled, String mode, boolean requireAuthentication, boolean legacyDevelopmentTokenEnabled,
                        String developmentAdminToken, boolean requireToolApproval, boolean requireApprovalForPrivileged,
                        boolean requireApprovalForCritical, boolean denyUnknownScopes, boolean defaultDeny, Oidc oidc) {
            this(enabled, mode, requireAuthentication, legacyDevelopmentTokenEnabled, developmentAdminToken,
                    requireToolApproval, requireApprovalForPrivileged, requireApprovalForCritical, denyUnknownScopes,
                    defaultDeny, oidc, Admission.defaults(), Limits.defaults());
        }

        static Security defaults() {
            return new Security(true, "dev", false, false, "", false, true, true, true, true, Oidc.defaults(), Admission.defaults(), Limits.defaults());
        }

        /** Deployment-owned OIDC/JWT claim mapping. No issuer value is committed to source. */
        public record Oidc(
                boolean enabled,
                String issuerUri,
                String audience,
                String tenantClaim,
                String profileIdClaim,
                String rolesClaim,
                String grantsClaim
        ) {
            public Oidc {
                issuerUri = issuerUri == null ? "" : issuerUri.trim();
                audience = audience == null ? "" : audience.trim();
                tenantClaim = defaultString(tenantClaim, "tenant_id");
                profileIdClaim = defaultString(profileIdClaim, "profile_id");
                rolesClaim = defaultString(rolesClaim, "roles");
                grantsClaim = defaultString(grantsClaim, "scope");
            }

            static Oidc defaults() { return new Oidc(false, "", "", "tenant_id", "profile_id", "roles", "scope"); }
        }

        /** Provider-neutral admission policy. CLOSED never creates a profile from a login. */
        public record Admission(AdmissionMode mode, String defaultTenantId, InitialProfileStatus initialProfileStatus) {
            public Admission {
                mode = mode == null ? AdmissionMode.CLOSED : mode;
                defaultTenantId = defaultTenantId == null ? "" : defaultTenantId.trim();
                initialProfileStatus = initialProfileStatus == null ? InitialProfileStatus.PENDING_REVIEW : initialProfileStatus;
            }
            static Admission defaults() { return new Admission(AdmissionMode.CLOSED, "", InitialProfileStatus.PENDING_REVIEW); }
        }

        public enum AdmissionMode { CLOSED, INVITE_ONLY, SUBJECT_ALLOWLIST, SELF_SERVICE_UNPRIVILEGED }
        public enum InitialProfileStatus { PENDING_REVIEW, ACTIVE }

        /** Deployment-wide limits. Zero is an explicit unlimited default, not a missing profile override. */
        public record Limits(
                boolean enabled,
                boolean recordUsage,
                @Min(0) int requestsPerWindow,
                @NotNull Duration requestWindow,
                @Min(0) int toolExecutionsPerWindow,
                @NotNull Duration toolExecutionWindow,
                @Min(0) int maxConcurrentCalls
        ) {
            public Limits {
                requestsPerWindow = Math.max(0, requestsPerWindow);
                requestWindow = Objects.requireNonNullElse(requestWindow, Duration.ofMinutes(1));
                toolExecutionsPerWindow = Math.max(0, toolExecutionsPerWindow);
                toolExecutionWindow = Objects.requireNonNullElse(toolExecutionWindow, Duration.ofMinutes(1));
                maxConcurrentCalls = Math.max(0, maxConcurrentCalls);
            }
            static Limits defaults() { return new Limits(false, false, 0, Duration.ofMinutes(1), 0, Duration.ofMinutes(1), 0); }
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

    public record Repository(
            @NotBlank String root,
            @NotBlank String runtimeCacheRoot,
            @NotBlank String runtimeRegistrationStorePath,
            String apiBaseUrl,
            @NotBlank String apiRole,
            @NotBlank String signingKeyId,
            @NotBlank String signingSecret,
            @Valid @NotNull List<PublicationVerificationKey> verificationKeys
    ) {
        public Repository {
            root = defaultString(root, "repository");
            runtimeCacheRoot = defaultString(runtimeCacheRoot, DEFAULT_RUNTIME_CACHE_ROOT);
            runtimeRegistrationStorePath = defaultString(runtimeRegistrationStorePath, "runtime/tool-registrations.json");
            apiBaseUrl = apiBaseUrl == null ? "" : apiBaseUrl.trim();
            apiRole = defaultString(apiRole, "publisher");
            signingKeyId = defaultString(signingKeyId, "local-dev-hmac");
            signingSecret = defaultString(signingSecret, "dev-repository-signing-key");
            verificationKeys = verificationKeys == null ? List.of() : List.copyOf(verificationKeys);
        }

        static Repository defaults() {
            return new Repository("repository", DEFAULT_RUNTIME_CACHE_ROOT, "runtime/tool-registrations.json", "", "publisher", "local-dev-hmac", "dev-repository-signing-key", List.of());
        }

        public record PublicationVerificationKey(
                @NotBlank String keyId,
                @NotBlank String algorithm,
                @NotBlank String publicKey,
                @NotNull PublicationVerificationKeyStatus status
        ) {
            public PublicationVerificationKey {
                keyId = keyId == null ? "" : keyId.trim();
                algorithm = defaultString(algorithm, "Ed25519");
                publicKey = publicKey == null ? "" : publicKey.trim();
                status = status == null ? PublicationVerificationKeyStatus.ACTIVE : status;
            }
        }

        public enum PublicationVerificationKeyStatus {
            ACTIVE,
            REVOKED
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

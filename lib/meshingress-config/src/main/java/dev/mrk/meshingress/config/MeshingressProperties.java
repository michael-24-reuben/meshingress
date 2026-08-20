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
import java.util.Map;
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
        @Valid @NotNull Repository repository,
        @Valid @NotNull Storage storage
) {
    private static final String DEFAULT_CACHE_LOCATION = ".cache/meshingress";
    private static final String DEFAULT_CACHE_DIRECTORY = DEFAULT_CACHE_LOCATION + "/cache";
    private static final String DEFAULT_RUNTIME_CACHE_ROOT = DEFAULT_CACHE_LOCATION + "/runtime-tools";

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
        storage = storage == null ? Storage.defaults() : storage;
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

    public record Storage(
            boolean enabled,
            @NotNull Lifecycle lifecycle,
            @NotNull DataSize maxEntrySize,
            @Valid @NotNull Local local,
            @Valid @NotNull External external,
            @Valid @NotNull Metadata metadata
    ) {
        public Storage {
            lifecycle = lifecycle == null ? Lifecycle.LOCAL_LOCAL : lifecycle;
            maxEntrySize = Objects.requireNonNullElse(maxEntrySize, DataSize.ofMegabytes(256));
            local = local == null ? Local.defaults() : local;
            external = external == null ? External.defaults() : external;
            metadata = metadata == null ? Metadata.defaults() : metadata;
        }

        static Storage defaults() {
            return new Storage(true, Lifecycle.LOCAL_LOCAL, DataSize.ofMegabytes(256), Local.defaults(), External.defaults(), Metadata.defaults());
        }

        /** Placement policy for bytes acquired by Meshingress itself. */
        public enum Lifecycle { LOCAL_LOCAL, LOCAL_EXTERNAL }
        public enum AccessMode { WRITE_ONLY }
        public enum MutationPolicy { CREATE_ONLY }
        public enum ConflictPolicy { FAIL }
        public enum RetentionPolicy { PROVIDER_MANAGED }
        public enum Provider { WEBDAV, NEXTCLOUD, GOOGLE_DRIVE, ONEDRIVE }
        public enum StagingMode { DIRECT_FINAL_ONLY, PROVIDER_SESSION }

        public record Local(
                @NotBlank String root,
                @Min(1) int maxEntries,
                @Valid @NotNull Staging staging,
                @Valid @NotNull Published published,
                @Valid @NotNull Cleanup cleanup
        ) {
            public Local {
                root = defaultString(root, DEFAULT_CACHE_LOCATION + "/storage");
                maxEntries = maxEntries <= 0 ? 1024 : maxEntries;
                staging = staging == null ? Staging.defaults() : staging;
                published = published == null ? Published.defaults() : published;
                cleanup = cleanup == null ? Cleanup.defaults() : cleanup;
            }
            static Local defaults() { return new Local(DEFAULT_CACHE_LOCATION + "/storage", 1024, Staging.defaults(), Published.defaults(), Cleanup.defaults()); }
        }

        public record Staging(@NotNull DataSize maxBytes, @NotNull Duration ttl) {
            public Staging {
                maxBytes = Objects.requireNonNullElse(maxBytes, DataSize.ofMegabytes(256));
                ttl = Objects.requireNonNullElse(ttl, Duration.ofMinutes(15));
            }
            static Staging defaults() { return new Staging(DataSize.ofMegabytes(256), Duration.ofMinutes(15)); }
        }

        public record Published(
                @NotNull DataSize maxBytes,
                @NotNull Duration defaultTtl,
                @NotNull Duration maxTtl,
                @Min(1) int defaultMaxRequests,
                @Min(1) int maxRequests,
                @Min(1) int maxConcurrentRetrievals
        ) {
            public Published {
                maxBytes = Objects.requireNonNullElse(maxBytes, DataSize.ofGigabytes(1));
                defaultTtl = Objects.requireNonNullElse(defaultTtl, Duration.ofMinutes(30));
                maxTtl = Objects.requireNonNullElse(maxTtl, Duration.ofHours(24));
                defaultMaxRequests = defaultMaxRequests <= 0 ? 1 : defaultMaxRequests;
                maxRequests = maxRequests <= 0 ? 50 : maxRequests;
                maxConcurrentRetrievals = maxConcurrentRetrievals <= 0 ? 32 : maxConcurrentRetrievals;
            }
            static Published defaults() { return new Published(DataSize.ofGigabytes(1), Duration.ofMinutes(30), Duration.ofHours(24), 1, 50, 32); }
        }

        public record Cleanup(@NotNull Duration interval, @Min(1) int batchSize) {
            public Cleanup {
                interval = Objects.requireNonNullElse(interval, Duration.ofMinutes(5));
                batchSize = batchSize <= 0 ? 100 : batchSize;
            }
            static Cleanup defaults() { return new Cleanup(Duration.ofMinutes(5), 100); }
        }

        public record External(
                String defaultTarget,
                String delegatedTarget,
                @NotNull AccessMode accessMode,
                @NotNull MutationPolicy mutationPolicy,
                @NotNull ConflictPolicy conflictPolicy,
                @NotNull RetentionPolicy retentionPolicy,
                @Min(1) int maxConcurrentUploads,
                @NotNull Duration uploadTimeout,
                @Valid @NotNull AsyncHandoff asyncHandoff,
                @NotNull Map<String, Target> targets
        ) {
            @ConstructorBinding
            public External {
                defaultTarget = defaultTarget == null ? "" : defaultTarget.trim();
                delegatedTarget = delegatedTarget == null ? "" : delegatedTarget.trim();
                accessMode = accessMode == null ? AccessMode.WRITE_ONLY : accessMode;
                mutationPolicy = mutationPolicy == null ? MutationPolicy.CREATE_ONLY : mutationPolicy;
                conflictPolicy = conflictPolicy == null ? ConflictPolicy.FAIL : conflictPolicy;
                retentionPolicy = retentionPolicy == null ? RetentionPolicy.PROVIDER_MANAGED : retentionPolicy;
                maxConcurrentUploads = maxConcurrentUploads <= 0 ? 8 : maxConcurrentUploads;
                uploadTimeout = Objects.requireNonNullElse(uploadTimeout, Duration.ofMinutes(30));
                asyncHandoff = asyncHandoff == null ? AsyncHandoff.defaults() : asyncHandoff;
                targets = targets == null ? Map.of() : Map.copyOf(targets);
            }
            /** Compatibility constructor: the former default target remains the local-byte target. */
            public External(String defaultTarget, AccessMode accessMode, MutationPolicy mutationPolicy, ConflictPolicy conflictPolicy,
                            RetentionPolicy retentionPolicy, int maxConcurrentUploads, Duration uploadTimeout,
                            AsyncHandoff asyncHandoff, Map<String, Target> targets) {
                this(defaultTarget, "", accessMode, mutationPolicy, conflictPolicy, retentionPolicy, maxConcurrentUploads,
                        uploadTimeout, asyncHandoff, targets);
            }
            public static External defaults() { return new External("", "", AccessMode.WRITE_ONLY, MutationPolicy.CREATE_ONLY, ConflictPolicy.FAIL, RetentionPolicy.PROVIDER_MANAGED, 8, Duration.ofMinutes(30), AsyncHandoff.defaults(), Map.of()); }
        }

        /** Durable-worker policy for tool-requested queued LOCAL_BYTES handoffs. */
        public record AsyncHandoff(
                @NotNull Duration workerInterval,
                @NotNull Duration leaseDuration,
                @Min(1) int maxAttempts,
                @NotNull Duration initialRetryDelay,
                @NotNull Duration maxRetryDelay
        ) {
            public AsyncHandoff {
                workerInterval = Objects.requireNonNullElse(workerInterval, Duration.ofSeconds(5));
                leaseDuration = Objects.requireNonNullElse(leaseDuration, Duration.ofMinutes(35));
                maxAttempts = maxAttempts <= 0 ? 8 : maxAttempts;
                initialRetryDelay = Objects.requireNonNullElse(initialRetryDelay, Duration.ofSeconds(5));
                maxRetryDelay = Objects.requireNonNullElse(maxRetryDelay, Duration.ofMinutes(5));
            }
            static AsyncHandoff defaults() { return new AsyncHandoff(Duration.ofSeconds(5), Duration.ofMinutes(35), 8, Duration.ofSeconds(5), Duration.ofMinutes(5)); }
        }

        public record Target(
                @NotNull Provider provider,
                boolean enabled,
                String endpoint,
                String basePath,
                String credentialRef,
                String parentFolderId,
                String driveId,
                @NotNull StagingMode stagingMode
        ) {
            public Target {
                provider = provider == null ? Provider.WEBDAV : provider;
                endpoint = endpoint == null ? "" : endpoint.trim();
                basePath = basePath == null ? "" : basePath.trim();
                credentialRef = credentialRef == null ? "" : credentialRef.trim();
                parentFolderId = parentFolderId == null ? "" : parentFolderId.trim();
                driveId = driveId == null ? "" : driveId.trim();
                stagingMode = stagingMode == null ? StagingMode.DIRECT_FINAL_ONLY : stagingMode;
            }
        }

        public record Metadata(@Valid @NotNull Sql sql) {
            public Metadata { sql = sql == null ? Sql.defaults() : sql; }
            static Metadata defaults() { return new Metadata(Sql.defaults()); }
        }

        public record Sql(@NotBlank String schema, @NotBlank String tablePrefix, @Valid @NotNull Tables table, boolean initializeSchema) {
            public Sql {
                schema = defaultString(schema, "meshingress");
                tablePrefix = defaultString(tablePrefix, "storage_");
                table = table == null ? Tables.defaults(tablePrefix) : table.withDefaults(tablePrefix);
            }
            static Sql defaults() { return new Sql("meshingress", "storage_", Tables.defaults("storage_"), true); }
        }

        public record Tables(String entries, String usage, String events) {
            public Tables withDefaults(String prefix) { return new Tables(defaultString(entries, prefix + "entries"), defaultString(usage, prefix + "usage"), defaultString(events, prefix + "events")); }
            static Tables defaults(String prefix) { return new Tables(prefix + "entries", prefix + "usage", prefix + "events"); }
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

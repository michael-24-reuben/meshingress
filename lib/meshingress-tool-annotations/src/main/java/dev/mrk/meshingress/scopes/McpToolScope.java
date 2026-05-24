package dev.mrk.meshingress.scopes;

// #architect: [security, credentials] - Scopes introduce security measures in the production of auth verification and privileges that secret-tokens contain

/**
 * Fine-grained permissions that a Meshingress tool may request.
 * <p>
 * Class-level scopes define baseline privileges.
 * Method-level scopes should be treated as additive unless the framework
 * explicitly introduces override semantics.
 */
public enum McpToolScope {

    // Local host / runtime
    LOCAL_READ(
            ScopeCategory.LOCAL,
            AccessMode.READ,
            RiskLevel.MEDIUM,
            true,
            false,
            "Read local non-file host/runtime information."
    ),

    LOCAL_WRITE(
            ScopeCategory.LOCAL,
            AccessMode.WRITE,
            RiskLevel.HIGH,
            true,
            true,
            "Modify local non-file host/runtime state."
    ),

    RUNTIME_READ(
            ScopeCategory.LOCAL,
            AccessMode.READ,
            RiskLevel.LOW,
            true,
            false,
            "Read runtime metadata such as version, capabilities, or uptime."
    ),

    RUNTIME_WRITE(
            ScopeCategory.LOCAL,
            AccessMode.WRITE,
            RiskLevel.HIGH,
            true,
            true,
            "Modify runtime behavior or mutable runtime settings."
    ),

    // Files
    FILES_LIST(
            ScopeCategory.FILES,
            AccessMode.READ,
            RiskLevel.MEDIUM,
            true,
            false,
            "List files or directories without reading file contents."
    ),

    FILES_METADATA_READ(
            ScopeCategory.FILES,
            AccessMode.READ,
            RiskLevel.MEDIUM,
            true,
            false,
            "Read file metadata such as size, timestamps, and permissions."
    ),

    FILES_READ(
            ScopeCategory.FILES,
            AccessMode.READ,
            RiskLevel.HIGH,
            true,
            false,
            "Read file contents."
    ),

    FILES_WRITE(
            ScopeCategory.FILES,
            AccessMode.WRITE,
            RiskLevel.HIGH,
            true,
            true,
            "Create or modify file contents."
    ),

    FILES_COPY(
            ScopeCategory.FILES,
            AccessMode.WRITE,
            RiskLevel.HIGH,
            true,
            true,
            "Copy files or directories."
    ),

    FILES_MOVE(
            ScopeCategory.FILES,
            AccessMode.WRITE,
            RiskLevel.HIGH,
            true,
            true,
            "Move or rename files or directories."
    ),

    FILES_DELETE(
            ScopeCategory.FILES,
            AccessMode.DELETE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Delete files or directories."
    ),

    FILES_UPLOAD(
            ScopeCategory.FILES,
            AccessMode.WRITE,
            RiskLevel.HIGH,
            true,
            true,
            "Upload files into a local or managed file store."
    ),

    FILES_DOWNLOAD(
            ScopeCategory.FILES,
            AccessMode.READ,
            RiskLevel.HIGH,
            true,
            false,
            "Download files from a local or managed file store."
    ),

    FILES_ARCHIVE(
            ScopeCategory.FILES,
            AccessMode.WRITE,
            RiskLevel.HIGH,
            true,
            true,
            "Archive, compress, or package files."
    ),

    // Network
    NETWORK_ACCESS(
            ScopeCategory.NETWORK,
            AccessMode.MANAGE,
            RiskLevel.HIGH,
            true,
            false,
            "Broad network access. Prefer narrower network scopes when possible."
    ),

    NETWORK_OUTBOUND(
            ScopeCategory.NETWORK,
            AccessMode.EXECUTE,
            RiskLevel.HIGH,
            true,
            false,
            "Open outbound network connections."
    ),

    NETWORK_INBOUND(
            ScopeCategory.NETWORK,
            AccessMode.EXECUTE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Bind or listen for inbound network connections."
    ),

    DNS_RESOLVE(
            ScopeCategory.NETWORK,
            AccessMode.READ,
            RiskLevel.MEDIUM,
            false,
            false,
            "Resolve DNS names."
    ),

    HTTP_CLIENT(
            ScopeCategory.NETWORK,
            AccessMode.EXECUTE,
            RiskLevel.HIGH,
            true,
            false,
            "Make outbound HTTP or HTTPS requests."
    ),

    WEBSOCKET_CONNECT(
            ScopeCategory.NETWORK,
            AccessMode.EXECUTE,
            RiskLevel.HIGH,
            true,
            false,
            "Open outbound WebSocket connections."
    ),

    // Database
    DATABASE_READ(
            ScopeCategory.DATABASE,
            AccessMode.READ,
            RiskLevel.HIGH,
            true,
            false,
            "Read database records."
    ),

    DATABASE_WRITE(
            ScopeCategory.DATABASE,
            AccessMode.WRITE,
            RiskLevel.HIGH,
            true,
            true,
            "Create or update database records."
    ),

    DATABASE_DELETE(
            ScopeCategory.DATABASE,
            AccessMode.DELETE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Delete database records."
    ),

    DATABASE_SCHEMA_READ(
            ScopeCategory.DATABASE,
            AccessMode.READ,
            RiskLevel.MEDIUM,
            true,
            false,
            "Read database schema metadata."
    ),

    DATABASE_SCHEMA_WRITE(
            ScopeCategory.DATABASE,
            AccessMode.ADMIN,
            RiskLevel.CRITICAL,
            true,
            true,
            "Create, alter, or drop database schema objects."
    ),

    DATABASE_TRANSACTION_MANAGE(
            ScopeCategory.DATABASE,
            AccessMode.MANAGE,
            RiskLevel.HIGH,
            true,
            true,
            "Manage explicit database transaction behavior."
    ),

    DATABASE_ADMIN(
            ScopeCategory.DATABASE,
            AccessMode.ADMIN,
            RiskLevel.CRITICAL,
            true,
            true,
            "Perform privileged database administrative operations."
    ),

    // Process / shell
    PROCESS_READ(
            ScopeCategory.PROCESS,
            AccessMode.READ,
            RiskLevel.HIGH,
            true,
            false,
            "Read process information."
    ),

    PROCESS_EXECUTE(
            ScopeCategory.PROCESS,
            AccessMode.EXECUTE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Execute a process without shell command parsing."
    ),

    PROCESS_KILL(
            ScopeCategory.PROCESS,
            AccessMode.DELETE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Terminate a process."
    ),

    SHELL_EXECUTE(
            ScopeCategory.PROCESS,
            AccessMode.EXECUTE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Execute shell commands."
    ),

    // Email / notifications
    EMAIL_SEND(
            ScopeCategory.EMAIL,
            AccessMode.EXECUTE,
            RiskLevel.HIGH,
            true,
            true,
            "Send email."
    ),

    NOTIFICATIONS_READ(
            ScopeCategory.NOTIFICATIONS,
            AccessMode.READ,
            RiskLevel.MEDIUM,
            true,
            false,
            "Read notification state or delivery status."
    ),

    NOTIFICATIONS_SEND(
            ScopeCategory.NOTIFICATIONS,
            AccessMode.EXECUTE,
            RiskLevel.HIGH,
            true,
            true,
            "Send notifications through configured channels."
    ),

    NOTIFICATIONS_MANAGE(
            ScopeCategory.NOTIFICATIONS,
            AccessMode.MANAGE,
            RiskLevel.HIGH,
            true,
            true,
            "Manage notification channels, templates, or routing."
    ),

    // Users and identity
    USER_READ(
            ScopeCategory.USER,
            AccessMode.READ,
            RiskLevel.HIGH,
            true,
            false,
            "Read user records or profile data."
    ),

    USER_WRITE(
            ScopeCategory.USER,
            AccessMode.WRITE,
            RiskLevel.HIGH,
            true,
            true,
            "Create or modify user records."
    ),

    USER_DELETE(
            ScopeCategory.USER,
            AccessMode.DELETE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Delete user records."
    ),

    // Tokens
    TOKEN_INTROSPECT(
            ScopeCategory.TOKEN,
            AccessMode.READ,
            RiskLevel.HIGH,
            true,
            false,
            "Inspect token validity or claims."
    ),

    TOKEN_ISSUE(
            ScopeCategory.TOKEN,
            AccessMode.ADMIN,
            RiskLevel.CRITICAL,
            true,
            true,
            "Issue new tokens."
    ),

    TOKEN_REVOKE(
            ScopeCategory.TOKEN,
            AccessMode.ADMIN,
            RiskLevel.CRITICAL,
            true,
            true,
            "Revoke active tokens."
    ),

    TOKEN_ROTATE(
            ScopeCategory.TOKEN,
            AccessMode.ADMIN,
            RiskLevel.CRITICAL,
            true,
            true,
            "Rotate token material or token signing state."
    ),

    // Configuration and environment
    CONFIG_READ(
            ScopeCategory.CONFIG,
            AccessMode.READ,
            RiskLevel.MEDIUM,
            true,
            false,
            "Read application configuration."
    ),

    CONFIG_WRITE(
            ScopeCategory.CONFIG,
            AccessMode.WRITE,
            RiskLevel.HIGH,
            true,
            true,
            "Modify application configuration."
    ),

    ENVIRONMENT_READ(
            ScopeCategory.ENVIRONMENT,
            AccessMode.READ,
            RiskLevel.HIGH,
            true,
            false,
            "Read environment variables or environment-derived settings."
    ),

    ENVIRONMENT_WRITE(
            ScopeCategory.ENVIRONMENT,
            AccessMode.WRITE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Modify process or runtime environment settings."
    ),

    // Secrets and keystore
    SECRETS_USE(
            ScopeCategory.SECRETS,
            AccessMode.USE,
            RiskLevel.HIGH,
            true,
            false,
            "Use a secret without exposing its raw value to the tool."
    ),

    SECRETS_READ(
            ScopeCategory.SECRETS,
            AccessMode.READ,
            RiskLevel.CRITICAL,
            true,
            true,
            "Read raw secret values."
    ),

    SECRETS_WRITE(
            ScopeCategory.SECRETS,
            AccessMode.WRITE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Create or update secrets."
    ),

    SECRETS_DELETE(
            ScopeCategory.SECRETS,
            AccessMode.DELETE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Delete secrets."
    ),

    KEYSTORE_USE(
            ScopeCategory.KEYSTORE,
            AccessMode.USE,
            RiskLevel.HIGH,
            true,
            false,
            "Use key material for signing, encryption, or authentication without exposing raw key material."
    ),

    KEYSTORE_READ(
            ScopeCategory.KEYSTORE,
            AccessMode.READ,
            RiskLevel.CRITICAL,
            true,
            true,
            "Read raw keystore material."
    ),

    KEYSTORE_WRITE(
            ScopeCategory.KEYSTORE,
            AccessMode.WRITE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Create or update keystore material."
    ),

    KEYSTORE_DELETE(
            ScopeCategory.KEYSTORE,
            AccessMode.DELETE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Delete keystore material."
    ),

    // External APIs
    EXTERNAL_API_READ(
            ScopeCategory.EXTERNAL_API,
            AccessMode.READ,
            RiskLevel.HIGH,
            true,
            false,
            "Read from external APIs."
    ),

    EXTERNAL_API_WRITE(
            ScopeCategory.EXTERNAL_API,
            AccessMode.WRITE,
            RiskLevel.HIGH,
            true,
            true,
            "Write to external APIs or trigger external API side effects."
    ),

    // Observability
    LOGS_READ(
            ScopeCategory.OBSERVABILITY,
            AccessMode.READ,
            RiskLevel.HIGH,
            true,
            false,
            "Read logs."
    ),

    LOGS_APPEND(
            ScopeCategory.OBSERVABILITY,
            AccessMode.APPEND,
            RiskLevel.MEDIUM,
            false,
            false,
            "Append log records without mutating existing logs."
    ),

    METRICS_READ(
            ScopeCategory.OBSERVABILITY,
            AccessMode.READ,
            RiskLevel.MEDIUM,
            false,
            false,
            "Read metrics."
    ),

    METRICS_EMIT(
            ScopeCategory.OBSERVABILITY,
            AccessMode.EMIT,
            RiskLevel.LOW,
            false,
            false,
            "Emit metrics."
    ),

    TELEMETRY_SEND(
            ScopeCategory.OBSERVABILITY,
            AccessMode.EMIT,
            RiskLevel.MEDIUM,
            true,
            false,
            "Send telemetry."
    ),

    // Scheduling / jobs
    SCHEDULE_READ(
            ScopeCategory.SCHEDULING,
            AccessMode.READ,
            RiskLevel.MEDIUM,
            true,
            false,
            "Read schedules."
    ),

    SCHEDULE_WRITE(
            ScopeCategory.SCHEDULING,
            AccessMode.WRITE,
            RiskLevel.HIGH,
            true,
            true,
            "Create or modify schedules."
    ),

    SCHEDULE_DELETE(
            ScopeCategory.SCHEDULING,
            AccessMode.DELETE,
            RiskLevel.HIGH,
            true,
            true,
            "Delete schedules."
    ),

    JOBS_READ(
            ScopeCategory.JOBS,
            AccessMode.READ,
            RiskLevel.MEDIUM,
            true,
            false,
            "Read job state."
    ),

    JOBS_EXECUTE(
            ScopeCategory.JOBS,
            AccessMode.EXECUTE,
            RiskLevel.HIGH,
            true,
            true,
            "Start or trigger jobs."
    ),

    JOBS_CANCEL(
            ScopeCategory.JOBS,
            AccessMode.DELETE,
            RiskLevel.HIGH,
            true,
            true,
            "Cancel jobs."
    ),

    // Tool registry / MCP control plane
    TOOLS_READ(
            ScopeCategory.TOOLS,
            AccessMode.READ,
            RiskLevel.MEDIUM,
            true,
            false,
            "Read registered tool metadata."
    ),

    TOOLS_REGISTER(
            ScopeCategory.TOOLS,
            AccessMode.WRITE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Register new tools."
    ),

    TOOLS_UPDATE(
            ScopeCategory.TOOLS,
            AccessMode.WRITE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Update registered tool metadata or behavior."
    ),

    TOOLS_DISABLE(
            ScopeCategory.TOOLS,
            AccessMode.WRITE,
            RiskLevel.HIGH,
            true,
            true,
            "Disable registered tools."
    ),

    TOOLS_DELETE(
            ScopeCategory.TOOLS,
            AccessMode.DELETE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Delete tool registrations."
    ),

    TOOLS_CALL(
            ScopeCategory.TOOLS,
            AccessMode.EXECUTE,
            RiskLevel.HIGH,
            true,
            true,
            "Invoke another registered tool."
    ),

    // Policy / authorization
    POLICY_READ(
            ScopeCategory.POLICY,
            AccessMode.READ,
            RiskLevel.HIGH,
            true,
            false,
            "Read authorization policy."
    ),

    POLICY_WRITE(
            ScopeCategory.POLICY,
            AccessMode.WRITE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Modify authorization policy."
    ),

    POLICY_EVALUATE(
            ScopeCategory.POLICY,
            AccessMode.EXECUTE,
            RiskLevel.MEDIUM,
            true,
            false,
            "Request policy evaluation."
    ),

    // Deployment / plugin runtime
    DEPLOYMENTS_READ(
            ScopeCategory.DEPLOYMENT,
            AccessMode.READ,
            RiskLevel.MEDIUM,
            true,
            false,
            "Read deployment state."
    ),

    DEPLOYMENTS_DEPLOY(
            ScopeCategory.DEPLOYMENT,
            AccessMode.EXECUTE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Deploy runtime components."
    ),

    DEPLOYMENTS_ROLLBACK(
            ScopeCategory.DEPLOYMENT,
            AccessMode.EXECUTE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Rollback deployments."
    ),

    PLUGINS_READ(
            ScopeCategory.PLUGINS,
            AccessMode.READ,
            RiskLevel.MEDIUM,
            true,
            false,
            "Read plugin metadata."
    ),

    PLUGINS_INSTALL(
            ScopeCategory.PLUGINS,
            AccessMode.EXECUTE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Install plugins or load extension code."
    ),

    PLUGINS_UPDATE(
            ScopeCategory.PLUGINS,
            AccessMode.WRITE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Update plugins."
    ),

    PLUGINS_REMOVE(
            ScopeCategory.PLUGINS,
            AccessMode.DELETE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Remove plugins."
    ),

    // Backups
    BACKUPS_READ(
            ScopeCategory.BACKUPS,
            AccessMode.READ,
            RiskLevel.CRITICAL,
            true,
            false,
            "Read backup contents or backup metadata."
    ),

    BACKUPS_CREATE(
            ScopeCategory.BACKUPS,
            AccessMode.WRITE,
            RiskLevel.HIGH,
            true,
            true,
            "Create backups."
    ),

    BACKUPS_RESTORE(
            ScopeCategory.BACKUPS,
            AccessMode.EXECUTE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Restore from backups."
    ),

    BACKUPS_DELETE(
            ScopeCategory.BACKUPS,
            AccessMode.DELETE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Delete backups."
    ),

    // Cache
    CACHE_READ(
            ScopeCategory.CACHE,
            AccessMode.READ,
            RiskLevel.MEDIUM,
            false,
            false,
            "Read cache entries."
    ),

    CACHE_WRITE(
            ScopeCategory.CACHE,
            AccessMode.WRITE,
            RiskLevel.MEDIUM,
            false,
            true,
            "Create or update cache entries."
    ),

    CACHE_DELETE(
            ScopeCategory.CACHE,
            AccessMode.DELETE,
            RiskLevel.MEDIUM,
            false,
            true,
            "Delete cache entries."
    ),

    // Streams / messaging
    STREAM_READ(
            ScopeCategory.STREAMS,
            AccessMode.READ,
            RiskLevel.HIGH,
            true,
            false,
            "Read from streams or long-lived transports."
    ),

    STREAM_WRITE(
            ScopeCategory.STREAMS,
            AccessMode.WRITE,
            RiskLevel.HIGH,
            true,
            true,
            "Write to streams or long-lived transports."
    ),

    MESSAGE_PUBLISH(
            ScopeCategory.MESSAGING,
            AccessMode.WRITE,
            RiskLevel.HIGH,
            true,
            true,
            "Publish messages."
    ),

    MESSAGE_SUBSCRIBE(
            ScopeCategory.MESSAGING,
            AccessMode.READ,
            RiskLevel.HIGH,
            true,
            false,
            "Subscribe to messages."
    ),

    QUEUE_READ(
            ScopeCategory.MESSAGING,
            AccessMode.READ,
            RiskLevel.HIGH,
            true,
            false,
            "Read queue state or messages."
    ),

    QUEUE_WRITE(
            ScopeCategory.MESSAGING,
            AccessMode.WRITE,
            RiskLevel.HIGH,
            true,
            true,
            "Write or enqueue messages."
    ),

    QUEUE_MANAGE(
            ScopeCategory.MESSAGING,
            AccessMode.MANAGE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Manage queue configuration or lifecycle."
    ),

    // Health / audit
    HEALTH_CHECK(
            ScopeCategory.HEALTH,
            AccessMode.READ,
            RiskLevel.LOW,
            false,
            false,
            "Run health checks."
    ),

    AUDIT_READ(
            ScopeCategory.AUDIT,
            AccessMode.READ,
            RiskLevel.HIGH,
            true,
            false,
            "Read audit records."
    ),

    AUDIT_APPEND(
            ScopeCategory.AUDIT,
            AccessMode.APPEND,
            RiskLevel.MEDIUM,
            true,
            false,
            "Append audit records without mutating existing audit history."
    ),

    AUDIT_EXPORT(
            ScopeCategory.AUDIT,
            AccessMode.READ,
            RiskLevel.CRITICAL,
            true,
            false,
            "Export audit records."
    ),

    AUDIT_DELETE(
            ScopeCategory.AUDIT,
            AccessMode.DELETE,
            RiskLevel.CRITICAL,
            true,
            true,
            "Delete audit records."
    );

    private final ScopeMetadata scopeMetadata;

    McpToolScope(
            ScopeCategory category,
            AccessMode accessMode,
            RiskLevel riskLevel,
            boolean auditRecommended,
            boolean privileged,
            String description
    ) {
        this.scopeMetadata = new ScopeMetadata(category, accessMode, riskLevel, auditRecommended, privileged, description);
    }

    public ScopeCategory category() {
        return scopeMetadata.category();
    }

    public AccessMode accessMode() {
        return scopeMetadata.accessMode();
    }

    public RiskLevel riskLevel() {
        return scopeMetadata.riskLevel();
    }

    public boolean auditRecommended() {
        return scopeMetadata.auditRecommended();
    }

    public boolean privileged() {
        return scopeMetadata.privileged();
    }

    public String description() {
        return scopeMetadata.description();
    }

    public boolean isReadOnly() {
        return switch (scopeMetadata.accessMode()) {
            case READ, USE -> true;
            case WRITE, DELETE, EXECUTE, ADMIN, APPEND, EMIT, MANAGE -> false;
        };
    }

    public boolean isMutating() {
        return switch (scopeMetadata.accessMode()) {
            case WRITE, DELETE, ADMIN, APPEND, EMIT, MANAGE -> true;
            case READ, EXECUTE, USE -> false;
        };
    }

    public boolean isHighRiskOrAbove() {
        return scopeMetadata.riskLevel() == RiskLevel.HIGH || scopeMetadata.riskLevel() == RiskLevel.CRITICAL;
    }

    public boolean requiresExplicitApproval() {
        return scopeMetadata.privileged() || scopeMetadata.riskLevel() == RiskLevel.CRITICAL;
    }

    public boolean requiresAudit() {
        return scopeMetadata.auditRecommended() || isHighRiskOrAbove();
    }

    public String authority() {
        return "SCOPE_" + name();
    }
}
package dev.mrk.meshingress.scopes;

// #architect: [security, credentials] - Scopes introduce security measures in the production of auth verification and privileges that secret-tokens contain

/**
 * This enum defines the various scopes that a tool can have in the MeshIngress Control Plane (MCP).
 * Each scope represents a specific permission or access level that a tool can request when it is registered with the MCP.
 * The scopes are categorized: based on the type of access they provide, such as local operations, file operations,
 * network access, database operations, shell execution, email sending, user management, configuration management, and
 * external API access. By defining these scopes, we can implement fine-grained access control for tools in the MCP,
 * ensuring that they only have the permissions they need to function properly while minimizing potential security risks.
 */
public enum McpToolScope {
    LOCAL_READ,
    LOCAL_WRITE,

    FILES_READ,
    FILES_WRITE,
    FILES_DELETE,

    NETWORK_ACCESS,

    DATABASE_READ,
    DATABASE_WRITE,
    DATABASE_DELETE,

    SHELL_EXECUTE,

    EMAIL_SEND,

    USER_READ,
    USER_WRITE,
    USER_DELETE,

    CONFIG_READ,
    CONFIG_WRITE,

    EXTERNAL_API_READ,
    EXTERNAL_API_WRITE
}
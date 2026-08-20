package dev.mrk.meshingress.api;

/** How the server verified the caller. Tool code must never receive the credential evidence. */
public enum McpAuthenticationMethod {
    ANONYMOUS,
    SPRING_SECURITY,
    OIDC_JWT,
    DEVELOPMENT_TOKEN,
    WORKFLOW
}

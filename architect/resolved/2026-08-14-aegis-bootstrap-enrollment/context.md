# Context

`packages/aegis` provides framework-free credential verification, verified
grants, capability acquisition, versioned credential-safe profiles, opaque
`SecretReference` values, and generic authorization decisions. It does not
provide persistence, secret resolution, token generation, HTTP handling,
OIDC/JWT validation, audit storage, or Spring integration.

The intended bootstrap credential is not a long-lived default password or an
ordinary administrator login. It is a short-lived, single-use enrollment
factor used only while no administrator has been established. On successful
enrollment, the host atomically marks it consumed and revokes its use. Later
administrator rotation and recovery require an explicit privileged operation;
the application must not silently issue a replacement after restart.

Spring Boot's generated development password is not an Aegis bootstrap
credential: it is printed by the framework, changes on restart, and has no
portable consumption or rotation state.

The parent MCP authentication design establishes two separate boundaries:
incoming caller identity and outgoing backend credentials. Bootstrap enrollment
creates an initial local identity/profile boundary only; it must not be
presented as production OIDC authentication or as authority to downstream
services.

# Context

The current dispatch framework is available through `@McpDispatchMapping` and
`@McpDispatchMethod`; `roles/tools/*` is the server-side precedent. The former
`@McpDispatchRoute` name is not the current annotation API.

`McpCallContext` now carries a credential-safe `McpPrincipal`, but verified
OIDC/JWT production integration remains pending. A development bearer adapter
exists only for local migration testing and cannot be the authority for a
security-management release.

`packages/aegis` provides `ProfileService`, `ProfileStore`, revisions, safe
profile values, and generic `AuthorizationPolicy` / `CredentialVerifier` SPIs.
It has no durable store implementation, Spring beans, policy-record model,
policy CRUD, route DTOs, audit emitter, or application adapter. Credential
bindings are owned by a profile identity; Aegis persists only a
`SecretReference`, never raw credential evidence.

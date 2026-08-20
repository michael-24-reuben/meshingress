# Aegis

Aegis is a framework-free Java 25 authentication and authorization core for
reuse across projects. It models verified identities, credential-safe profile
metadata, opaque secret references, and generic grant-based authorization.

`AuthProfile` is versioned by `schemaVersion` and its matching JSON Schema is
packaged at `META-INF/aegis/auth-profile.schema.json`.

The module intentionally does not provide JWT/OIDC validation, HTTP handling,
secret-store access, persistence, logging, or application-specific policy.
Hosts supply those adapters through `CredentialVerifier` and `AuthorizationPolicy`.
Raw tokens, refresh tokens, API keys, and private keys are never profile fields;
only `SecretReference` is durable.

## Ordinary profile lifecycle

`ProfileService` is the portable lifecycle API for normal profiles. It uses a
host-supplied `ProfileStore` and returns a `ProfileSnapshot` with a monotonic
revision. Every mutation requires the revision the caller read, so a durable
host implementation must compare-and-set the revision atomically. The supplied
`InMemoryProfileStore` is a synchronized test and local-demo reference only.

The service supports `create`, `get`, `list`, `updateMetadata`, `updateLimitPolicy`, `activate`,
`disable`, `revoke`, `linkIdentity`, `unlinkIdentity`, `bindCredential`,
`replaceCredential`, `revokeCredential`, and `unbindCredential`. Revocation is
terminal and marks every retained credential binding revoked. An identity cannot
be detached while bindings reference it. `unbindCredential` removes only the
Aegis metadata reference: it never deletes or revokes an upstream secret.

MCP and HTTP adapters remain host-owned. Meshingress route names are recorded
in `architect/resolved/2026-08-15-aegis-profile-lifecycle/plan.md`; this module
does not install routes, Spring beans, or authorization rules.

## Provider-neutral verified identity and limits

`VerifiedIdentity` is the safe hand-off from a host authentication provider to
profile admission. It contains a verified issuer/subject and display metadata,
but never raw evidence. A host may use it for OIDC, native credentials, or a
later provider without changing the profile model.

`ResourceLimitPolicy` is likewise provider-neutral. Each limit is explicitly
`INHERIT`, `UNLIMITED`, or `LIMITED`; a host resolves the policy hierarchy and
owns actual rate/concurrency/storage enforcement.

## Bootstrap administrator enrollment

`BootstrapEnrollmentService` is the portable, one-time first-administrator
contract. Its safe defaults are read without Spring from this module's
`src/main/resources/application.properties`:

```properties
aegis.bootstrap.enabled=false
aegis.bootstrap.action=bootstrap-enroll-admin
aegis.bootstrap.credential-ttl=PT15M
aegis.bootstrap.max-attempts=3
aegis.bootstrap.administrator-role=admin
```

Use `BootstrapEnrollmentProperties.loadDefault()` for those defaults, or
`BootstrapEnrollmentProperties.from(Properties)` / `load(InputStream)` when a
host supplies controlled configuration. Enabling it does not issue a secret:
the host must explicitly call `issue` or `rotate`, generate and retain its own
digest or external secret reference, and implement `BootstrapCredentialVerifier`.

`BootstrapEnrollmentStore.enroll` is an atomic host persistence boundary: it
must verify that no active administrator exists, consume the issuance, and
create the administrator profile in one transaction. The supplied
`InMemoryBootstrapEnrollmentStore` is only a thread-safe reference for tests
and local demonstrations. There is no Spring, HTTP, OIDC, secret-store, or
automatic restart reissue behavior in Aegis.

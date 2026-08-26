# Plan

## Required implementation sequence

1. Complete verified OIDC/JWT principal resolution and bind stable subject,
   tenant, roles, grants, expiry, and profile ID into `McpPrincipal`.
2. Define a durable transactional `ProfileStore` adapter with atomic
   expected-revision replacement and read projections for bounded profile and
   optional global credential-binding queries.
3. Define the stored tool-policy model, priority/conflict semantics, lifecycle,
   compiler/evaluator, revision contract, and audit events. Decide whether the
   first policy language is grant/scope-only or supports attribute conditions.
4. Add server-side Aegis adapters, role/grant authorization checks, route
   parameter/response DTOs, JSON-RPC error mapping, and structured audit events.
5. Add authenticated `security/whoami`, then route tests for anonymous,
   unauthorized, stale-revision, cross-tenant, disabled/revoked, and successful
   paths.
6. Add integration tests against a durable test store and a verified principal;
   do not accept raw credentials in administration calls.

## Supported MCP methods

`security/whoami` requires an authenticated principal but not an administrator.
Every `roles/security/*` method requires a host-defined privileged grant or
administrator role. Every mutation below accepts `expectedRevision` and returns
the new revision.

```text
security/whoami

roles/security/profiles/list
roles/security/profiles/get
roles/security/profiles/create
roles/security/profiles/update
roles/security/profiles/activate
roles/security/profiles/disable
roles/security/profiles/revoke
roles/security/profiles/identities/link
roles/security/profiles/identities/unlink
roles/security/profiles/credentials/bind
roles/security/profiles/credentials/replace
roles/security/profiles/credentials/revoke
roles/security/profiles/credentials/unbind

roles/security/credential-bindings/list
roles/security/credential-bindings/get

roles/security/tool-policies/list
roles/security/tool-policies/get
roles/security/tool-policies/create
roles/security/tool-policies/update
roles/security/tool-policies/delete
roles/security/tool-policies/evaluate
```

The global credential-binding read routes are optional admin projections; all
binding mutations remain profile-nested so the identity ownership check cannot
be bypassed.

`roles/security/credential-bindings/verify` is intentionally excluded. It
would require credential evidence and belongs in an internal, host-owned
authentication/credential-verifier adapter rather than an MCP administration
method. It must never receive raw tokens, API keys, refresh tokens, or private
keys in MCP arguments.

## Completion criteria

- A verified non-development principal is required for every enabled route.
- Each mutable resource has durable optimistic revisions and audit records.
- Profile, binding, and policy authorization decisions are tenant-safe and
  tested.
- Tool availability/list/call enforcement consumes the stored policy evaluator
  consistently.
- No management route logs or returns raw credential material.

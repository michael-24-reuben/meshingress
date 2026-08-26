# Product Requirements: MCP Authentication and Authorization

## Proposed Trust Model

Meshingress should have two explicit boundaries.

| Boundary | Question answered | Proposed responsibility |
|---|---|---|
| Incoming: MCP client -> Meshingress | Who is calling, and may this principal use this operation? | Validate client identity; build a normalized principal; enforce per-operation and per-tool authorization. |
| Outgoing: Meshingress -> tool/upstream | Which credential may this tool use for this backend action? | Resolve an explicit per-backend credential strategy; never expose the resolved secret to the MCP client. |

Artifact admission and scope approval are separate. A signed tool artifact is not automatically callable by every authenticated principal, and an authorized principal does not automatically receive all of the user's backend privileges.

## Incoming Authentication

| Mode | Intended use | Requirements |
|---|---|---|
| `oidc` | Production remote MCP | Validate OAuth/OIDC access tokens: issuer, audience, expiry, signature; support opaque-token introspection only when configured. Require an audience unique to the Meshingress resource. |
| `service-account` | Service-to-service or in-cluster clients | Validate a workload/service token against its configured issuer and audience. Map workload identity to the normalized principal model. |
| `local-development` | Explicit local-only development | Must be opt-in, visibly insecure, and unavailable from non-loopback or production profiles. Existing shared-token/header behavior can live here temporarily. |

`X-Mcp-Role` and `X-Mcp-Admin` must not grant production authority. The shared `dev-admin` fallback must be removed or rejected in every production profile before production authentication is enabled.

## Authorization Contract

Authorization must use a normalized decision input:

```txt
principal: subject, issuer, audience, expiry, scopes, roles/groups, authentication method
action: initialize | tools/list | tools/call | roles/tools/* | resource/prompt operation
resource: tool ID/function, visibility, declared scopes, risk metadata, owning artifact/module
context: request/session correlation and only allowlisted, policy-safe tool arguments
```

1. Default deny for every non-public operation once production authentication is enabled.
2. Evaluate `tools/call` before execution, independently of client-controlled metadata.
3. Filter `tools/list` using the same call permission used for each tool; a caller sees only tools it could invoke at that point.
4. Evaluate registry and artifact administration against explicit permissions, not a role header.
5. Support policy based on principal scopes/roles, tool/function ID, declared tool scopes/risk, artifact ownership/trust, and selected safe arguments.
6. Log the decision identifier, subject identifier, action, target, outcome, and reason code. Redact credentials and configured sensitive arguments.
7. Return stable machine-readable `authentication_required`, `authorization_denied`, and `credential_authorization_required` outcomes. Design the exact MCP-compatible envelope before implementation.

## Outgoing Credential Strategies

Each tool/upstream binding must declare one strategy; there is no implicit credential forwarding.

| Strategy | Use when | Constraints |
|---|---|---|
| `none` | Backend is intentionally unauthenticated. | Explicit declaration required. |
| `secret-reference` | Backend uses a shared API key or service bearer token. | Resolve server-side from an approved secret provider; inject only into the backend request; never return it to the client or logs. |
| `header-passthrough` | Backend accepts the client's credential unchanged. | Explicit header allowlist and per-backend opt-in only; never forward host, hop-by-hop, forwarding, or arbitrary client headers. |
| `token-exchange` | Backend trusts Meshingress's identity provider but needs a narrower audience/scope. | Bind audience, subject, allowed scopes, expiry, and intended backend; prefer this over broad passthrough. |
| `delegated-oauth` | A SaaS backend requires a user's consented identity. | Broker login once, store encrypted refresh/access tokens per subject/provider/backend, refresh server-side, and permit revocation. |
| `workload-identity` | Cloud/Kubernetes backend issues workload-bound credentials. | Bind to Meshingress deployment identity and restrict the target resource. |

Credential precedence must be deterministic: a backend-specific strategy wins; global passthrough is only an explicit low-precedence fallback. A strategy selection failure must deny the call before the backend is contacted.

## Token and Secret Handling

- Do not put raw credentials in JSON-RPC parameters, tool schemas, annotations, tool results, audit events, exception messages, or architect records.
- Use a dedicated secret/token store with encryption at rest and defined key-management ownership before delegated OAuth is implemented.
- Keep issuer, provider, subject, client ID, audience, scopes, issued/expiry times, refresh state, and revocation state as durable metadata; do not use a single global credential store without a subject/provider binding.
- Limit decrypted secrets to outbound request construction and redact them before any logging path.
- Define refresh, retry, revocation, expiry, and user re-consent behavior without prompting the user on ordinary successful calls.

## Migration Requirements

1. Inventory existing client flows, WebSocket handshake behavior, role-gated RPC methods, tool dispatch, and every upstream integration.
2. Define profiles and an upgrade path from current development header/token behavior.
3. Add report-only/shadow authorization before a hard production default-deny rollout when transport-compatible.
4. Prove incoming authentication, per-tool discovery filtering, direct-call denial, outbound credential selection, redaction, expiry, and revocation with integration tests.
5. Do not claim artifact installation, availability policy, or administration is production-secure until it uses the new decision path.

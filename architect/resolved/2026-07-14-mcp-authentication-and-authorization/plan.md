# Design Plan

## Phase 1: Establish the Security Inventory

Map HTTP and WebSocket entry points, `McpCallContext`, admin RPC methods, tool dispatch, scope policy, secret resolution, artifact admission/install flow, and each upstream HTTP/process integration.

Deliverable: a trust-boundary and data-flow map that identifies where caller identity, authorization decisions, and backend credentials are absent, implicit, or header-based.

## Phase 2: Decide the Incoming Identity Contract

Choose the first production identity-provider path, normalized-principal model, token-validation runtime, OIDC discovery/JWKS lifecycle, audience convention, local-development exception, and WebSocket session binding.

Deliverable: a configuration contract and migration decision for the shared admin token and role headers.

## Phase 3: Decide the Authorization Model

Choose an application-native, Cedar/OPA-style, or external policy decision point. Define action/resource vocabulary, list filtering, cache/invalidation, reason codes, audit schema, and safe policy arguments.

Deliverable: policy examples for public, role-limited, scope-limited, administrative, and argument-constrained tools.

## Phase 4: Decide the Outgoing Credential Model

Classify every integration as `none`, `secret-reference`, `header-passthrough`, `token-exchange`, `delegated-oauth`, or `workload-identity`. Specify credential storage and lifecycle before adding delegated flows.

Deliverable: per-tool/backend credential-binding design, precedence, and revocation rules.

## Phase 5: Create an Implementation Package

Split approved work into small entries: incoming OIDC, authorization enforcement, secret/token store, outbound strategy adapters, migration/removal of development header authority, and end-to-end security tests.

Do not begin implementation from this pending record without user approval.

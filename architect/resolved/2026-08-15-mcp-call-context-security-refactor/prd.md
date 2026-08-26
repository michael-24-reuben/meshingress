# Product Requirements: MCP Call Context Security and Eligibility Refactor

## Goal

Provide a safe, typed context contract that reaches every MCP handler and
internal workflow tool invocation, while keeping authentication evidence in
server-only transport adapters. Use it to make list and call eligibility
consistent before integrating a real identity provider.

## Required Context Model

The public tool-facing context must contain typed, credential-safe values for:

- request IDs: stable MCP session ID, server correlation ID, unique invocation
  ID, and JSON-RPC call identity where applicable;
- verified principal: subject, issuer, authentication method, expiry, roles,
  scopes/grants, and optional profile/tenant identifiers;
- client metadata: name, version, and negotiated protocol version, marked as
  caller-declared rather than verified;
- lineage: parent invocation ID and optional workflow run/node/attempt data;
- execution control: deadline, cancellation signal, and progress capability.

The contract must not expose raw bearer values, cookies, upstream credentials,
legacy role hints, private keys, or arbitrary request headers.

## Eligibility Requirements

1. One `ToolAccessService` or equivalent evaluates tool visibility and
   executability from function metadata, verified principal, configured scope
   policy, and availability state.
2. `tools/list` and `tools/call` consume the same decision model. A principal
   must not receive a callable tool in list that it cannot execute under the
   same list-time inputs.
3. Role-authorized administration may inspect disabled/private registration
   state through its existing role routes, but such inspection never bypasses
   the execution decision.
4. Static availability conditions may participate in list-time decisions.
   Principal-aware conditions may participate when principal data is present.
   Argument-dependent conditions are call-time only and must deny safely before
   handler invocation when unmet.
5. Authorization/availability failures use stable non-secret error semantics
   and are safe to audit.

## Transport and Propagation Requirements

1. HTTP and WebSocket adapters create server-side transport requests, verify
   authentication evidence once, and then construct public call context.
2. Each JSON-RPC batch element and each WebSocket message obtains a unique
   invocation ID; the original correlation ID remains stable as lineage.
3. Workflow child calls derive context from their parent, preserving principal,
   session, root correlation, and explicit run/node/attempt lineage.
4. Timeouts must result in an execution deadline/cancellation signal visible
   to handlers; progress transport remains an execution capability, not an
   identity field.

## Non-Goals

- No OIDC provider, JWT parser, Spring Security filter chain, token issuer,
  Aegis persistence, bootstrap enrollment endpoint, or secret store in this
  entry.
- No public exposure of whether a non-authorized or unavailable private tool
  exists.
- No generic untyped map of incoming headers or claims in tool-facing context.

## Acceptance Criteria

- Tool handlers cannot access raw authorization or legacy role header values.
- A verified principal can make equivalent list/call eligibility decisions.
- An administrator can inspect disabled registration state but an attempted
  invocation remains denied.
- A non-administrator cannot discover a tool that policy marks invisible.
- Batch, WebSocket, and workflow calls retain correct distinct invocation and
  parent correlation IDs.
- Tests demonstrate a static availability decision, a principal-aware decision,
  and an argument-dependent call-time denial without invoking the tool body.

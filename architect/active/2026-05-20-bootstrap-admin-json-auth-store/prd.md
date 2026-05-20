# PRD: Bootstrap Admin And Store-Backed MCP Auth

## Problem

The current MCP WebSocket sample creates credentials in `temp/mcp-ws-demo/application-mcp-ws-demo.properties` and requires the server to be restarted with that file as additional Spring configuration. This makes auth setup fragile and backwards: the credential source is outside the application lifecycle, and a normal server start does not naturally support first-admin setup.

## Goals

- Start the server with zero preconfigured MCP auth tokens.
- Create a local JSON auth store on startup when one does not exist.
- Seed the JSON store with bootstrap admin details whose password is the Spring Security generated password.
- Allow first admin registration by confirming the stored bootstrap password.
- Let the first registration optionally replace the bootstrap password with a new password.
- Generate MCP auth credentials after first admin registration.
- Return generated MCP auth credentials once in the registration response.
- Store generated auth metadata and secret material in the JSON store for runtime validation.
- Design persistence through store registry interfaces so SQL can replace JSON later.
- Keep MCP header auth behavior: clients continue sending `Authorization`, `X-Secret-Key`, and `X-Auth-Token`.

## Non-Goals

- Do not keep using Spring property files as the source of MCP token truth.
- Do not require restarting the server to register or rotate initial MCP credentials.
- Do not expose generated secret material repeatedly after registration unless a separate recovery or rotation design is added.
- Do not implement SQL immediately unless this architect entry is explicitly moved into implementation and scoped to include it.

## Proposed User Flow

1. Server starts normally.
2. If `data/mcp-auth-store.json` does not exist, the app creates it.
3. The file contains bootstrap admin state, including the Spring Security generated password as the initial bootstrap password.
4. A user calls a first-admin registration endpoint with:
   - username
   - bootstrap/confirmation password
   - optional new password
   - optional confirmation for the new password
   - optional future-ready fields such as email
5. The server validates that no admin is already registered.
6. The server validates the bootstrap password from the JSON store.
7. The server creates the first admin record.
8. The server generates MCP auth material.
9. The server stores MCP auth data in the JSON store.
10. The server returns the generated MCP auth material once.

## Auth Behavior

Normal MCP connections should continue to function according to existing method-level authorization behavior. Public MCP calls can remain usable where they are already allowed. Admin-only MCP operations must continue to require admin authorization.

The WebSocket handshake should no longer validate against fixed Spring properties. It should validate credentials through the configured store registry implementation.

## Storage Model

The first implementation should use a JSON file store, tentatively:

```txt
data/mcp-auth-store.json
```

The name can be changed during implementation if a better repo convention emerges.

The store should be structured so it can represent:

- bootstrap state
- admin users
- MCP auth credentials
- credential metadata
- created/updated timestamps
- one-time display or recovery state
- future identity fields such as email

Secret values should not be logged. Passwords should be stored as encoded hashes where practical. If raw MCP token material must be stored for validation in the first JSON implementation, the implementation should document the risk and keep the storage interface ready for stronger hashing or SQL-backed secret handling.

## Store Registry Contract

Introduce interfaces around auth persistence instead of coupling controllers, validators, or WebSocket middleware to JSON files directly.

Expected responsibilities:

- get bootstrap state
- initialize bootstrap state when missing
- look up admin by username
- determine whether a first admin exists
- create first admin
- update admin password
- store MCP auth credentials
- look up MCP auth credentials by client id or token material
- validate MCP credential triples
- list non-secret credential metadata for admin views
- support future SQL implementation behind the same contract

## Lifecycle Requirement

When implementation starts, this architect entry should remain active until SQL persistence is implemented, unless the project explicitly creates a linked SQL migration entry and updates this record with that split.

## Acceptance Criteria

- Server can start without `application-mcp-ws-demo.properties`.
- Missing JSON store is created automatically.
- First admin registration succeeds only with the stored bootstrap password.
- Optional new password flow validates confirmation.
- First admin registration returns generated MCP auth material once.
- MCP WebSocket credential validation uses the store registry instead of Spring property values.
- Existing public and role-gated MCP behavior remains intact.
- Tests cover bootstrap creation, first admin registration, duplicate registration rejection, generated credential storage, and WebSocket auth validation through the store.

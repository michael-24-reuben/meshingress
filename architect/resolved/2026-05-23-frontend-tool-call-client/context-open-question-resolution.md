## Resolved Design Questions

### Should WebSocket support be implemented in MVP or only designed as a future transport?

Only design WebSocket support as a future transport.

MVP should implement HTTP JSON-RPC tool calls only. The frontend architecture should define a transport boundary so WebSocket can be added later without rewriting form generation, response rendering, settings, or error handling.

Implementation note:

```js
// #architect: MVP uses HTTP JSON-RPC only. WebSocket support is intentionally deferred,
// but the transport boundary should remain replaceable so a WebSocket transport can be added later.
````

### Should settings persist in local storage, session storage, or memory only?

Settings persist in memory for now.

Do not use `localStorage` or `sessionStorage` in MVP. Runtime settings such as MCP endpoint URL, refresh interval, and “do not ask again” risk prompts should reset on page reload.

Implementation note:

```js
// #architect: Settings are memory-only in MVP. Future transport/settings work should design
// localStorage persistence after the security and auth model is finalized.
```

### Should auth token storage be disabled until authentication is implemented?

Yes.

Authentication should remain an empty shell. Do not store tokens. Do not persist auth credentials. Do not expose token fields in durable browser storage.

A settings/auth controller or config object may exist, but it should not perform real auth work yet.

Implementation note:

```js
// #architect: Authentication is intentionally a no-op shell in MVP.
// Do not store auth tokens until Meshingress authentication is implemented.
```

For a Spring controller:

```java
// #architect: Authentication integration is intentionally empty.
// This controller only exposes the static client page until Meshingress auth is implemented.
```

### Should the UI prevent calls to high-risk scopes without an explicit confirmation?

No hard prevention.

The UI should show a popup warning before calling a high-risk tool, but it should allow the user to proceed after confirmation.

Behavior:

* Detect high-risk scopes from tool annotations.
* Show warning before submit.
* Include **Proceed**, **Cancel**, and **Do not ask again**.
* Save “Do not ask again” in memory only.
* Reset warning suppression on page reload.

Implementation note:

```js
// #architect: High-risk scope confirmation is advisory in MVP, not a hard policy gate.
// "Do not ask again" is memory-only and resets on reload.
```

High-risk examples to treat as warning-worthy:

```txt
SHELL_EXECUTE
FILES_WRITE
FILES_DELETE
NETWORK_ACCESS
HTTP_CLIENT
WEBSOCKET_CONNECT
EXTERNAL_API_WRITE
PROCESS_EXECUTE
PROCESS_KILL
SECRETS_READ
TOKEN_ISSUE
TOKEN_REVOKE
POLICY_WRITE
TOOLS_CALL
```

### Should the module be plain static assets or integrated into an existing frontend build system?

Leave it as **plain static assets** for MVP.

Use vanilla HTML, CSS, and JavaScript served by Spring. Avoid introducing Node.js, Vite, npm scripts, bundler config, or a frontend framework until the client grows beyond the static-module boundary.

Implementation note:

```js
// #architect: MVP is plain static assets served by Spring.
// Do not introduce Vite, Node.js, or a bundler unless future UI complexity requires it.
```

## Final module decision

Use:

```txt
frontend/tool-call-client
```

Recommended MVP stance:

```txt
frontend/tool-call-client is a plain static Spring-served frontend module for HTTP JSON-RPC tool calls.
It is not a Node/Vite app in MVP.
It should keep transport, settings, form generation, response rendering, and error handling modular enough for later WebSocket/auth/persistence work.
```

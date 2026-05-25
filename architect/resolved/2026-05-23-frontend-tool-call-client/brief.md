# Frontend Tool Call Client

## Original Request

Create a clean, modern frontend webpage for making Meshingress/MCP HTTP tool calls.

The module should live under:

```txt
frontend/tool-call-client
```

This is a frontend-side module, distinct from backend `lib/` and backend tool implementation modules. Its purpose is to provide a browser-based UI for discovering available tools, generating request forms from schemas, submitting JSON-RPC requests to the MCP endpoint, and rendering responses in readable and inspectable formats.

## Problem Statement

Meshingress exposes tools through an MCP-compatible JSON-RPC interface, but there needs to be a dedicated frontend module for manually calling tools during development, debugging, and operational use.

The frontend should avoid hard-coded tool forms. Instead, it should treat `tools/list` as the method of truth, fetch available tools and input schemas from the MCP endpoint, then dynamically generate form controls for the selected tool.

## Expected Behavior

On page load, the client should:

1. Load user-configurable settings such as MCP endpoint URL and refresh interval.
2. Fetch `tools/list` from the configured MCP HTTP endpoint.
3. Populate a tool selector with available tool names, titles, descriptions, schemas, and annotations.
4. Generate input controls from the selected tool's `inputSchema`.
5. Submit JSON-RPC requests to the endpoint when the form is submitted.
6. Display JSON-RPC `result` responses in multiple user-toggleable views.
7. Display JSON-RPC `error` responses clearly with code, message, and optional data.
8. Keep a timestamped scrollable history of responses.
9. Support immediate settings changes without a full page reload.

## Scope

In scope:

- Static HTML/CSS/JavaScript implementation.
- Dynamic tool list fetching through `tools/list`.
- Schema-driven form generation for common JSON Schema object properties.
- JSON-RPC request construction.
- HTTP POST transport.
- Future-ready WebSocket transport boundary.
- Response rendering for `content`, `structuredContent`, `_meta`, and JSON-RPC errors.
- Secure MIME rendering with sanitization for HTML content.
- Settings page or modal for endpoint URL and refresh interval.

Out of scope for the initial MVP:

- Full JSON Schema renderer parity for all schema keywords.
- Authentication UI beyond optional token/session settings hooks.
- Server-side proxying.
- Persistent encrypted secret storage.
- Destructive tool approval workflows, unless the backend already enforces them.

## Proposed Module Name

Use:

```txt
frontend/tool-call-client
```

Rationale: the name is frontend-specific, directly describes the module's job, and avoids confusion with backend `toolspace/` modules that implement tools rather than call them.

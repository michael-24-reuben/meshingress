# Implementation Plan

## Module Path

```txt
frontend/tool-call-client
```

## Proposed File Layout

```txt
frontend/tool-call-client/
├─ index.html
├─ settings.html
├─ css/
│  ├─ colors.css
│  └─ styles.css
├─ js/
│  ├─ settings.js
│  ├─ script.js
│  ├─ utils.js
│  ├─ errorHandler.js
│  ├─ responseRenderer.js
│  ├─ toolFormGenerator.js
│  └─ toolListFetcher.js
├─ components/
│  ├─ response-card.html
│  ├─ error-card.html
│  ├─ tool-select.html
│  └─ settings-panel.html
└─ mimes/
   ├─ json.html
   ├─ text.html
   ├─ html.html
   ├─ image.html
   └─ audio.html
```

The `components/` and `mimes/` templates can start as optional placeholders if the initial implementation renders directly from JavaScript. Keep the boundaries documented so the module can evolve without a rewrite.

## Responsibility Boundaries

### `index.html`

- Owns page shell and static placeholders.
- Includes the form container, tool selector placeholder, generated fields container, submit button, response console, and settings entry point.
- Should not contain core business logic.

### `settings.html`

- Provides a page or modal content fragment for editing:
  - MCP endpoint URL
  - tool refresh interval
  - optional bearer token
  - optional session ID
- Settings updates should notify the active page script and apply immediately.

### `css/colors.css`

Defines reusable CSS custom properties for the theme:

```css
:root {
  --color-primary: #4A90E2;
  --color-secondary: #50E3C2;
  --color-background: #F5F7FA;
  --color-text: #333333;
}
```

### `css/styles.css`

- Owns layout, spacing, typography, cards, buttons, forms, response console, settings panel, and animation polish.
- Imports or depends on `colors.css`.

### `js/settings.js`

- Owns default constants and mutable runtime settings.
- Recommended defaults:
  - endpoint: `http://100.121.15.11:4737/mcp`
  - tool refresh interval: `30000`
  - auth token: empty
  - session ID: empty
- Provides load/save/update APIs.
- Emits or calls hooks when settings change.

### `js/toolListFetcher.js`

- Builds and sends the `tools/list` JSON-RPC request.
- Parses `result.tools`.
- Updates the tool selector.
- Handles refresh timers.
- Should expose a manual refresh function.

### `js/toolFormGenerator.js`

- Converts selected tool `inputSchema` into HTML form controls.
- Handles required fields.
- Converts submitted field values back to JSON params.
- Falls back to raw JSON editor for unsupported schema shapes.

### `js/responseRenderer.js`

- Renders `result.content`, `result.structuredContent`, `_meta`, and raw JSON.
- Dispatches content entries based on `type` and `mimeType`.
- Sanitizes HTML before rendering.
- Keeps MIME rendering extensible.

### `js/errorHandler.js`

- Maps known JSON-RPC error codes to names.
- Builds readable error card data.
- Handles network errors, parse failures, missing result/error fields, and server error responses.

### `js/utils.js`

- Generates unique JSON-RPC IDs.
- Pretty-prints JSON.
- Sanitizes strings and HTML.
- Builds safe DOM nodes.
- Parses JSON from textarea fields.
- Formats timestamps.

### `js/script.js`

- Main coordinator.
- Initializes settings, fetches tools, binds events, handles form submission, and appends response entries.
- Delegates specialized work to other modules.

## Data Flow

1. `script.js` loads settings from `settings.js`.
2. `toolListFetcher.js` submits `tools/list` to the configured endpoint.
3. Tool selector is populated from `result.tools`.
4. User selects a tool.
5. `toolFormGenerator.js` generates parameter fields from `inputSchema`.
6. User submits form.
7. `script.js` builds JSON-RPC payload with selected method and generated params.
8. Fetch API sends POST request to MCP endpoint.
9. Response is parsed.
10. `responseRenderer.js` renders success responses.
11. `errorHandler.js` renders JSON-RPC and network errors.
12. Response card is appended to the console/history.

## JSON-RPC Request Shape

```json
{
  "jsonrpc": "2.0",
  "id": "generated-id",
  "method": "tools/list",
  "params": {}
}
```

For tool invocation, `method` should be the selected tool name and `params` should be assembled from generated inputs.

## Security Plan

- Never inject raw HTML directly into `innerHTML` unless sanitized first.
- Prefer text nodes for text output.
- Restrict HTML rendering to a sandboxed/sanitized preview.
- Treat endpoint URL, auth token, session ID, and response payloads as untrusted.
- Avoid persistent storage of auth tokens for the first implementation unless explicitly approved.
- Show annotations/scopes near the selected tool so risky tools are visible before submission.

## Implementation Order

1. Create static HTML shell and CSS theme.
2. Implement settings defaults and runtime update behavior.
3. Implement JSON-RPC ID generation and POST helper.
4. Implement `tools/list` fetch and selector population.
5. Implement basic schema-driven form generation.
6. Implement submit handler and request construction.
7. Implement response history cards.
8. Implement structured JSON and text rendering.
9. Implement JSON-RPC error handling.
10. Add MIME renderer dispatch and safe HTML handling.
11. Add settings page/modal behavior.
12. Add polish: transitions, loading states, empty states, and manual refresh.

## Risks

- MCP endpoint may not support browser CORS yet.
- JSON Schema forms can become complex quickly; MVP should support common cases first and provide raw JSON fallback.
- HTML MIME rendering can introduce XSS if not sanitized correctly.
- WebSocket support requires a separate transport abstraction and should not block HTTP MVP.
- Tool calls can be high-risk depending on backend scopes and authorization policy; the UI should surface tool annotations/scopes clearly.

# Context

## Module Decision

The frontend module should be named:

```txt
frontend/tool-call-client
```

This module is not a backend tool implementation module. Backend tool modules live under `toolspace/<module-name>/`, attach to the server, and expose tools through annotations or SPI. This frontend module is instead a client UI for discovering and invoking tools.

## Existing Backend Concepts Relevant to the Frontend

### Tool Listing

The frontend should call `tools/list` and trust the returned `result.tools` array as the source of truth for:

- tool name
- title
- description
- input schema
- annotations
- scopes/hints

The sample `tools/list` response includes tools such as:

- `architect.entries.list`
- `cli.powershell`
- `helloworld.greet`
- `instagram.fetch`

Each tool includes an `inputSchema` object that can drive form generation.

### Tool Result Shape

Successful calls generally return JSON-RPC responses with:

```json
{
  "jsonrpc": "2.0",
  "id": "request-id",
  "result": {
    "content": [],
    "structuredContent": {},
    "_meta": {}
  }
}
```

The backend result model supports:

- `content`: list of displayable result entries
- `structuredContent`: optional structured JSON result
- `isError`: tool-level error marker in result payloads
- `_meta`: generated metadata such as status, summary, error information, and generated timestamp

The frontend should therefore treat JSON-RPC `error` and tool-level `result.isError` as related but distinct states.

### Content Entry Shape

Content entries can be:

```json
{
  "type": "text",
  "text": "..."
}
```

or:

```json
{
  "type": "json",
  "mimeType": "application/json",
  "data": {}
}
```

or another MIME-backed content entry:

```json
{
  "type": "mime",
  "mimeType": "...",
  "data": {}
}
```

The frontend response renderer should use both `type` and `mimeType` as rendering hints.

## MCP HTTP Endpoint

Initial endpoint from user-provided request example:

```txt
http://100.121.15.11:4737/mcp
```

Example HTTP request shape:

```http
POST /mcp HTTP/1.1
Host: 100.121.15.11:4737
User-Agent: meshingress-client/1.0
Accept: application/json
Content-Type: application/json
X-Request-Id: GENERATED_ID
Authorization: Bearer <token>
X-Mcp-Session-Id: <session-id>
```

Only `Accept` and `Content-Type` are required for the MVP. Auth and session headers should be optional settings.

## JSON-RPC Errors

Known JSON-RPC/application error codes:

```txt
PARSE_ERROR = -32700
INVALID_REQUEST = -32600
METHOD_NOT_FOUND = -32601
INVALID_PARAMS = -32602
INTERNAL_ERROR = -32603
UNAUTHORIZED = -32001
FORBIDDEN = -32003
```

The UI should map these codes to names and show both the numeric code and readable label.

## Schema Generation Notes

The MVP schema renderer should focus on object schemas with direct `properties` and `required` arrays. This covers the known sample tools.

Known sample schema cases:

- `helloworld.greet`: required string field `name`.
- `instagram.fetch`: required string field `url`.
- `architect.entries.list`: optional strings and enum status.
- `cli.powershell`: required string `script`, optional strings, optional integer, optional array, optional object, optional boolean.

For arrays and objects, use JSON textarea controls first. More advanced nested editing can be added later.


## Agent Review Targets

Before implementing or modifying `frontend/tool-call-client`, the agent should review these project files for broader protocol, safety, and rendering context. These are review targets, not files this frontend module necessarily edits.

### Result and content contracts

- `lib/meshingress-tool-api/src/main/java/dev/mrk/meshingress/api/result/DispatchExecutionResult.java`
  - Review the exact response payload emitted by tool handlers.
  - Confirm handling for `content`, `structuredContent`, `isError`, and `_meta` fields.
  - Use this to keep `responseRenderer.js` and `errorHandler.js` compatible with backend output.

- `lib/meshingress-tool-api/src/main/java/dev/mrk/meshingress/api/result/ResultContent.java`
  - Review supported content entry types: `text`, `json`, and `mime`.
  - Confirm field names for text content versus MIME/JSON data content.
  - Use this to avoid hard-coding incorrect frontend assumptions about `text`, `data`, `type`, or `mimeType`.

### Tool schema and declaration context

- `lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/McpTool.java`
  - Review tool ID and invocation-name patterns.
  - Use this to validate, display, or sort tool names returned by `tools/list`.

- `lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/McpFunction.java`
  - Review function naming constraints.
  - Use this when deciding whether the frontend should expose function-level method names directly or rely only on complete names returned by `tools/list`.

- `lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/McpInputField.java`
  - Review how backend input metadata maps to generated input schemas.
  - Use this as background for `toolFormGenerator.js`, even though the frontend should consume the JSON schema returned by `tools/list`, not Java annotations directly.

- `lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/McpInputSchema.java`
  - Review custom schema provider support.
  - Use this to avoid assuming every schema is annotation-derived or flat.

### Risk, scopes, and safety presentation

- `lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/scopes/McpToolScope.java`
  - Review risk level, privileged status, audit recommendation, and helper semantics such as explicit approval and audit requirement.
  - Use this to design visible warnings for high-risk scopes such as `SHELL_EXECUTE`, `FILES_WRITE`, `FILES_DELETE`, `HTTP_CLIENT`, `WEBSOCKET_CONNECT`, and broad `NETWORK_ACCESS`.

- `lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/scopes/ScopeMetadata.java`
  - Review the structured metadata available for each scope.
  - Use this as the conceptual model for frontend badges, severity labels, and tool warning copy.

- `lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/scopes/RiskLevel.java`
  - Review available risk levels.
  - Use this to keep frontend severity labels aligned with backend categories.

- `lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/scopes/AccessMode.java`
  - Review access modes such as `READ`, `WRITE`, `DELETE`, `EXECUTE`, `ADMIN`, and `MANAGE`.
  - Use this to distinguish passive tools from mutating or execution-oriented tools.

- `lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/scopes/ScopeCategory.java`
  - Review scope categories such as `NETWORK`, `FILES`, `PROCESS`, `SECRETS`, `TOOLS`, and `AUDIT`.
  - Use this to group scope badges in the UI.

### Dispatch, security, audit, and runtime configuration

- `lib/meshingress-config/src/main/java/dev/mrk/meshingress/config/MeshingressProperties.java`
  - Review the typed configuration surface for MCP transport, tools registry behavior, dispatch limits, security policy, scope enforcement, audit, and secrets.
  - Use this to keep settings names and assumptions aligned with backend configuration.

- `architect/pending-or-active/meshingress-properties-todo.md` or the current properties TODO entry if moved
  - Review planned defaults for MCP endpoint, dispatch timeout, max concurrent calls, security posture, audit behavior, and secret policy.
  - Use this to decide which frontend settings should be exposed now versus deferred.

### Tool module behavior examples

- `toolspace/*` module examples, especially `toolspace/helloworld`, `toolspace/powershell-cli-tool`, and `toolspace/instagram-api` if present in the working tree
  - Review real tool names, schemas, annotations, result payloads, and risky scopes.
  - Use these as manual fixtures for `tools/list`, dynamic form generation, response rendering, and high-risk warning states.

- `tool-module-README.md`
  - Review the expected lifecycle of server-attached tool modules, required dependencies, result examples, scopes, and security checklist.
  - Use this only as backend context; do not place this frontend module under `toolspace/`.

### Frontend implementation files to create or review

- `frontend/tool-call-client/index.html`
  - Main page shell, form region, settings link/modal mount, and response console container.

- `frontend/tool-call-client/styles.css`
  - Layout, cards, form controls, buttons, response console, animations, and risk states.

- `frontend/tool-call-client/colors.css`
  - Theme variables for primary, secondary, background, text, success, warning, and error colors.

- `frontend/tool-call-client/settings.js`
  - MCP endpoint URL, refresh interval, optional auth/session header configuration, and runtime settings state.

- `frontend/tool-call-client/settings.html`
  - User-editable settings view or modal content.

- `frontend/tool-call-client/script.js`
  - Page bootstrap and event wiring.

- `frontend/tool-call-client/toolListFetcher.js`
  - `tools/list` JSON-RPC fetch, refresh timer, dropdown population, and stale/error states.

- `frontend/tool-call-client/toolFormGenerator.js`
  - Dynamic form generation from JSON schema.

- `frontend/tool-call-client/responseRenderer.js`
  - Rendering for text, JSON, structured content, MIME entries, and raw debug views.

- `frontend/tool-call-client/errorHandler.js`
  - JSON-RPC error mapping, tool-level `isError` handling, and user-facing error cards.

- `frontend/tool-call-client/utils.js`
  - Request ID generation, JSON parsing/formatting, DOM-safe rendering helpers, and HTML sanitization wrapper.

- `frontend/tool-call-client/mimes/*.html`
  - Optional templates for specialized MIME renderers.

- `frontend/tool-call-client/components/*.html`
  - Optional reusable component templates for tool selectors, response cards, settings controls, and warnings.

### Review rule

If a behavior depends on backend contracts, prefer reviewing the backend contract file first rather than inferring behavior from the sample response alone. The sample response is useful for UI fixtures, but the source/result classes and scope metadata are the durable contract references.

## UX Notes

- Show risky annotations/scopes near the selected tool before submission.
- Make `cli.powershell` appear visibly high-risk if scopes are available in annotations.
- Preserve previous responses so users can compare outputs.
- Add timestamps to every response entry.
- Use empty/loading/error states rather than silent failures.

## Security Notes

- Treat every response as untrusted input.
- Text should render through text nodes.
- JSON should be stringified and inserted as text.
- HTML MIME rendering must sanitize content first.
- Avoid storing bearer tokens persistently unless later approved.
- Surface destructive/high-risk tool hints when annotations provide them.

## Open Questions

- Should WebSocket support be implemented in MVP or only designed as a future transport?
- Should settings persist in local storage, session storage, or memory only?
- Should auth token storage be disabled until authentication is implemented?
- Should the UI prevent calls to high-risk scopes without an explicit confirmation?
- Should the module be plain static assets or integrated into an existing frontend build system?

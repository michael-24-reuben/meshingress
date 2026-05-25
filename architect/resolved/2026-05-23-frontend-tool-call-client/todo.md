# Todo

## Setup

- [x] Create `frontend/tool-call-client/` module directory.
- [x] Add `index.html` page shell.
- [x] Add `settings.html` page or modal fragment.
- [x] Add `css/colors.css` with theme variables.
- [x] Add `css/styles.css` for layout, forms, response cards, and settings UI.

## Settings

- [x] Add `js/settings.js` with default MCP endpoint URL.
- [x] Add configurable tool refresh interval.
- [x] Add optional bearer token setting placeholder.
- [x] Add optional MCP session ID setting placeholder.
- [x] Apply settings updates without full page reload.
- [x] Re-fetch tool list when endpoint changes.
- [x] Restart refresh timer when interval changes.

## Tool Discovery

- [x] Add `js/toolListFetcher.js`.
- [x] Build `tools/list` JSON-RPC request.
- [x] Send POST request to MCP endpoint.
- [x] Parse `result.tools`.
- [x] Populate tool selector with name/title/description.
- [x] Show selected tool schema and annotations.
- [x] Add manual refresh control.
- [x] Add automatic refresh timer.

## Dynamic Form Generation

- [x] Add `js/toolFormGenerator.js`.
- [x] Generate string inputs.
- [x] Generate number/integer inputs.
- [x] Generate boolean controls.
- [x] Generate enum dropdowns.
- [x] Generate array/object JSON textareas.
- [x] Respect `required` fields.
- [x] Show schema descriptions as helper text.
- [x] Add raw JSON fallback for unsupported schema features.
- [x] Convert field values into JSON params on submit.

## Request Submission

- [x] Add JSON-RPC ID generation in `js/utils.js`.
- [x] Build JSON-RPC payload with selected tool name and arguments through `tools/call`.
- [x] Include optional `X-Request-Id` header.
- [x] Include optional `Authorization` header when configured.
- [x] Include optional `X-Mcp-Session-Id` header when configured.
- [x] Show loading/submitting state.
- [x] Prevent duplicate submits while a request is in flight unless explicitly allowed.

## Response Rendering

- [x] Add `js/responseRenderer.js`.
- [x] Render `result.content` text entries.
- [x] Render `result.content` JSON entries.
- [x] Render `result.structuredContent` as formatted JSON.
- [x] Render `_meta` as optional details.
- [x] Add raw JSON view.
- [x] Add toggle controls for rendered/structured/raw views.
- [x] Keep timestamped response history.
- [x] Add scrollable console area.
- [x] Add clear-history action.

## MIME Rendering

- [x] Add MIME renderer dispatch by `type` and `mimeType`.
- [x] Add safe text renderer.
- [x] Add JSON renderer.
- [x] Add sanitized HTML renderer.
- [x] Add image renderer when safe source data is available.
- [x] Add audio renderer when safe source data is available.
- [x] Document extension point for additional MIME renderers.

## Error Handling

- [x] Add `js/errorHandler.js`.
- [x] Map known JSON-RPC error codes to symbolic names.
- [x] Render JSON-RPC `error.code` and `error.message` clearly.
- [x] Render optional `error.data` as formatted JSON.
- [x] Handle network errors.
- [x] Handle invalid JSON responses.
- [x] Handle malformed success responses with no `result` or `error`.

## Verification

- [x] Verify page loads through a static HTTP server.
- [x] Verify JavaScript files pass syntax checks.
- [x] Verify frontend modules import successfully where they do not require a browser DOM.
- [x] Verify submit payload helper matches Meshingress `tools/call` JSON-RPC 2.0 shape.
- [x] Verify headless Chrome renders the shell, settings controls, inspector, and response history.
- [ ] Verify live `tools/list`, generated forms, and response rendering against a reachable Meshingress endpoint.

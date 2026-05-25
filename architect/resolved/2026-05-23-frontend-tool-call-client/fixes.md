# Fixes

## Files Added

- `frontend/tool-call-client/index.html`
- `frontend/tool-call-client/settings.html`
- `frontend/tool-call-client/css/colors.css`
- `frontend/tool-call-client/css/styles.css`
- `frontend/tool-call-client/js/settings.js`
- `frontend/tool-call-client/js/script.js`
- `frontend/tool-call-client/js/utils.js`
- `frontend/tool-call-client/js/errorHandler.js`
- `frontend/tool-call-client/js/responseRenderer.js`
- `frontend/tool-call-client/js/toolFormGenerator.js`
- `frontend/tool-call-client/js/toolListFetcher.js`
- `frontend/tool-call-client/components/README.md`
- `frontend/tool-call-client/mimes/README.md`

## Behavior Added

- Loads `tools/list` from the configured MCP endpoint.
- Populates a tool selector from `result.tools`.
- Generates controls for string, integer, number, boolean, enum, array, and object schema properties.
- Builds `tools/call` payloads with generated request IDs.
- Sends optional `X-Request-Id`, `Authorization`, and `X-Mcp-Session-Id` headers.
- Renders text, JSON, structured content, raw responses, `_meta`, JSON-RPC errors, tool-level error results, and MIME-backed content.
- Sanitizes HTML MIME previews before rendering them in a sandboxed iframe.
- Shows high-risk scope warnings before submission, with memory-only suppression.
- Provides memory-only settings for endpoint, refresh interval, bearer token, and session ID.

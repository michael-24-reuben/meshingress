# PRD: Frontend Tool Call Client

## Goal

Build a browser-based client for invoking Meshingress tools over HTTP JSON-RPC, with a clean UI, dynamic schema-driven forms, configurable endpoint settings, and safe response rendering.

## User Experience Requirements

### Theme

Use a clean modern interface with these colors:

```css
--color-primary: #4A90E2;
--color-secondary: #50E3C2;
--color-background: #F5F7FA;
--color-text: #333333;
```

### Layout

- Place the tool call form prominently near the top of the page.
- Use clear labels for method/tool selection and generated parameters.
- Place the submit button near the form and make it visually prominent.
- Put response output below the form.
- Maintain a scrollable, timestamped response history.
- Use subtle transitions for response insertion, view toggles, hover states, and settings updates.

## Functional Requirements

### Tool Discovery

- On page load, submit a JSON-RPC request to the configured endpoint with method `tools/list` and empty params.
- Use the returned `result.tools` array as the authoritative source for available tools.
- Populate a dropdown or selection control with tool names, titles, and descriptions.
- Refresh the tool list every configured interval.
- Allow refresh interval changes to apply immediately.

### Dynamic Form Generation

- When a tool is selected, inspect its `inputSchema`.
- Generate controls for object properties.
- Support at minimum:
  - `string`
  - `integer`
  - `number`
  - `boolean`
  - `array`
  - `object`
  - `enum`
- Respect `required` properties.
- Show property descriptions as helper text.
- For unsupported schema structures, provide a raw JSON editor fallback.

### Request Submission

Each submitted call should produce a JSON-RPC 2.0 POST payload:

```json
{
  "jsonrpc": "2.0",
  "id": "GENERATED_UNIQUE_JSON_RPC_ID",
  "method": "SELECTED_TOOL_NAME",
  "params": {}
}
```

The client may include optional headers:

- `X-Request-Id`
- `Authorization: Bearer <token>`
- `X-Mcp-Session-Id`

These should be configurable but not required for MVP.

### Response Rendering

Render successful responses with toggleable views:

1. Rendered content view from `result.content`.
2. Structured content JSON view from `result.structuredContent`.
3. Raw JSON response view.
4. Error/debug metadata view when `_meta` is present.

For `content` entries:

- `type: text` should render as readable text.
- `type: json` or `mimeType: application/json` should render as formatted JSON.
- Supported MIME content should render in a safe preview when possible.
- HTML content must be sanitized before insertion into the DOM.
- Images may render through `img` if the source is safe and representable.
- Audio may render through `audio` if the source is safe and representable.

### Error Handling

If the response contains `error`, show a clear error card with:

- code
- symbolic name if known
- message
- data, if present

Known error codes:

```txt
PARSE_ERROR = -32700
INVALID_REQUEST = -32600
METHOD_NOT_FOUND = -32601
INVALID_PARAMS = -32602
INTERNAL_ERROR = -32603
UNAUTHORIZED = -32001
FORBIDDEN = -32003
```

Use clear visual treatment, such as a red-tinted background and error icon/label.

## Non-Goals

- Do not implement the backend MCP endpoint.
- Do not implement backend tool discovery.
- Do not hard-code tool-specific forms.
- Do not execute arbitrary returned HTML without sanitization.
- Do not store auth tokens in plaintext local storage unless explicitly accepted later.

## Acceptance Criteria

- The page loads with the configured default endpoint.
- `tools/list` is called on load.
- Returned tools populate the selector.
- Selecting a tool generates parameter fields from `inputSchema`.
- Submitting the form sends a JSON-RPC POST with selected method and params.
- Successful responses show content and structuredContent when present.
- Error responses show code, message, and data.
- Response history supports multiple entries with timestamps.
- Endpoint URL and refresh interval can be changed without page reload.
- CSS uses shared color variables from `colors.css`.
- JavaScript is split by concern, not placed entirely in `index.html`.

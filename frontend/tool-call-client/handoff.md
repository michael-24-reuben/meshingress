# Frontend Function Integration Handoff

## Scope

This handoff documents how the frontend integrates with Meshingress backend tool execution. It focuses on callable functions, data contracts, request/response shape, and the reusable implementation blueprint needed to connect backend scripts or tool modules to the frontend.

Out of scope: CSS, visual themes, colors, page layout styling, and any presentation-only decisions.

---

## System Boundary

The frontend is a static browser client that talks to a Meshingress MCP endpoint over HTTP POST using JSON-RPC 2.0 payloads.

The two backend-facing JSON-RPC methods used by the frontend are:

| Method | Caller | Purpose | Expected response |
|---|---|---|---|
| `tools/list` | `fetchToolList(settings)` | Discover available tool descriptors from the configured MCP endpoint. | JSON-RPC response with `result.tools` array. |
| `tools/call` | `submitToolCall()` via `buildCallPayload()` | Invoke the selected tool with collected arguments. | JSON-RPC response with `result.content`, optional `result.structuredContent`, optional `result._meta`, and optional `result.isError`. |

The backend must expose tool metadata compatible with the frontend form generator:

```json
{
  "name": "tool.name",
  "title": "Optional Human Title",
  "description": "Optional description",
  "inputSchema": {
    "type": "object",
    "properties": {
      "fieldName": {
        "type": "string",
        "description": "Field description"
      }
    },
    "required": ["fieldName"]
  },
  "annotations": {
    "scopes": ["FILES_READ"]
  }
}
```

---

## End-to-End Interaction Flow

### 1. Page bootstraps

`bootstrap()` runs immediately on module load. It:

1. Loads the settings form template with `loadSettingsTemplate()`.
2. Registers UI event handlers with `bindEvents()`.
3. Writes current settings into the settings form with `renderSettings(getSettings())`.
4. Displays the active endpoint with `updateEndpointLabels()`.
5. Starts the periodic tool refresh timer via `refreshController.start(getSettings().refreshIntervalMs)`.
6. Calls `refreshTools()` to load initial tool metadata.

### 2. Frontend discovers tools

`refreshTools()` calls `fetchToolList(getSettings())`.

`fetchToolList(settings)` sends:

```json
{
  "jsonrpc": "2.0",
  "id": "tools-list-<generated>",
  "method": "tools/list",
  "params": {}
}
```

On success, the function returns:

```ts
{
  tools: ToolDescriptor[],
  raw: JsonRpcResponse,
  fetchedAt: Date
}
```

The frontend sorts tools by `name`, stores them in `state.tools`, renders the `<select>`, and selects the first tool by default.

### 3. Frontend renders selected tool

`selectTool(toolName)` finds the descriptor in `state.tools`, stores it in `state.selectedTool`, enables or disables the submit button, then calls:

- `renderSelectedTool()`
- `updatePayloadPreview()`

`renderSelectedTool()`:

- Shows the tool title/name.
- Shows the tool description.
- Reads scopes from `tool.annotations.scopes`.
- Marks configured high-risk scopes for advisory confirmation.
- Calls `renderToolForm(elements.formFields, tool)` to generate argument inputs from `tool.inputSchema`.
- Writes raw schema and annotations into inspector views.

### 4. Frontend collects arguments

When the form changes, `updatePayloadPreview()` calls `buildCurrentPayloadPreview()`.

`buildCurrentPayloadPreview()` uses:

- `collectFormArguments(elements.toolCallForm, state.selectedTool, { validateRequired: false })`
- `buildCallPayload(state.selectedTool, args, "GENERATED_ID")`

When the user submits, `submitToolCall()` uses the same functions but with a real generated request ID and required-field validation enabled.

### 5. Frontend invokes backend tool

`submitToolCall()` builds and sends:

```json
{
  "jsonrpc": "2.0",
  "id": "tool-call-<generated>",
  "method": "tools/call",
  "params": {
    "name": "selected.tool.name",
    "arguments": {
      "fieldName": "value"
    }
  }
}
```

The HTTP target is `settings.endpointUrl`. Headers come from `buildRequestHeaders(settings, requestId)`.

### 6. Frontend renders response

`submitToolCall()` parses the response and calls `assertValidJsonRpcResponse(body)`.

Then it branches:

- `body.error` exists: call `appendResponse(responseHistory, normalizeJsonRpcError(body, payload))`.
- no `body.error`: call `appendResponse(responseHistory, { at, toolName, request, response })`.
- thrown fetch, parse, validation, or client error: call `appendResponse(responseHistory, normalizeClientError(error, buildCurrentPayloadPreview()))`.

`appendResponse()` prepends a rendered response card using `renderResponseCard(entry)`.

---

## Frontend Function Reference

### `script.js`: Interaction Orchestrator

#### `bootstrap()`

**Called by:** module load.

**Does:** Initializes the frontend lifecycle: settings template, events, settings form state, endpoint label, refresh timer, and initial tool list fetch.

**Returns:** `Promise<void>`.

**Frontend integration role:** This is the entry point. Any frontend implementation should preserve this order: mount settings first, bind events, render settings, start refresh, fetch tools.

---

#### `bindEvents()`

**Called by:** `bootstrap()`.

**Does:** Registers all user interaction handlers:

- Tool selection change.
- Tool call form input and submit.
- Reset form.
- Manual refresh.
- Clear response history.
- Open/apply/reset settings.
- Inspector tab selection.
- Settings change listener.

**Returns:** `void`.

**Frontend integration role:** Central place where DOM events connect to state-changing functions.

---

#### `loadSettingsTemplate()`

**Called by:** `bootstrap()`.

**Does:** Attempts to fetch `./settings.html` and place it inside `#settingsMount`. Falls back to an inline settings form if the fetch fails.

**Returns:** `Promise<void>`.

**Frontend integration role:** Decouples settings fields from the main HTML shell. Required field names are:

- `endpointUrl`
- `refreshIntervalMs`
- `bearerToken`
- `sessionId`

---

#### `refreshTools()`

**Called by:** initial bootstrap, refresh button, settings reset/apply, and refresh timer.

**Does:** Fetches the tool list from the backend, stores sorted descriptors in `state.tools`, populates the selector, updates status labels, and records fetch time.

**Returns:** `Promise<void>`.

**Backend dependency:** The endpoint must support `tools/list` and return `result.tools` as an array.

**Error behavior:** Displays the thrown error message in the tool status area and marks the tool count as refresh failed.

---

#### `populateTools(tools)`

**Called by:** `refreshTools()`.

**Does:** Converts tool descriptors into `<option>` nodes. Uses `tool.title ? `${tool.name} - ${tool.title}` : tool.name` as the label. Replaces the select contents and selects the first tool.

**Returns:** `void`.

**Input:** `ToolDescriptor[]`.

---

#### `selectTool(toolName)`

**Called by:** `populateTools()` and the tool selector change handler.

**Does:** Finds the selected descriptor by `name`, stores it in `state.selectedTool`, enables/disables submit, renders details/form, and refreshes payload preview.

**Returns:** `void`.

---

#### `renderSelectedTool()`

**Called by:** `selectTool()`.

**Does:** Renders selected tool details and inspector data. It reads:

- `tool.title`
- `tool.name`
- `tool.description`
- `tool.annotations.scopes`
- `tool.inputSchema`
- `tool.annotations`

It delegates form generation to `renderToolForm()`.

**Returns:** `void`.

**Backend dependency:** Tool descriptors should include `inputSchema` and `annotations.scopes` for best frontend behavior.

---

#### `renderScopeBadges(scopes, risky)`

**Called by:** `renderSelectedTool()`.

**Does:** Builds DOM nodes representing declared scopes. A scope is considered high-risk by the frontend if it appears in `highRiskScopes`.

**Returns:** `HTMLElement`.

**Integration note:** This is advisory only. Server-side authorization must still enforce actual policy.

---

#### `submitToolCall()`

**Called by:** tool form submit handler.

**Does:** Executes the selected tool:

1. Stops if no selected tool or already submitting.
2. Runs `confirmRiskIfNeeded(selectedTool)`.
3. Generates a request ID with `generateJsonRpcId("tool-call")`.
4. Collects arguments with `collectFormArguments()`.
5. Builds JSON-RPC payload with `buildCallPayload()`.
6. Sends HTTP POST to `settings.endpointUrl`.
7. Validates response shape with `assertValidJsonRpcResponse()`.
8. Appends normalized success/error result to response history.
9. Restores submit state in `finally`.

**Returns:** `Promise<void>`.

**Backend dependency:** The endpoint must support `tools/call` and return a JSON-RPC object with either `result` or `error`.

---

#### `confirmRiskIfNeeded(tool)`

**Called by:** `submitToolCall()` before network execution.

**Does:** Reads tool scopes, checks for any configured high-risk scope, and opens a confirmation dialog before proceeding. A session-only `Do not ask again` flag suppresses future prompts.

**Returns:** `Promise<boolean>`.

- `true`: continue call.
- `false`: abort call.

**Integration note:** This is not a hard security gate. Backend policy must remain authoritative.

---

#### `updatePayloadPreview()`

**Called by:** form input handler, `selectTool()`, and reset.

**Does:** Writes pretty JSON from `buildCurrentPayloadPreview()` into the payload inspector.

**Returns:** `void`.

---

#### `buildCurrentPayloadPreview()`

**Called by:** `updatePayloadPreview()` and client error normalization.

**Does:** Builds a non-submitting preview payload using ID `GENERATED_ID`. Required validation is disabled so incomplete forms can still preview.

**Returns:**

- JSON-RPC tool call preview object, or
- `{ error: string }` if argument parsing fails.

---

#### `getToolScopes(tool)`

**Called by:** `renderSelectedTool()` and `confirmRiskIfNeeded()`.

**Does:** Reads `tool.annotations.scopes`, coerces array values to strings, and returns an empty array if missing.

**Returns:** `string[]`.

---

#### `renderSettings(settings)`

**Called by:** `bootstrap()`, opening settings, and reset settings.

**Does:** Copies each setting value into matching form fields by name.

**Returns:** `void`.

---

#### `applySettingsFromForm()`

**Called by:** settings form submit handler.

**Does:** Reads settings form values, calls `updateSettings()`, then refreshes tools against the new endpoint/configuration.

**Returns:** `void`.

---

#### `updateEndpointLabels(settings = getSettings())`

**Called by:** `bootstrap()` and settings change listener.

**Does:** Writes active endpoint URL to the endpoint status label.

**Returns:** `void`.

---

#### `selectInspectorTab(tabName)`

**Called by:** inspector tab click handlers.

**Does:** Activates the selected inspector tab and shows one of:

- schema view
- annotations view
- payload preview

**Returns:** `void`.

---

## Tool Discovery Functions

### `fetchToolList(settings)`

**File:** `toolListFetcher.js`

**Called by:** `refreshTools()`.

**Does:** Sends a JSON-RPC `tools/list` request to `settings.endpointUrl`.

**Request:**

```json
{
  "jsonrpc": "2.0",
  "id": "tools-list-<generated>",
  "method": "tools/list",
  "params": {}
}
```

**Returns:**

```ts
Promise<{
  tools: ToolDescriptor[];
  raw: JsonRpcResponse;
  fetchedAt: Date;
}>
```

**Details:**

- Builds headers through `buildRequestHeaders(settings, requestId)`.
- Parses response text as JSON.
- Validates the body has either `result` or `error`.
- Throws if JSON-RPC `error` exists.
- Sorts returned tools by `name`.

---

### `createToolRefreshController(fetcher)`

**File:** `toolListFetcher.js`

**Called by:** top-level script initialization.

**Does:** Creates a timer controller around a `fetcher` callback.

**Returns:**

```ts
{
  start(intervalMs: number): void;
  stop(): void;
}
```

**Frontend integration role:** Keeps periodic tool discovery independent from UI state.

---

### `parseJsonResponse(response)`

**File:** `toolListFetcher.js`

**Called by:** `fetchToolList()`.

**Does:** Reads `response.text()`, parses JSON if non-empty, and throws a readable error for malformed JSON.

**Returns:** `Promise<object>`.

**Visibility:** Internal helper.

---

## Tool Form Functions

### `renderToolForm(container, tool)`

**File:** `toolFormGenerator.js`

**Called by:** `renderSelectedTool()` and reset form handler.

**Does:** Generates argument input controls from `tool.inputSchema`.

**Supported schema shape:**

```json
{
  "type": "object",
  "properties": {
    "name": { "type": "string" }
  },
  "required": ["name"]
}
```

**Supported field types:**

- `string`
- `integer`
- `number`
- `boolean`
- `array`
- `object`
- `enum`
- fallback raw JSON value

**Returns:** `void`.

**Fallback behavior:** If `inputSchema` is not an object schema with `properties`, renders a single `Arguments JSON` textarea.

---

### `collectFormArguments(form, tool, options = {})`

**File:** `toolFormGenerator.js`

**Called by:** `submitToolCall()` and `buildCurrentPayloadPreview()`.

**Does:** Reads generated form controls and converts DOM strings into JSON-compatible argument values according to schema type.

**Options:**

```ts
{
  validateRequired?: boolean // default true
}
```

**Returns:** `Record<string, unknown>`.

**Conversion behavior:**

| Schema/control type | Returned value |
|---|---|
| `boolean` | `field.checked` boolean |
| `integer` | integer `Number(rawValue)`; throws if not integer |
| `number` | finite number; throws if invalid |
| `array` | parsed JSON array/object from textarea |
| `object` | parsed JSON object from textarea |
| `raw` | parsed JSON value |
| `string` / `enum` | string value |
| optional blank field | omitted from returned object |
| required blank field | throws when validation is enabled |

---

### `buildCallPayload(tool, argumentsValue, id)`

**File:** `toolFormGenerator.js`

**Called by:** `submitToolCall()` and `buildCurrentPayloadPreview()`.

**Does:** Builds the backend `tools/call` JSON-RPC payload.

**Returns:**

```json
{
  "jsonrpc": "2.0",
  "id": "tool-call-id",
  "method": "tools/call",
  "params": {
    "name": "tool.name",
    "arguments": {}
  }
}
```

---

### Internal form helpers

These functions are internal but important for reuse:

| Function | Does | Returns |
|---|---|---|
| `createField(name, schema, required)` | Creates label, input, helper text, and error container for one schema field. | `HTMLElement` |
| `createInput(name, type, schema, required)` | Maps schema type/enum to browser control. | `HTMLElement` |
| `createRawField(name, schema, required)` | Creates fallback textarea for unsupported field schema. | `HTMLElement` |
| `renderRawJsonFallback(container, schema)` | Renders full-arguments JSON textarea when schema is unsupported. | `void` |
| `readFieldValue(field, schema, required, name, validateRequired)` | Converts one DOM field to typed argument value and handles validation errors. | `unknown | undefined` |
| `schemaType(schema)` | Resolves schema type, including nullable union schemas. | `string` |
| `showFieldError(name, message)` | Shows field-specific validation error. | `void` |
| `clearFieldError(name)` | Clears field-specific validation error. | `void` |

---

## Response Rendering Functions

### `appendResponse(historyContainer, entry)`

**File:** `responseRenderer.js`

**Called by:** `submitToolCall()`.

**Does:** Removes the empty state if present and prepends a response card created by `renderResponseCard(entry)`.

**Returns:** `void`.

---

### `renderResponseCard(entry)`

**File:** `responseRenderer.js`

**Called by:** `appendResponse()`.

**Does:** Builds the full response card for:

- transport/client errors
- JSON-RPC errors
- successful tool calls
- tool results marked with `result.isError`

**Returns:** `HTMLElement`.

**Response tabs:**

- `Rendered`: human-rendered `result.content`
- `Structured`: `result.structuredContent`
- `Raw`: full JSON-RPC response
- `Meta`: `result._meta`

---

### `renderRenderedContent(result)`

**File:** `responseRenderer.js`

**Called by:** `renderResponseCard()`.

**Does:** Renders each item in `result.content`.

**Returns:** `HTMLElement`.

**Expected backend result shape:**

```json
{
  "content": [
    { "type": "text", "text": "plain text" },
    { "type": "json", "mimeType": "application/json", "data": {} },
    { "type": "mime", "mimeType": "text/html", "data": "<p>html</p>" },
    { "type": "mime", "mimeType": "image/png", "data": "data:image/png;base64,..." }
  ],
  "structuredContent": {},
  "_meta": {}
}
```

---

### Internal response helpers

| Function | Does | Returns |
|---|---|---|
| `renderContentItem(item)` | Dispatches one content item by `type` or `mimeType`. | `HTMLElement` |
| `renderMediaBlock(tagName, item, className, attributes = {})` | Renders safe image/audio source into media element. | `HTMLElement` |
| `renderJsonBlock(value)` | Renders pretty JSON in a `<pre>`. | `HTMLElement` |
| `renderTabs(tabs)` | Creates response tab buttons. | `HTMLElement` |
| `bindTabs(tabs, views)` | Connects response tab buttons to views. | `void` |
| `contentValue(item)` | Extracts string content from `item.data`, `item.text`, or whole item. | `string` |
| `isSafeMediaSource(value)` | Allows only `data:`, `https://`, or `http://` media sources. | `boolean` |

---

## Settings Functions

### `DEFAULT_SETTINGS`

**File:** `settings.js`

**Purpose:** Initial in-memory client settings.

```ts
{
  endpointUrl: "http://100.121.15.11:4737/mcp",
  refreshIntervalMs: 30000,
  bearerToken: "",
  sessionId: ""
}
```

---

### `getSettings()`

**Called by:** bootstrap, refresh, submit, settings render, endpoint label update.

**Does:** Returns a copy of current in-memory settings.

**Returns:**

```ts
{
  endpointUrl: string;
  refreshIntervalMs: number;
  bearerToken: string;
  sessionId: string;
}
```

---

### `updateSettings(nextSettings)`

**Called by:** `applySettingsFromForm()`.

**Does:** Merges next settings into current settings, normalizes values, notifies listeners, and returns the updated copy.

**Returns:** settings object.

---

### `resetSettings()`

**Called by:** reset settings button handler.

**Does:** Restores `DEFAULT_SETTINGS`, notifies listeners, and returns the reset copy.

**Returns:** settings object.

---

### `onSettingsChanged(listener)`

**Called by:** `bindEvents()`.

**Does:** Registers a settings listener.

**Returns:** unsubscribe function.

```ts
() => void
```

---

### `buildRequestHeaders(settings, requestId)`

**Called by:** `fetchToolList()` and `submitToolCall()`.

**Does:** Builds headers for backend JSON-RPC requests.

**Returns:**

```ts
{
  Accept: "application/json";
  "Content-Type": "application/json";
  "X-Request-Id": string;
  Authorization?: `Bearer ${string}`;
  "X-Mcp-Session-Id"?: string;
}
```

**Auth behavior:** Authorization and session headers are included only if settings provide `bearerToken` and `sessionId`.

---

### `normalizeSettings(value)`

**File:** `settings.js`

**Does:** Normalizes endpoint, refresh interval, bearer token, and session ID.

**Returns:** settings object.

**Rules:**

- `endpointUrl`: string, trimmed, fallback to default.
- `refreshIntervalMs`: finite integer, minimum `5000`, fallback to default.
- `bearerToken`: trimmed string.
- `sessionId`: trimmed string.

**Visibility:** Internal helper.

---

## Error Handling Functions

### `errorNameForCode(code)`

**File:** `errorHandler.js`

**Does:** Maps JSON-RPC numeric error codes to symbolic names.

**Returns:** string.

Known mappings:

| Code | Name |
|---:|---|
| `-32700` | `PARSE_ERROR` |
| `-32600` | `INVALID_REQUEST` |
| `-32601` | `METHOD_NOT_FOUND` |
| `-32602` | `INVALID_PARAMS` |
| `-32603` | `INTERNAL_ERROR` |
| `-32001` | `UNAUTHORIZED` |
| `-32003` | `FORBIDDEN` |
| other | `JSON_RPC_ERROR` |

---

### `normalizeClientError(error, request)`

**Called by:** `submitToolCall()` catch block.

**Does:** Converts thrown client/transport exceptions into a response-history entry.

**Returns:**

```ts
{
  transportError: true,
  request: unknown,
  error: {
    code: "CLIENT_ERROR",
    name: "CLIENT_ERROR",
    message: string,
    data: string | null
  }
}
```

---

### `normalizeJsonRpcError(response, request)`

**Called by:** `submitToolCall()` when `body.error` exists.

**Does:** Converts backend JSON-RPC error response into a response-history entry.

**Returns:**

```ts
{
  response: JsonRpcResponse,
  request: JsonRpcRequest,
  error: {
    code: number | string | undefined,
    name: string,
    message: string,
    data: unknown
  }
}
```

---

### `assertValidJsonRpcResponse(response)`

**Called by:** `fetchToolList()` and `submitToolCall()`.

**Does:** Validates minimal JSON-RPC response shape.

**Returns:** `void`.

**Throws when:**

- body is not an object
- neither `response.result` nor `response.error` exists

---

### `renderErrorDetails(errorInfo)`

**Called by:** `renderResponseCard()` for transport errors.

**Does:** Renders normalized error name, code, message, and optional data.

**Returns:** `HTMLElement`.

---

## Utility Functions

### `generateJsonRpcId(prefix = "mcp")`

**Called by:** `fetchToolList()` and `submitToolCall()`.

**Does:** Creates a request ID using `crypto.randomUUID()` when available, else timestamp plus random fallback.

**Returns:** string.

Examples:

- `tools-list-<uuid>`
- `tool-call-<uuid>`

---

### `formatTimestamp(date = new Date())`

**Called by:** `refreshTools()` and `renderResponseCard()`.

**Does:** Formats a timestamp with hour, minute, second, and fractional seconds.

**Returns:** localized string.

---

### `prettyJson(value)`

**Called by:** inspectors, errors, response blocks, form defaults.

**Does:** `JSON.stringify(value ?? null, null, 2)`.

**Returns:** string.

---

### `safeText(value)`

**Called by:** text content renderer.

**Does:** Converts nullish values to empty string, strings to themselves, non-strings to pretty JSON.

**Returns:** string.

---

### `parseJsonTextarea(value, fieldName)`

**Called by:** form argument collection.

**Does:** Parses textarea content as JSON. Empty trimmed content returns `undefined`.

**Returns:** parsed JSON value or `undefined`.

**Throws:** `Error(`${fieldName} must contain valid JSON: ...`)`.

---

### `createElement(tagName, options = {})`

**Called by:** all DOM renderer modules.

**Does:** Creates an element, applies `className`, `textContent`, and attributes.

**Returns:** `HTMLElement`.

---

### `replaceChildren(parent, children)`

**Called by:** tool selector, form renderer, history clear, tool details renderer.

**Does:** Replaces children after filtering falsy children.

**Returns:** `void`.

---

### `sanitizeHtml(html)`

**Called by:** HTML MIME renderer.

**Does:** Parses HTML, removes blocked tags, strips event-handler attributes and `javascript:` attribute values.

**Returns:** sanitized HTML string.

**Blocked tags:** `script`, `iframe`, `object`, `embed`, `link`, `meta`, `base`.

---

### `isPlainObject(value)`

**Called by:** tool form generator.

**Does:** Checks for non-null object that is not an array.

**Returns:** boolean.

---

## Backend Tool Integration Contract

### Tool discovery contract

The frontend expects `tools/list` to return descriptors with this minimum shape:

```ts
type ToolDescriptor = {
  name: string;
  title?: string;
  description?: string;
  inputSchema?: JsonSchemaObject;
  annotations?: {
    scopes?: string[];
    [key: string]: unknown;
  };
};
```

`inputSchema` should be a flat object schema when possible:

```json
{
  "type": "object",
  "properties": {
    "path": {
      "type": "string",
      "description": "Path to read"
    },
    "limit": {
      "type": "integer",
      "description": "Maximum rows"
    }
  },
  "required": ["path"]
}
```

The frontend can still handle unsupported or non-flat schemas by falling back to raw JSON arguments.

---

### Tool call contract

The frontend sends `tools/call` payloads in this shape:

```ts
type ToolCallPayload = {
  jsonrpc: "2.0";
  id: string;
  method: "tools/call";
  params: {
    name: string;
    arguments: Record<string, unknown>;
  };
};
```

The backend should respond with a JSON-RPC envelope:

```ts
type JsonRpcSuccess = {
  jsonrpc?: "2.0";
  id: string;
  result: ToolResult;
};

type JsonRpcError = {
  jsonrpc?: "2.0";
  id: string | null;
  error: {
    code: number;
    message: string;
    data?: unknown;
  };
};
```

The frontend only requires either `result` or `error` to exist, but preserving `jsonrpc` and matching `id` is recommended.

---

### Tool result contract

A successful tool execution result should use:

```ts
type ToolResult = {
  content: ResultContent[];
  structuredContent?: unknown;
  isError?: true;
  _meta?: {
    status?: string;
    summary?: string;
    errorCode?: string;
    errorMessage?: string;
    generatedAt?: string;
    [key: string]: unknown;
  };
};
```

`content` drives the `Rendered` tab.

`structuredContent` drives the `Structured` tab.

`_meta` drives the `Meta` tab.

The raw JSON-RPC body drives the `Raw` tab.

---

## Backend Java API Mapping

### `McpDispatchHandler<R extends DispatchExecutionResult>`

**Backend role:** Generic dispatch interface for callable handlers.

```java
R call(ObjectNode arguments, McpCallContext context);
```

**Called by:** Meshingress dispatcher when a `tools/call` request reaches a registered handler.

**Does:** Executes backend logic using JSON object arguments and call context.

**Returns:** `DispatchExecutionResult` or subtype.

---

### `McpToolHandler`

**Backend role:** Tool-specific handler SPI.

```java
public interface McpToolHandler extends McpDispatchHandler<DispatchExecutionResult> {
    McpToolDescriptor descriptor();
}
```

**Functions:**

| Function | Does | Returns |
|---|---|---|
| `descriptor()` | Supplies tool metadata for discovery. | `McpToolDescriptor` |
| `call(ObjectNode arguments, McpCallContext context)` | Executes the tool. | `DispatchExecutionResult` |

**Frontend relevance:** `descriptor()` feeds `tools/list`; `call()` feeds `tools/call`.

---

### `DispatchExecutionResult`

**Backend role:** Canonical tool execution result that serializes into the frontend `result` object.

**Important builder methods:**

| Method | Does | Frontend output |
|---|---|---|
| `appendContent(ResultContent)` / `content(ResultContent)` | Adds an item to `content`. | `result.content[]` |
| `text(String)` | Adds text content. | Rendered as a text block. |
| `json(JsonNode)` | Adds JSON content. | Rendered as JSON. |
| `object(JsonNode)` | Adds object JSON content. | Rendered as JSON. |
| `array(JsonNode)` | Adds array JSON content. | Rendered as JSON. |
| `structuredContent(JsonNode)` | Sets machine-readable result. | `Structured` tab. |
| `error(boolean)` | Marks tool-level error. | Card title changes to tool error when true. |
| `error(String, String)` | Marks error and sets code/message metadata. | `isError`, `_meta.errorCode`, `_meta.errorMessage`. |
| `status(String)` | Adds result status metadata. | `_meta.status`. |
| `summary(String)` | Adds result summary metadata. | `_meta.summary`. |
| `meta(ObjectNode)` | Adds custom metadata. | `_meta`. |

**Serialization:** `toJson()` produces:

```json
{
  "content": [],
  "structuredContent": {},
  "isError": true,
  "_meta": {
    "status": "failed",
    "summary": "...",
    "errorCode": "...",
    "errorMessage": "...",
    "generatedAt": "2026-05-24T...Z"
  }
}
```

`structuredContent`, `isError`, and `_meta` are omitted when not set/applicable. `_meta.generatedAt` is added whenever metadata is emitted and no `generatedAt` is already present.

---

### `ResultContent`

**Backend role:** One renderable content block inside `DispatchExecutionResult.content`.

**Factories and frontend behavior:**

| Factory | Output type | JSON shape | Frontend behavior |
|---|---|---|---|
| `ResultContent.text(String)` | `text` | `{ "type": "text", "text": "..." }` | Renders as text. |
| `ResultContent.text(JsonNode)` | `text` | `{ "type": "text", "text": "..." }` | Requires textual JsonNode. |
| `ResultContent.object(JsonNode)` | `json` | `{ "type": "json", "mimeType": "application/json", "data": {} }` | Renders as JSON. |
| `ResultContent.array(JsonNode)` | `json` | `{ "type": "json", "mimeType": "application/json", "data": [] }` | Renders as JSON. |
| `ResultContent.number(Number)` | `json` | JSON scalar in `data`. | Renders as JSON. |
| `ResultContent.bool(boolean)` | `json` | JSON boolean in `data`. | Renders as JSON. |
| `ResultContent.json(JsonNode)` | `json` | application/json MIME content. | Renders as JSON. |
| `ResultContent.mime(MimeType, JsonNode)` | `json` or `mime` | MIME-dependent. | Renders by MIME type where supported. |

**MIME rendering supported by frontend:**

| MIME type | Frontend rendering |
|---|---|
| `application/json` | JSON block |
| `text/html` | sanitized iframe preview |
| `image/*` | image element if source is safe |
| `audio/*` | audio element if source is safe |
| other | raw JSON block |

---

## Annotation-Based Backend Tool Blueprint

A backend script or module should expose frontend-callable functionality by producing a public tool descriptor and accepting JSON arguments.

### Minimum annotated tool shape

```java
@McpTool(
    value = "example.tool",
    title = "Example Tool",
    description = "Callable from the frontend.",
    defaultFunction = "main"
)
@McpToolMapping("tools")
@McpToolScopes({McpToolScope.RUNTIME_READ})
public class ExampleTool {

    private final ObjectMapper objectMapper;

    public ExampleTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @McpFunction(
        value = "main",
        title = "Run Example",
        description = "Runs the example backend function."
    )
    @McpConfigureMapping(timeoutMs = 30_000, audit = true)
    public DispatchExecutionResult main(ExampleArgs args, McpCallContext context) {
        ObjectNode structured = objectMapper.createObjectNode()
            .put("message", args.message());

        return DispatchExecutionResult.builder()
            .text("Completed example call.")
            .structuredContent(structured)
            .status("ok")
            .summary("Example completed.")
            .build();
    }
}
```

### Argument DTO shape

```java
public record ExampleArgs(
    @McpInputField(
        value = "message",
        description = "Message to process.",
        required = true
    )
    String message
) {}
```

The schema generated from this model should allow the frontend to produce a form and submit:

```json
{
  "message": "hello"
}
```

---

## Required Backend Descriptor Fields

For each backend function intended for frontend use, ensure discovery exposes:

| Descriptor field | Required | Frontend use |
|---|---:|---|
| `name` | yes | Tool selection and `tools/call.params.name`. |
| `title` | no | Human-readable selector/detail label. |
| `description` | no | Tool detail text. |
| `inputSchema.type` | recommended | Must be `object` for generated form mode. |
| `inputSchema.properties` | recommended | Drives generated fields. |
| `inputSchema.required` | recommended | Drives required validation. |
| `annotations.scopes` | recommended | Drives risk display and advisory confirmation. |

---

## Argument Schema Guidelines

Use frontend-friendly JSON Schema:

```json
{
  "type": "object",
  "properties": {
    "query": {
      "type": "string",
      "description": "Search query."
    },
    "limit": {
      "type": "integer",
      "description": "Maximum result count."
    },
    "includeHidden": {
      "type": "boolean",
      "description": "Whether to include hidden records."
    },
    "filters": {
      "type": "object",
      "description": "Additional filter object."
    },
    "tags": {
      "type": "array",
      "description": "Tag list."
    },
    "mode": {
      "type": "string",
      "enum": ["fast", "full"]
    }
  },
  "required": ["query"]
}
```

Avoid relying on deep/nested interactive field generation. For nested objects or arrays, the current frontend expects users to enter JSON in a textarea.

---

## Result Design Guidelines

### Prefer combined human and machine output

Return both display content and structured content:

```java
return DispatchExecutionResult.builder()
    .text("Found 3 matching records.")
    .structuredContent(payload)
    .object(payload)
    .status("ok")
    .summary("Search completed.")
    .build();
```

This gives the frontend:

- Rendered human output from `content`.
- Machine-readable output from `structuredContent`.
- Full raw response for debugging.
- Metadata for status/summary.

### Use tool-level errors for handled failures

For expected domain failures, return a successful JSON-RPC envelope with `result.isError = true`:

```java
return DispatchExecutionResult.builder()
    .error("VALIDATION_FAILED", "The provided path is not allowed.")
    .status("failed")
    .summary("Tool rejected the request.")
    .build();
```

Use JSON-RPC `error` only for protocol, authorization, dispatch, or server-level failures.

---

## High-Risk Scope Integration

The frontend currently treats these scopes as high-risk for advisory confirmation:

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

Backend modules should still declare the complete least-privilege scope set using `@McpToolScopes`. Frontend confirmation does not replace backend authorization, approval policy, scope enforcement, auditing, or secret handling.

---

## Reusable Integration Blueprint

Use this sequence for any new backend script/tool that should be callable from the frontend.

### Backend steps

1. Create or expose a Meshingress tool handler.
2. Ensure `descriptor()` or annotation discovery publishes:
   - stable `name`
   - useful `title`
   - useful `description`
   - JSON object `inputSchema`
   - `annotations.scopes`
3. Implement `call(ObjectNode arguments, McpCallContext context)` or an `@McpFunction` method.
4. Convert validated arguments into backend script/function inputs.
5. Return `DispatchExecutionResult` with:
   - at least one `ResultContent`
   - optional `structuredContent`
   - optional `_meta` status/summary
   - `isError` for handled tool errors
6. Verify `tools/list` exposes the descriptor.
7. Verify `tools/call` accepts the arguments generated by the frontend.

### Frontend steps

1. Set `endpointUrl` to the Meshingress MCP endpoint.
2. Call `refreshTools()` or wait for the refresh timer.
3. Select the tool by `name`.
4. Let `renderToolForm()` generate fields from `inputSchema`.
5. Let `collectFormArguments()` produce JSON arguments.
6. Let `buildCallPayload()` construct the `tools/call` request.
7. POST using `buildRequestHeaders()`.
8. Validate with `assertValidJsonRpcResponse()`.
9. Render via `appendResponse()`.

---

## Minimal Request/Response Examples

### `tools/list` response

```json
{
  "jsonrpc": "2.0",
  "id": "tools-list-123",
  "result": {
    "tools": [
      {
        "name": "files.read",
        "title": "Read File",
        "description": "Reads a file from an allowed path.",
        "inputSchema": {
          "type": "object",
          "properties": {
            "path": {
              "type": "string",
              "description": "File path to read."
            }
          },
          "required": ["path"]
        },
        "annotations": {
          "scopes": ["FILES_READ"]
        }
      }
    ]
  }
}
```

### `tools/call` request

```json
{
  "jsonrpc": "2.0",
  "id": "tool-call-123",
  "method": "tools/call",
  "params": {
    "name": "files.read",
    "arguments": {
      "path": "/tmp/example.txt"
    }
  }
}
```

### `tools/call` success response

```json
{
  "jsonrpc": "2.0",
  "id": "tool-call-123",
  "result": {
    "content": [
      {
        "type": "text",
        "text": "File read successfully."
      },
      {
        "type": "json",
        "mimeType": "application/json",
        "data": {
          "path": "/tmp/example.txt",
          "bytes": 128
        }
      }
    ],
    "structuredContent": {
      "path": "/tmp/example.txt",
      "bytes": 128
    },
    "_meta": {
      "status": "ok",
      "summary": "Read completed.",
      "generatedAt": "2026-05-24T17:00:00Z"
    }
  }
}
```

### `tools/call` handled tool error response

```json
{
  "jsonrpc": "2.0",
  "id": "tool-call-123",
  "result": {
    "content": [],
    "isError": true,
    "_meta": {
      "status": "failed",
      "summary": "Read rejected.",
      "errorCode": "PATH_NOT_ALLOWED",
      "errorMessage": "The path is outside the allowed directory.",
      "generatedAt": "2026-05-24T17:00:00Z"
    }
  }
}
```

### JSON-RPC protocol error response

```json
{
  "jsonrpc": "2.0",
  "id": "tool-call-123",
  "error": {
    "code": -32602,
    "message": "Invalid params",
    "data": {
      "field": "path"
    }
  }
}
```

---

## Implementation Checklist

### Backend checklist

- [ ] Tool appears in `tools/list`.
- [ ] Descriptor has stable `name`.
- [ ] Descriptor has `inputSchema.type = "object"`.
- [ ] Descriptor has `inputSchema.properties`.
- [ ] Required fields are listed in `inputSchema.required`.
- [ ] Descriptor has `annotations.scopes`.
- [ ] Handler accepts `tools/call.params.arguments` exactly as emitted by the frontend.
- [ ] Handler returns `DispatchExecutionResult` serialized to `result`.
- [ ] Result includes `content` array.
- [ ] Result uses `structuredContent` for machine-readable data when useful.
- [ ] Result uses `_meta.status` and `_meta.summary` when useful.
- [ ] Expected tool failures use `result.isError`, not JSON-RPC `error`.
- [ ] Protocol/auth/server failures use JSON-RPC `error`.

### Frontend checklist

- [ ] `endpointUrl` points to the MCP endpoint.
- [ ] `buildRequestHeaders()` includes any required token/session headers.
- [ ] `fetchToolList()` can parse the endpoint response.
- [ ] `renderToolForm()` can handle the tool schema.
- [ ] `collectFormArguments()` emits the expected JSON argument object.
- [ ] `buildCallPayload()` uses the selected tool `name`.
- [ ] `assertValidJsonRpcResponse()` passes for both success and error envelopes.
- [ ] `appendResponse()` renders content, structured data, raw response, and metadata.

---

## Known Constraints

- Settings are memory-only in the current MVP.
- Authentication is included only as optional bearer/session headers from in-memory settings.
- The high-risk confirmation dialog is advisory, page-session scoped, and not server authorization.
- Generated forms are optimized for flat object schemas.
- Nested object and array fields are entered as JSON textareas.
- HTML rendering is sanitized before preview.
- Media rendering only accepts `data:`, `https://`, or `http://` sources.
- CSS and presentation styling are intentionally excluded from this handoff.

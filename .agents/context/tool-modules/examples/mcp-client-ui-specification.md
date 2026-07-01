# MCP Client UI Specification

Build a web page that makes HTTP tool calls. The page should have a form for selecting a tool and entering parameters, plus a submit button that sends the request.

### Page theme and layout
The webpage should have a clean and modern design, with a simple layout that focuses on usability. The form for inputting the method name and parameters should be prominently displayed at the top of the page, with clear labels and input fields. The submit button should be easily identifiable and placed near the form for easy access.

#### Theme accent colors
- Primary: #4A90E2 (a calm blue for buttons and highlights)
- Secondary: #50E3C2 (a refreshing teal for accents and hover states)
- Background: #F5F7FA (a light, neutral background color)
- Text: #333333 (a dark gray for high readability)

### Method of truth

The tool `tools/list` can be used to retrieve the list of available tools and their schemas, which can then be used to populate the form dynamically.
The webpage can be built using HTML, CSS, and JavaScript (with Fetch API for making HTTP requests). Populating tool options dynamically will require fetching the tool list on page load and updating the form accordingly.

* Every time the form is submitted, it should send a POST request to the MCP endpoint with the appropriate JSON-RPC payload, and then display the response in a readable format.

### Included pages (not limited to)
* `index.html`: The main webpage with the form and response display area.
* `styles.css`: CSS for styling the webpage.
* `colors.css`: CSS variables for the theme colors to maintain consistency across the page.
* `script.js`: JavaScript for handling form submission, making HTTP requests, and updating the
* `settings.js`: JavaScript for configuring the MCP endpoint and other constants.
* `settings.html`: A separate page or modal for updating settings like the MCP endpoint URL and tool refresh interval.
* `mimes\*.html`: Optional HTML templates for rendering different mime types in the response.
* `components\*.html`: Optional HTML templates for reusable components like the tool selection dropdown, response display cards, error messages, etc.
* `utils.js`: JavaScript utility functions for tasks like pretty-printing JSON, sanitizing HTML for safe rendering, generating unique IDs for requests, etc.
* `responseRenderer.js`: JavaScript for handling the logic of rendering different types of response content based on the `type` and `mimeType` fields in the response.
* `toolFormGenerator.js`: JavaScript for dynamically generating form fields based on the input schema of the selected tool.
* `toolListFetcher.js`: JavaScript for fetching the list of tools from the MCP endpoint and updating the tool selection dropdown.
* `errorHandler.js`: JavaScript for handling and displaying errors from the MCP response in a user-friendly way.

The CSS files should define styles for the overall layout, form elements, buttons, and response display cards, using the defined theme colors for consistency. 
The JavaScript files should be organized to separate concerns, with each file handling a specific aspect of the webpage's functionality, making it easier to maintain and extend in the future.

### Page behavior
1. On page load, fetch the list of tools from the MCP endpoint and populate a dropdown or selection input with the tool names. (This should also refresh every N number of seconds to keep the tool list updated. Settable by the user in `settings.js`)
2. When a tool is selected, dynamically generate form fields based on the input schema of the selected tool.
3. When the form is submitted, construct a JSON-RPC request with the selected method and input parameters, and send it to the MCP endpoint.
4. Display the response from the MCP endpoint in multiple options below the form:
   1. (page-defined console) As a pretty-printed mime rendered output if the `content` field is present in the response, using the `type` and `mimeType` for rendering hints.
   2. (page-defined console) As viewable structured content if the `structuredContent` field is present in the response, rendering it as formatted JSON.
   3. (mime-rendered) If the `content` field includes a supported `mimeType`, render it accordingly (e.g., render HTML, display images, audio, etc.) instead of just showing the raw data.
   * These display options can be toggled by the user, allowing them to choose how they want to view the response content. For example, they might want to see the raw JSON for debugging purposes or a rendered view for easier consumption.
   * The console area should be designed to handle multiple response entries, allowing users to scroll through previous responses and compare them if needed. Each response entry can be timestamped for better tracking of interactions.
   * The mime rendering should be secure, especially for HTML content, to prevent XSS attacks. This can be achieved by sanitizing the HTML before rendering it in the page.
   * mime rendering can be extended to support various types, such as images (rendered in an `<img>` tag), audio (rendered in an `<audio>` tag), and other formats as needed. The rendering logic should be modular to allow easy addition of new mime types in the future.
5. When the settings are updated (e.g., MCP endpoint URL, tool refresh interval), the page should apply these changes immediately without requiring a full page reload. This can be achieved by using JavaScript to update the relevant variables and re-fetching the tool list if necessary.

Placeholders can be used in the HTML files for dynamic content that will be populated by JavaScript, such as the tool selection dropdown and the response display area. 
Animations and transitions can be added to enhance the user experience, such as smooth transitions when displaying new responses or when switching between different display options.
The response should be displayed in a formatted way below the form.

The webpage should handle both the `result` and `error` fields in the response, displaying the content or error message accordingly. The `content` field can be rendered as formatted text, while the `structuredContent` can be displayed as pretty-printed JSON for better readability.

### Response error handling
If the response contains an `error` field, the webpage should display the error code and message in a clear and user-friendly manner, possibly with a red background or an error icon to indicate that something went wrong. 
If the error includes additional data, that information can also be displayed to help the user understand the issue.

```text
PARSE_ERROR = -32700
INVALID_REQUEST = -32600
METHOD_NOT_FOUND = -32601
INVALID_PARAMS = -32602
INTERNAL_ERROR = -32603
UNAUTHORIZED = -32001
FORBIDDEN = -32003
```
---

```http request
# Replace placeholder values in <> before sending

POST /mcp HTTP/1.1
Host: 100.121.15.11:4737
User-Agent: meshingress-client/1.0
Accept: application/json
Content-Type: application/json
Content-Length: <calculated-by-client>      # optional
X-Request-Id: GENERATE_UNIQUE_JSON_RPC_ID   # optional
Authorization: Bearer <token>               # optional, auth not implemented yet
X-Mcp-Session-Id: <session-id>              # optional

{
  "jsonrpc": "2.0",
  "id": "GENERATE_UNIQUE_JSON_RPC_ID",
  "method": "tools/list",
  "params": {}
}
```

The response will contain a list of tools with their metadata, including the input schema for each tool. This information can be used to dynamically generate the form fields for the selected tool.
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "tools": [
      {
        "name": "architect.entries.list",
        "title": "List Architect Entries",
        "description": "List structured engineering memory entries by status, tag, or text query.",
        "inputSchema": {
          "type": "object",
          "properties": {
            "status": {
              "type": "string",
              "enum": [
                "pending",
                "active",
                "blocked",
                "resolved",
                "archived"
              ]
            },
            "tag": {
              "type": "string"
            },
            "query": {
              "type": "string"
            }
          },
          "additionalProperties": false
        },
        "annotations": {
          "readOnlyHint": true,
          "destructiveHint": false,
          "idempotentHint": true
        }
      },
      {
        "name": "cli.powershell",
        "title": "PowerShell CLI",
        "description": "Execute a received PowerShell script and append every execution track to the result.",
        "inputSchema": {
          "type": "object",
          "additionalProperties": false,
          "properties": {
            "script": {
              "type": "string",
              "description": "PowerShell script body to execute."
            },
            "workingDirectory": {
              "type": "string",
              "description": "Optional working directory. Defaults to the current JVM working directory."
            },
            "timeoutMs": {
              "type": "integer",
              "description": "Optional execution timeout in milliseconds. Defaults to 20000 and is capped by the tool."
            },
            "executable": {
              "type": "string",
              "description": "Optional PowerShell executable. Defaults to pwsh. Use powershell.exe for Windows PowerShell."
            },
            "arguments": {
              "type": "array",
              "description": "Optional script arguments passed after -File."
            },
            "environment": {
              "type": "object",
              "description": "Optional environment variables to add or override for the child process."
            },
            "includeScriptInStructuredContent": {
              "type": "boolean",
              "description": "Whether to echo the script body in structuredContent. Defaults to false."
            }
          },
          "required": [
            "script"
          ]
        },
        "annotations": {
          "scopes": [
            "SHELL_EXECUTE",
            "FILES_WRITE"
          ]
        }
      },
      {
        "name": "helloworld.greet",
        "title": "Hello World",
        "description": "Return a greeting from an external Meshingress tool module.",
        "inputSchema": {
          "type": "object",
          "additionalProperties": false,
          "description": "Greet a person by name",
          "properties": {
            "name": {
              "type": "string",
              "description": "The name of the person to greet"
            }
          },
          "required": [
            "name"
          ]
        },
        "annotations": {
          "scopes": [
            "USER_WRITE"
          ]
        }
      },
      {
        "name": "instagram.fetch",
        "title": "Instagram Fetch",
        "description": "Fetches data from Instagram based on a given URL.",
        "inputSchema": {
          "type": "object",
          "additionalProperties": false,
          "description": "Fetches data from Instagram based on a given URL.",
          "properties": {
            "url": {
              "type": "string",
              "description": "The URL of the Instagram post to fetch data from."
            }
          },
          "required": [
            "url"
          ]
        },
        "annotations": {
          "scopes": [
            "NETWORK_ACCESS",
            "EXTERNAL_API_READ"
          ]
        }
      }
    ]
  }
}
```

The expected tool response is usually in the shape of the JSON provided below. 

#### Successful response example:
```json
{
   "jsonrpc": "2.0",
   "id": "GENERATED_UNIQUE_JSON_RPC_ID_FROM_REQUEST",
   "result": {
      "content": [
         {
            "type": "text",
            "text": ... // tool output in text form, such as a formatted list of architect entries or a greeting message
         },
         {
            "type": "json",
            "mimeType": "application/json", // suports multiple mime types for rendering hints, but defaults to application/json for structured content
            "data": ... // tool output in structured form, such as an array of architect entry objects or a structured greeting with metadata
         }
      ],
      "structuredContent": {
         "message": "Hello, Meshingress!"
      },
      "_meta": {
         "generatedAt": "2026-05-24T02:49:57.643624800Z"
      }
   }
}
```

#### Error response example:
```json
{
   "jsonrpc": "2.0",
   "id": "GENERATED_UNIQUE_JSON_RPC_ID_FROM_REQUEST",
   "error": {
      "code": -32602,
      "message": ..., // error message such as "Invalid params: missing required parameter 'name'"
      "data": { // optional additional error data for debugging or user feedback
         "parameter": "name",
         "expectedType": "string",
         "receivedValue": null
      }
   }
}
```

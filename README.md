# Meshingress

Meshingress is a Java/Spring Boot MCP tool server and runtime that discovers, registers, and dispatches tool modules. Tools can be declared with annotations or by implementing the SPI.

> Status: early project/runtime framework. APIs and configuration are still evolving.

## Repository Layout

```txt
app/meshingress-server
app/meshingress-tool-bundle
lib/meshingress-tool-api
lib/meshingress-tool-annotations
lib/meshingress-tool-framework
lib/meshingress-config
toolspace/
architect/
```

- `app/meshingress-server` is the Spring Boot MCP server runtime.
- `app/meshingress-tool-bundle` aggregates attachable tool modules for server startup discovery.
- `lib/meshingress-tool-api` contains the tool SPI (`McpToolHandler`, `DispatchExecutionResult`).
- `lib/meshingress-tool-annotations` defines the tool annotation model.
- `lib/meshingress-tool-framework` implements annotation scanning and dispatch wiring.
- `lib/meshingress-config` defines `meshingress.*` configuration properties.
- `toolspace/` contains attachable tool modules.
- `architect/` stores durable project planning and decision records.

## Core Concepts

### MCP server/runtime

The server exposes MCP transports and dispatches JSON-RPC tool calls. The HTTP JSON-RPC endpoint is `POST /mcp`, and the WebSocket transport is configured by `meshingress.mcp.websocket.path` (default `/mcp/ws`).

### Tool modules

Tool modules live under `toolspace/<module-name>/`, are Maven modules, and are attached to the server via dependency aggregation. The server discovers tool beans via Spring Boot auto-configuration.

### Tool annotations

The annotation path for declaring tools includes:

- `@McpTool`
- `@McpToolMapping`
- `@McpFunction`
- `@McpInputField`
- `@McpFunctionParam`
- `@McpToolScopes`
- `@McpConfigureMapping`

### Tool SPI

Tools may also implement `McpToolHandler`, which extends `McpDispatchHandler<DispatchExecutionResult>`, and return a `McpToolDescriptor` from `descriptor()`.

### Results

Tool calls return `DispatchExecutionResult`, which can include `ResultContent` items, optional `structuredContent`, and `_meta` fields such as status, summary, and error details.

### Scopes

`McpToolScope` declares fine-grained permissions and carries metadata such as category, access mode, risk level, audit recommendation, and privileged status.

## Configuration

`meshingress.*` configuration groups (see `MeshingressProperties`):

- `meshingress.identity` defines the node identity, environment, and public base URL.
- `meshingress.mcp.websocket` configures the MCP WebSocket transport (path, origins, timeouts, auth).
- `meshingress.tools` controls tool registry behavior, allow/deny lists, and defaults.
- `meshingress.dispatch` sets dispatch concurrency limits, timeouts, and error shaping.
- `meshingress.security` controls auth mode, approval requirements, and deny/allow defaults.
- `meshingress.scopes` enforces scope-specific safeguards and approvals.
- `meshingress.audit` governs audit logging and redaction behavior.
- `meshingress.secrets` configures secret resolution and redaction.

## Building

```bash
./mvnw clean verify
```

```powershell
.\mvnw.cmd clean verify
```

## Running

Run the server module from `app/meshingress-server` using Spring Boot/Maven once the required configuration is present. The MCP HTTP entry point is `POST /mcp`, and the WebSocket path defaults to `/mcp/ws`.

## Creating a Tool Module

1. Create `toolspace/<module-name>/`.
2. Add a module `pom.xml`.
3. Depend on:
   - `meshingress-tool-api`
   - `meshingress-tool-annotations`
   - `spring-boot-autoconfigure`
4. Add the module to the root Maven `<modules>` list.
5. Add the tool module as a dependency of `app/meshingress-tool-bundle/pom.xml` (the server depends on this bundle).
6. Create a tool class with `@McpTool`, `@McpToolMapping`, and one or more `@McpFunction` methods.
7. Add input records or function parameters.
8. Declare least-privilege scopes with `@McpToolScopes`.
9. Add Spring auto-configuration for the tool bean.
10. Add `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
11. Build and verify the tool appears in `tools/list` and works through `tools/call`.

## Minimal Tool Example

```java
package dev.mrk.toolspace.example;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.result.ResultContent;
import dev.mrk.meshingress.api.tools.annotation.McpFunction;
import dev.mrk.meshingress.api.tools.annotation.McpInputField;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.api.tools.annotation.McpToolMapping;
import dev.mrk.meshingress.api.tools.annotation.McpToolScopes;
import dev.mrk.meshingress.scopes.McpToolScope;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@McpTool(
        value = "example.echo",
        title = "Example Echo",
        description = "Echoes input and returns structured content.",
        defaultFunction = "echo"
)
@McpToolMapping("tools")
@McpToolScopes(McpToolScope.RUNTIME_READ)
public class ExampleEchoTool {

    private final ObjectMapper objectMapper;

    public ExampleEchoTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @McpFunction(value = "echo", description = "Echo a message back.")
    public DispatchExecutionResult echo(EchoArgs args, McpCallContext context) {
        ObjectNode payload = objectMapper.createObjectNode()
                .put("message", args.message());

        return DispatchExecutionResult.builder()
                .appendContent(ResultContent.text("Echo: " + args.message()))
                .structuredContent(payload)
                .error(false)
                .build();
    }

    public record EchoArgs(
            @McpInputField(value = "message", description = "Message to echo.")
            String message
    ) {
    }
}
```

## Security Notes

- Use least-privilege scopes and keep them focused.
- Avoid `SHELL_EXECUTE` unless it is necessary.
- Do not hard-code secrets; prefer references like `env:EXAMPLE_API_KEY`.
- Treat broad network, file, shell, database, token, secret, and policy scopes as high risk.
- Enable audit for privileged or high-risk operations.

## Architect Directory

`architect/` stores durable engineering memory and planning records. Use the lifecycle folders:

- `pending/`
- `active/`
- `resolved/`
- `discontinued/` for abandoned or superseded work in this workspace
- `archived/` for long-term archival records when used by the project

Work entries should include files such as `meta.json`, `brief.md`, `todo.md`, `context.md`, and resolution files when complete.

## Contributing Guidelines

- Keep tool IDs lowercase and pattern-safe (e.g., `example.echo`).
- Prefer focused modules and focused architect entries.
- Keep README examples free of real secrets.
- Update docs when adding annotations, scopes, configuration groups, or result behavior.
- Verify new tools with a build and MCP list/call checks.

## Non-goals / Cautions

- Do not document planned features as implemented.
- Do not expose raw secret values.
- Do not copy unresolved TODOs into the README as guarantees.

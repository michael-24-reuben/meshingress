# Agent Instructions: Create the Meshingress Project README

You are creating or updating the root `README.md` for the Meshingress project.

## Goal

Write a clear, developer-facing README that explains:

1. What Meshingress is.
2. How the repository is organized.
3. How to build and run it.
4. How MCP tools are exposed through tool modules.
5. How configuration, scopes, dispatch, audit, and secrets work at a high level.
6. How contributors should add a new tool module.
7. Where long-lived project planning records belong.

The README should be practical, not marketing-heavy.

## Source Material to Use

Use the project files as the source of truth.

Important references:

- Existing `architect/README.md` explains the structured engineering memory model for planning, active work, resolved work, and archived records.
- `tool-module-README.md` explains how Meshingress tool modules are structured, registered, configured, and tested.
- `MeshingressProperties.java` defines the current `meshingress.*` configuration surface.
- `mcp-tool-scopes.java.md` defines the `McpToolScope` permission model, including categories, risk levels, audit requirements, and approval behavior.
- `mcp-configuration-classes.java.md` defines mapping configuration, secrets, availability mode, audit, debug trace, and timeout annotations.
- `mcp-tool-classes.java.md`, `mcp-function-classes.java.md`, and `mcp-parameter-classes.java.md` define the annotation model for tools, functions, and inputs.
- `ResultContent.java`, `DispatchExecutionResult.java`, `McpToolHandler.java`, and `McpDispatchHandler.java` define the low-level SPI/result model.

## README Structure

Create the README with these sections, in this order:

### 1. Title

Use:

```md
# Meshingress
````

### 2. One-paragraph Summary

Explain that Meshingress is a Java/Spring Boot MCP tool server/runtime that discovers, registers, and dispatches tool modules. Mention that tools can be declared with annotations or by implementing the SPI.

### 3. Current Status

Add a concise status note. Example:

```md
> Status: early project/runtime framework. APIs and configuration are still evolving.
```

Do not overclaim production maturity unless the repository proves it.

### 4. Repository Layout

Document the known layout:

```txt
app/meshingress-server
lib/meshingress-tool-api
lib/meshingress-tool-annotations
lib/meshingress-tool-framework
lib/meshingress-config
toolspace/
architect/
```

For each directory, give a one-line explanation.

### 5. Core Concepts

Include short subsections for:

#### MCP server/runtime

Explain that the server exposes MCP transport endpoints and dispatches tool calls.

#### Tool modules

Explain that tool modules live under `toolspace/<module-name>/`, are Maven modules, and are attached to the server as dependencies.

#### Tool annotations

Explain the common annotation path:

* `@McpTool`
* `@McpToolMapping`
* `@McpFunction`
* `@McpInputField`
* `@McpFunctionParam`
* `@McpToolScopes`
* `@McpConfigureMapping`

#### Tool SPI

Explain that a tool can also implement `McpToolHandler`, which extends `McpDispatchHandler<DispatchExecutionResult>`.

#### Results

Explain that tool calls return `DispatchExecutionResult`, with optional content, structured content, error state, and `_meta`.

#### Scopes

Explain that `McpToolScope` declares fine-grained permissions and carries metadata such as category, access mode, risk level, audit recommendation, and privileged status.

### 6. Configuration

Add a section for `meshingress.*` configuration groups.

Include these groups:

* `meshingress.identity`
* `meshingress.mcp.websocket`
* `meshingress.tools`
* `meshingress.dispatch`
* `meshingress.security`
* `meshingress.scopes`
* `meshingress.audit`
* `meshingress.secrets`

For each group, provide 1–2 sentences. Avoid listing every property unless useful.

### 7. Building

Add a build section using the Maven wrapper.

Use:

```bash
./mvnw clean verify
```

For Windows:

```powershell
.\mvnw.cmd clean verify
```

If the exact build command is not verified, say:

```md
The project is Maven-based; use the repository Maven wrapper from the repo root.
```

### 8. Running

Add a placeholder run section if the exact command is not available from source files.

Use cautious wording:

```md
Run the server module from `app/meshingress-server` using Spring Boot/Maven once the required configuration is present.
```

Do not invent ports or endpoints unless they appear in config/docs. The known WebSocket path is `/mcp/ws`.

### 9. Creating a Tool Module

Summarize the workflow:

1. Create `toolspace/<module-name>/`.
2. Add a module `pom.xml`.
3. Depend on:

   * `meshingress-tool-api`
   * `meshingress-tool-annotations`
   * `spring-boot-autoconfigure`
4. Add the module to the root Maven modules list.
5. Add the tool module as a dependency of `app/meshingress-server`.
6. Create a tool class with `@McpTool`, `@McpToolMapping`, and one or more `@McpFunction` methods.
7. Add input records or function parameters.
8. Declare least-privilege scopes.
9. Add Spring auto-configuration.
10. Add `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
11. Build and verify the tool appears in `tools/list` and works through `tools/call`.

### 10. Minimal Tool Example

Include a compact Java example, not the full long example from the docs.

The example should show:

* a `@McpTool`
* a `@McpFunction`
* an input record with `@McpInputField`
* `DispatchExecutionResult.builder()`
* `ResultContent.text(...)`
* `structuredContent(...)`

Keep it short enough for a root README.

### 11. Security Notes

Add a section with firm guidance:

* Use least-privilege scopes.
* Avoid `SHELL_EXECUTE` unless necessary.
* Do not hard-code secrets.
* Prefer secret refs such as `env:EXAMPLE_API_KEY`.
* Treat broad network, file, shell, database, token, secret, and policy scopes as high risk.
* Enable audit for privileged or high-risk operations.

### 12. Architect Directory

Document that `architect/` stores durable engineering memory.

Mention:

* `pending/`
* `active/`
* `resolved/`
* `archived/`

Explain that work records should include files like `meta.json`, `brief.md`, `todo.md`, `context.md`, and resolution files where applicable.

### 13. Contributing Guidelines

Add practical contribution rules:

* Keep tool IDs lowercase and pattern-safe.
* Prefer focused modules and focused architect entries.
* Keep README examples free of real secrets.
* Update docs when adding annotations, scopes, configuration groups, or result behavior.
* Verify new tools with build and MCP list/call checks.

### 14. Non-goals / Cautions

Add a short note:

* Do not document planned features as implemented.
* Do not expose raw secret values.
* Do not copy unresolved TODOs into the README as guarantees.

## Style Requirements

* Use concise Markdown.
* Prefer tables only where they improve scanning.
* Use fenced code blocks for commands and examples.
* Keep the README useful to a new contributor.
* Do not include excessive internal implementation detail in the root README.
* Put deep tool-authoring details in a dedicated tool module guide.
* Do not invent behavior not supported by the source files.

## Final Notes
* The README should be a living document; update it as the project evolves.
* Use the `architect/` directory for detailed planning and records, not the README.

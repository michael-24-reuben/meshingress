AGENTS.md

Purpose
-------
This file gives an AI coding agent the minimal, high-value orientation required to be productive in this codebase.

Branch policy (important)
-------------------------
- `main` is the official deployed branch and is reserved for owner-directed deployment state. Treat it as protected by default.
- `development` is the active development branch. Unless the user explicitly permits work on `main`, make code, documentation, and cleanup changes on `development`.
- If an agent starts on `main` without explicit permission to edit it, stop before making changes and ask whether to switch to `development`, create a work branch from `development`, or proceed on `main`.
- Do not restore files deleted from `main` unless the user explicitly asks for that recovery. Deletions on `main` may be intentional deployment-branch pruning.

High-level architecture (big picture)
------------------------------------
- Maven reactor with three module families:
  - `lib/meshingress-tool-api` contains the shared MCP tool SPI.
  - `toolspace/*` contains attachable tool modules, including `toolspace/helloworld`, `toolspace/instagram-api`, `toolspace/powershell-cli`, `toolspace/whatsapp-cobalt`, `toolspace/voicebox`, and `toolspace/webtoon-downloader`.
  - `app/meshingress-server` contains the Spring Boot application. Java 25 is used (see `pom.xml`).
  - `app/meshingress-tool-bundle` aggregates tool module dependencies for server startup discovery.
- Two logical surfaces:
  - HTTP/REST internal API under `/api/v1/architect/*` (in `architect/` domain work; see `architect/README.md`).
  - MCP transport: a JSON-RPC 2.0 over HTTP endpoint at POST `/mcp` implemented by `mcp/McpController` and routed to `mcp/McpDispatcher`.
  - MCP transport over WebSocket at `/mcp/ws` configured by `meshingress.mcp.websocket.path`.
- MCP is the main plugin/extension surface: tool modules implement the API `McpToolHandler`, expose handlers as Spring beans, server internals discover them via `ToolRegistry`, and `ToolExecutor` invokes them.

Key files to read first
----------------------
- `lib/meshingress-tool-api/src/main/java/dev/mrk/meshingress/api/tools/McpToolHandler.java` — SPI interface tool modules implement.
- `lib/meshingress-tool-api/src/main/java/dev/mrk/meshingress/api/tools/McpToolDescriptor.java` — public tool metadata contract.
- `.agents/context/tool-modules/README.md` — portable context bundle and upload guidance for online models.
- `.agents/context/tool-modules/BUILDING_TOOL_MODULES.md` — compact current guide for building and editing tool modules.
- `toolspace/helloworld/src/main/java/dev/mrk/toolspace/helloworld/HelloWorldTool.java` — example external tool module.
- `toolspace/helloworld/src/main/java/dev/mrk/toolspace/helloworld/HelloWorldToolAutoConfiguration.java` — example auto-configuration that attaches the tool when the module is on the server classpath.
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/MeshingressApplication.java` — app entry.
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/McpController.java` — HTTP transport and request header expectations.
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/McpDispatcher.java` — JSON-RPC envelope validation and method-family gathering.
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/internal/InternalMcpController.java` — native MCP lifecycle methods.
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/tools/ToolsMcpController.java` — public MCP tool listing and calling.
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/RolesMcpController.java` — role-gated registry methods.
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/JsonRpcResponses.java` — canonical JSON-RPC response formatting.
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/config/McpWebSocketConfig.java` — WebSocket transport wiring for MCP.
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/McpWebSocketHandler.java` — WebSocket JSON-RPC handling.
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/RoleAuthorizationService.java` — role auth logic (X-Mcp-Role header, legacy X-Mcp-Admin header, or Bearer admin token).
- `app/meshingress-server/src/main/resources/application.properties` — application properties; default name set.
- `app/meshingress-tool-bundle/pom.xml` — tool dependency bundle attached by the server.
- `architect/README.md` — project-specific developer/agent conventions and task tracking (highly recommended).

What to know about MCP (concrete patterns)
-----------------------------------------
- Protocol: JSON-RPC 2.0. Dispatcher expects `jsonrpc: "2.0"` and supports single requests and batches.
- Supported methods (as implemented): `initialize`, `notifications/initialized`, `ping`, `tools/list`, `tools/call`, and role-gated methods under `roles/tools/*` (check/register/update/delete/list/reload).
- Initialization: `initialize` returns `protocolVersion` (default constant `2025-11-25`) and `capabilities.tools.listChanged = true`.
- Tool call shape: POST `/mcp` body {"jsonrpc":"2.0","id":...,"method":"tools/call","params":{"name":"<namespace>.<tool>.<function>","arguments":{...}}}
- MCP WebSocket transport uses the same JSON-RPC methods at `meshingress.mcp.websocket.path` (default `/mcp/ws`); see `samples/mcp-websocket/README.md`.
- Tool integration points to implement or inspect:
  - `ToolRegistry#listPublicEnabledTools()` — dispatcher uses this to produce `tools/list`.
  - `ToolExecutor#execute(name, arguments, context)` — returns a `ToolExecutionResult` whose `toJson(objectMapper)` is returned to the client.
  - `McpCallContext` carries headers: authorization, X-Mcp-Role, Mcp-Session-Id, X-Request-Id.
  - `McpToolHandler` lives in `lib/meshingress-tool-api`; tool modules should depend on that module, not on `app/meshingress-server`.

Security & admin
----------------
- Endpoints `/mcp` and `/actuator/health` are permitted anonymous access (see `security/SecurityConfig.java`). Everything else requires authentication.
- Admin role detection: either `X-Mcp-Role: admin`, legacy `X-Mcp-Admin: true`, or Authorization header `Bearer <admin-token>`. Default token property: `meshingress.mcp.roles.admin-token`, falling back to `meshingress.mcp.admin-token` (default `dev-admin`). See `controller/roles/RoleAuthorizationService.java`.

Build, run and test (concrete commands)
--------------------------------------
- Build all modules (Windows):
  mvnw.cmd clean package
- Build/test only the server plus required upstream modules:
  mvnw.cmd -pl app/meshingress-server -am test
- Run locally (dev):
  mvnw.cmd -pl app/meshingress-server -am spring-boot:run
  or after packaging: java -jar app\meshingress-server\target\meshingress.jar
- Run tests:
  mvnw.cmd test
- Notes: the repository uses the bundled Maven wrapper; use `mvnw.cmd` on Windows or `./mvnw` on *nix. Java 25 is required.

Conventions and project-specific workflows
-----------------------------------------
- `architect/` is the authoritative place for human+agent planning, PRD, and lifecycle metadata. Read `architect/README.md` for naming, templates, and expected files.
- JSON-RPC errors use `JsonRpcResponses` + `JsonRpcErrorCodes` and `JsonRpcException` for control flow; follow that pattern for new RPC methods.
- Keep public RPC names stable once introduced; add new role-gated registry functionality under `roles/tools/*` via the roles controller/service classes.

Where agents should edit to add a tool
------------------------------------
1. Create a module under `toolspace/<name>` that depends on `dev.mrk.meshingress:meshingress-tool-api`.
2. Implement `McpToolHandler` and return a `McpToolDescriptor` from `descriptor()`.
3. Expose the handler as a Spring bean. Prefer a module-local Boot auto-configuration file under `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
4. Attach the module by adding it as a dependency of `app/meshingress-tool-bundle/pom.xml` (the server depends on this bundle).
5. Add MVC tests in `app/meshingress-server` that confirm `tools/list` and `tools/call` see the attached tool.

Quick JSON-RPC examples
-----------------------
- tools/list request:
  {"jsonrpc":"2.0","id":1,"method":"tools/list"}
- tools/call request (example):
  {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"helloworld.greeting.greet","arguments":{"name":"Meshingress"}}}

Where to look for missing pieces
--------------------------------
- Tool-author API classes live in `lib/meshingress-tool-api`. Server-only classes such as `ToolRegistry`, `ToolExecutor`, `RoleToolService`, and JSON-RPC dispatcher code live under `app/meshingress-server`.

Contact points for human reviewers
---------------------------------
- Use `architect/` entries to create a short plan before larger changes. Follow the `architect/README.md` templates.

Summary
-------
This file contained: quick architecture overview, files to read, concrete JSON-RPC and admin examples, exact build/run/test commands, and step-by-step guidance for implementing tools.

Last updated: `backup-2026-05-30-202858` This branch will serve as a restore point if needed. During the time period that I will not be available.

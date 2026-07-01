# Agent Task: Meshingress Property Implementation Registry Review

You are reviewing the Meshingress codebase to determine where each `meshingress.*` configuration property should be implemented, consumed, or validated.

## Context

Meshingress now has a typed Spring Boot configuration bean:

- `dev.mrk.meshingress.config.MeshingressProperties`
- Annotated with `@ConfigurationProperties(prefix = "meshingress")`
- Contains nested records for:
  - `Identity`
  - `Mcp.WebSocket`
  - `Tools.Registry`
  - `Dispatch`
  - `Security`
  - `Scopes`
  - `Audit`
  - `Secrets`

The project exposes MCP tools through attachable tool modules. Tool modules live under `toolspace/`, use the Meshingress tool SPI and annotations, and are attached to the server through Spring Boot auto-configuration. The tool SPI centers around `McpToolHandler`, `McpDispatchHandler`, and `DispatchExecutionResult`. Tool metadata and behavior are described through annotations such as `@McpTool`, `@McpToolMapping`, `@McpToolScopes`, `@McpConfigureMapping`, and `@McpSecret`.

## Objective

Review the codebase and produce a registry showing exactly where each property should affect runtime behavior.

Do **not** implement changes yet unless explicitly asked. This task is discovery and mapping.

Return a Markdown report named conceptually:

```txt
meshingress-property-implementation-registry.md
````

## Required Output Format

For each property, return a table row with:

| Property | Current Field | Intended Behavior | Existing Code Location | Implementation Target | Status | Notes |
| -------- | ------------- | ----------------- | ---------------------- | --------------------- | ------ | ----- |

Use these status values only:

```txt
implemented
partially-implemented
missing
blocked
unclear
```

Where:

* `Property`: full property key, for example `meshingress.dispatch.default-timeout`
* `Current Field`: exact Java accessor path, for example `MeshingressProperties.dispatch().defaultTimeout()`
* `Intended Behavior`: what the property should control
* `Existing Code Location`: existing class/file/method where related behavior already exists
* `Implementation Target`: class/file/method where this property should be consumed
* `Status`: one of the allowed status values
* `Notes`: risks, design questions, or suggested implementation detail

Also include a final section:

```md
## Recommended Implementation Order
```

Rank fields by dependency order and risk.

---

# Review Scope

Search these areas first:

```txt
app/meshingress-server
lib/meshingress-config
lib/meshingress-tool-api
lib/meshingress-tool-annotations
lib/meshingress-tool-framework
toolspace
```

Also search for:

```txt
@ConfigurationProperties
@EnableConfigurationProperties
@ConfigurationPropertiesScan
WebSocket
McpWebSocket
McpDispatchHandler
McpToolHandler
DispatchExecutionResult
McpTool
McpToolMapping
McpToolScopes
McpConfigureMapping
McpSecret
McpToolScope
AvailabilityDecision
ToolAvailabilityContext
```

---

# Property-by-Property Review Requirements

## identity

### `meshingress.identity.name`

Look for where server/application identity is exposed.

Check:

* startup logs
* health/info endpoints
* MCP initialize/capabilities response
* audit metadata
* tool call context metadata

Expected behavior:

* Used as the logical service name for this Meshingress runtime.
* Should not replace `spring.application.name`; it should be Meshingress-specific identity.

Implementation target candidates:

* server info endpoint
* MCP handshake/capabilities payload
* audit event builder
* `McpCallContext` enrichment, if such a builder exists

---

### `meshingress.identity.instance-id`

Look for runtime instance or node ID behavior.

Expected behavior:

* Distinguishes this running Meshingress node from other nodes.
* Should appear in audit records, logs, and possibly tool call context.

Implementation target candidates:

* startup banner/logging
* audit event metadata
* dispatcher context creation
* health/info endpoint

---

### `meshingress.identity.environment`

Look for environment/profile handling.

Expected behavior:

* Identifies `dev`, `test`, `prod`, etc.
* Should influence display and metadata, not silently override Spring profiles.

Implementation target candidates:

* health/info endpoint
* audit metadata
* security posture warnings for unsafe dev settings

---

### `meshingress.identity.public-base-url`

Look for places that generate URLs or advertise endpoints.

Expected behavior:

* Canonical external URL for this Meshingress instance.
* Should be used when reporting MCP endpoint URLs or server metadata.

Implementation target candidates:

* MCP discovery/capabilities response
* generated endpoint metadata
* logs or info endpoint

---

### `meshingress.identity.node-role`

Look for node role / runtime role concepts.

Expected behavior:

* Describes this node’s operational role, for example `edge-ingress`.
* Should be metadata unless the codebase has role-based behavior.

Implementation target candidates:

* health/info endpoint
* capabilities response
* audit metadata

---

## mcp.websocket

### `meshingress.mcp.websocket.enabled`

Look for WebSocket endpoint registration.

Expected behavior:

* When `false`, the MCP WebSocket endpoint should not be registered or should reject startup clearly.

Implementation target candidates:

* WebSocket configuration class
* MCP endpoint auto-configuration
* handler mapping registration

---

### `meshingress.mcp.websocket.path`

Look for hardcoded `/mcp/ws`.

Expected behavior:

* Controls the WebSocket MCP endpoint path.
* Existing hardcoded path should be replaced by this property.

Implementation target candidates:

* WebSocket handler registration
* MCP endpoint config
* tests that connect to `/mcp/ws`

---

### `meshingress.mcp.websocket.allowed-origins`

Look for CORS/origin configuration.

Expected behavior:

* Controls allowed WebSocket origins.
* `*` should be acceptable in dev but flagged as unsafe in prod.

Implementation target candidates:

* WebSocket handler registry
* origin handshake interceptor
* security config

---

### `meshingress.mcp.websocket.max-message-size`

Look for message buffer limits.

Expected behavior:

* Limits inbound MCP WebSocket message size.
* Should prevent oversized JSON-RPC/tool payloads.

Implementation target candidates:

* WebSocket container config
* message decoder
* transport-level handler

---

### `meshingress.mcp.websocket.send-timeout`

Look for send/async timeout behavior.

Expected behavior:

* Controls outbound WebSocket send timeout.
* Should prevent blocked sessions from hanging dispatch flow.

Implementation target candidates:

* WebSocket session send wrapper
* async executor
* transport send operation

---

### `meshingress.mcp.websocket.idle-timeout`

Look for session lifecycle management.

Expected behavior:

* Closes or expires idle MCP WebSocket sessions.
* Should be enforced at transport or session manager layer.

Implementation target candidates:

* WebSocket container config
* session registry
* heartbeat/keepalive logic

---

### `meshingress.mcp.websocket.require-auth`

Look for authentication checks on WebSocket handshake.

Expected behavior:

* When true, MCP WebSocket connections must authenticate before use.
* Should probably delegate to global security config.

Implementation target candidates:

* handshake interceptor
* security filter chain
* MCP session creation

---

## tools

### `meshingress.tools.registry.enabled`

Look for tool discovery / tool registry creation.

Expected behavior:

* Enables or disables the Meshingress tool registry.
* If disabled, `tools/list` and `tools/call` should return a controlled unavailable response.

Implementation target candidates:

* tool registry bean auto-configuration
* tool discovery service
* tools/list handler
* tools/call handler

---

### `meshingress.tools.registry.fail-on-duplicate-tool-id`

Look for duplicate tool ID validation.

Expected behavior:

* If true, startup should fail when two tools register the same ID.
* If false, behavior must be deterministic and logged.

Implementation target candidates:

* tool registry builder
* annotation scanner
* descriptor registration logic

---

### `meshingress.tools.registry.fail-on-invalid-tool-id`

Look for validation against `@McpTool` ID pattern.

Expected behavior:

* If true, invalid tool IDs should fail startup.
* If false, invalid tools should be skipped or disabled with a warning.

Implementation target candidates:

* annotation processing/scanning
* descriptor validation
* registry registration

---

### `meshingress.tools.registry.include-disabled`

Look for `enabled=false` in `@McpTool` or `@McpFunction`.

Expected behavior:

* Controls whether disabled tools/functions appear in registry metadata.
* Disabled tools should not be callable unless explicitly intended.

Implementation target candidates:

* descriptor discovery
* tools/list response
* tools/call guard

---

### `meshingress.tools.registry.scan-on-startup`

Look for startup scanning of tool beans.

Expected behavior:

* If true, tool registry is built during startup.
* If false, tool discovery may be lazy or manual.

Implementation target candidates:

* registry initialization
* application ready listener
* bean post-processor

---

### `meshingress.tools.registry.expose-private-tools`

Look for `ToolVisibility`.

Expected behavior:

* Controls whether private/non-public tools are exposed through listing and invocation.
* Should affect both `tools/list` and `tools/call`.

Implementation target candidates:

* descriptor filtering
* authorization layer
* tool invocation resolver

---

### `meshingress.tools.allow-list`

Look for tool filtering.

Expected behavior:

* If non-empty, only listed tool IDs/invocation names are exposed or callable.
* Should be applied before deny-list or after; document actual choice.

Implementation target candidates:

* registry filter
* tools/list
* tools/call resolver

---

### `meshingress.tools.deny-list`

Look for tool filtering.

Expected behavior:

* Listed tools are hidden and not callable.
* Deny-list should generally override allow-list.

Implementation target candidates:

* registry filter
* tools/list
* tools/call resolver

---

### `meshingress.tools.default-timeout`

Look for timeout resolution from `@McpConfigureMapping(timeoutMs = ...)`.

Expected behavior:

* Provides global fallback timeout when a tool/function does not declare one.
* Annotation-level timeout should override global default.

Implementation target candidates:

* dispatch config resolver
* tool invocation metadata builder
* executor timeout wrapper

---

### `meshingress.tools.default-audit`

Look for `@McpConfigureMapping(audit = ...)`.

Expected behavior:

* Provides default audit behavior when tool/function does not explicitly configure audit.
* Should integrate with `meshingress.audit.*`.

Implementation target candidates:

* invocation policy resolver
* audit event publisher
* tool metadata builder

---

### `meshingress.tools.default-debug-trace`

Look for `@McpConfigureMapping(debugTrace = ...)`.

Expected behavior:

* Controls default debug trace behavior for tool calls.
* Must not leak secrets or raw sensitive payloads.

Implementation target candidates:

* dispatch tracing
* invocation metadata resolver
* audit/logging layer

---

# Additional Required Analysis

## Cross-Cutting Flow

Identify the intended configuration flow:

```txt
application.properties
  -> MeshingressProperties
  -> server auto-configuration
  -> registry/transport/dispatch/security/audit/secret components
  -> MCP behavior at runtime
```

Return whether the codebase currently supports this flow.

## Bean Registration

Check whether `MeshingressProperties` is actually registered.

Look for:

```java
@EnableConfigurationProperties(MeshingressProperties.class)
@ConfigurationPropertiesScan
```

If neither exists, mark property bean registration as `missing`.

## Tool Access to Properties

Determine whether tools should receive `MeshingressProperties`.

Return one of:

```txt
safe-direct-injection
prefer-readonly-facade
do-not-inject
unclear
```

Guidance:

* Direct injection may be acceptable for server internals.
* Tool modules should preferably receive a read-only facade or narrower config view.
* Do not expose resolved secrets to tool modules.
* Tools requiring config access should declare an appropriate config-related scope.

## Risks to Identify

Call out:

* hardcoded `/mcp/ws`
* unsafe `allowed-origins=*`
* dev auth disabled
* missing timeout enforcement
* missing concurrency limits
* secret leakage through logs/errors
* mismatch between annotation-level config and property-level defaults
* duplicate or invalid tool IDs
* disabled/private tools becoming callable
* tools obtaining too much global configuration

## Final Deliverables

Produce:

1. A property implementation registry table.
2. A short architecture summary.
3. A list of missing components/classes.
4. A recommended implementation order.
5. A list of risky defaults that must be overridden before production.
6. A list of exact files/classes to modify next.

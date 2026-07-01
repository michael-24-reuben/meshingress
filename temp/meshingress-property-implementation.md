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

## dispatch

### `meshingress.dispatch.default-timeout`

Look for dispatch execution timeout.

Expected behavior:

* Global fallback timeout for calls that do not resolve a tool-specific timeout.

Implementation target candidates:

* dispatcher
* executor service wrapper
* tool invocation service

---

### `meshingress.dispatch.max-concurrent-calls`

Look for executor, semaphore, or concurrency limit.

Expected behavior:

* Limits concurrent tool invocations.
* Should reject or queue based on queue settings.

Implementation target candidates:

* dispatch executor
* semaphore guard
* tool call service

---

### `meshingress.dispatch.queue-capacity`

Look for dispatch queue.

Expected behavior:

* Limits pending tool calls waiting for execution.
* Prevents unbounded memory growth.

Implementation target candidates:

* thread pool executor
* bounded queue
* call scheduler

---

### `meshingress.dispatch.reject-when-saturated`

Look for saturation behavior.

Expected behavior:

* If true, reject calls when concurrency and queue are full.
* If false, document whether calls block, wait, or degrade.

Implementation target candidates:

* executor rejection policy
* dispatcher guard
* structured error response builder

---

### `meshingress.dispatch.include-stacktrace`

Look for exception handling.

Expected behavior:

* Controls whether stack traces are included in tool error responses.
* Should default false.

Implementation target candidates:

* error mapper
* `DispatchExecutionResult` builder
* MCP error response conversion

---

### `meshingress.dispatch.include-generated-at`

Look for `_meta.generatedAt`.

Expected behavior:

* Controls whether generated timestamps appear in dispatch metadata.
* Existing `DispatchExecutionResult` auto-populates `_meta.generatedAt`; determine whether this can currently be disabled or requires refactor.

Implementation target candidates:

* `DispatchExecutionResult`
* result mapper
* response post-processor

---

### `meshingress.dispatch.redact-errors`

Look for error sanitization.

Expected behavior:

* Redacts sensitive details from error messages before returning to clients.
* Should still preserve detailed logs internally if safe.

Implementation target candidates:

* exception mapper
* result builder
* audit/logger split

---

## security

### `meshingress.security.enabled`

Look for global security enable/disable.

Expected behavior:

* Master switch for Meshingress security enforcement.
* Should not disable basic safety checks unless explicitly documented.

Implementation target candidates:

* authorization service
* security filter chain
* tool invocation guard

---

### `meshingress.security.mode`

Look for dev/prod/test mode branching.

Expected behavior:

* Controls strictness profile.
* Should warn or fail if dev-unsafe settings are used in prod.

Implementation target candidates:

* startup validator
* security policy resolver
* environment check

---

### `meshingress.security.require-authentication`

Look for caller authentication.

Expected behavior:

* Requires authenticated caller before MCP session/tool invocation.

Implementation target candidates:

* WebSocket handshake
* MCP session creation
* tool call authorization

---

### `meshingress.security.require-tool-approval`

Look for approval workflow.

Expected behavior:

* Requires explicit approval before tool execution.
* If no approval system exists, mark as `blocked`.

Implementation target candidates:

* tool invocation guard
* approval service
* policy engine

---

### `meshingress.security.require-approval-for-privileged`

Look for privileged scope logic.

Expected behavior:

* Requires approval for tools/functions with privileged scopes.
* Should use `McpToolScope.requiresExplicitApproval()` or equivalent scope metadata.

Implementation target candidates:

* scope evaluator
* authorization service
* tool invocation guard

---

### `meshingress.security.require-approval-for-critical`

Look for critical risk logic.

Expected behavior:

* Requires approval for critical-risk scopes.
* Should use `RiskLevel.CRITICAL` from scope metadata.

Implementation target candidates:

* scope evaluator
* authorization service
* approval service

---

### `meshingress.security.deny-unknown-scopes`

Look for handling of missing/unrecognized scopes.

Expected behavior:

* Unknown scopes should fail closed when true.
* If false, unknown scopes should be logged clearly.

Implementation target candidates:

* scope parser
* descriptor validator
* authorization policy evaluator

---

### `meshingress.security.default-deny`

Look for authorization default decision.

Expected behavior:

* If no allow rule matches, deny invocation.
* Should affect all tool calls.

Implementation target candidates:

* policy evaluator
* authorization guard
* tools/call handler

---

## scopes

### `meshingress.scopes.audit-required-for-high-risk`

Look for `McpToolScope.requiresAudit()` or risk evaluation.

Expected behavior:

* Enforces audit for high/critical risk scopes.

Implementation target candidates:

* scope policy resolver
* audit decision builder
* invocation metadata resolver

---

### `meshingress.scopes.explicit-approval-for-critical`

Look for critical risk handling.

Expected behavior:

* Requires explicit approval for critical scopes.

Implementation target candidates:

* scope evaluator
* approval guard
* authorization service

---

### `meshingress.scopes.allow-shell-execute`

Look for `McpToolScope.SHELL_EXECUTE`.

Expected behavior:

* If false, tools requiring shell execution should not be callable.
* Prefer fail-fast at registry time plus runtime guard.

Implementation target candidates:

* registry validator
* authorization service
* tools/call guard

---

### `meshingress.scopes.allow-files-delete`

Look for `McpToolScope.FILES_DELETE`.

Expected behavior:

* If false, tools requiring file deletion should not be callable.
* Prefer fail-fast at registry time plus runtime guard.

Implementation target candidates:

* registry validator
* authorization service
* tools/call guard

---

### `meshingress.scopes.allow-network-inbound`

Look for `McpToolScope.NETWORK_INBOUND`.

Expected behavior:

* If false, tools requiring inbound network binding should not be callable.
* Since Meshingress itself binds a server socket, distinguish server transport from tool-requested inbound network scope.

Implementation target candidates:

* scope evaluator
* registry validator
* authorization service

---

## audit

### `meshingress.audit.enabled`

Look for audit event publishing.

Expected behavior:

* Master switch for audit event generation.

Implementation target candidates:

* audit service
* dispatch lifecycle hooks
* tool invocation wrapper

---

### `meshingress.audit.log-tool-calls`

Look for logging before tool invocation.

Expected behavior:

* Records that a tool call was requested.

Implementation target candidates:

* dispatch pre-hook
* tools/call handler
* audit event service

---

### `meshingress.audit.log-tool-results`

Look for logging after tool invocation.

Expected behavior:

* Records results when enabled.
* Must respect redaction and max length.

Implementation target candidates:

* dispatch post-hook
* result mapper
* audit event service

---

### `meshingress.audit.log-arguments`

Look for argument capture.

Expected behavior:

* Records call arguments when enabled.
* Must redact secrets and truncate large payloads.

Implementation target candidates:

* tools/call handler
* dispatch wrapper
* audit event builder

---

### `meshingress.audit.redact-secrets`

Look for redaction service.

Expected behavior:

* Redacts secret-looking values, known secret refs, or configured sensitive keys.

Implementation target candidates:

* audit serializer
* logging interceptor
* result/error mapper

---

### `meshingress.audit.storage`

Look for audit sink.

Expected behavior:

* `log` means emit to application logs.
* Future values may include file, jsonl, database, or external sink.

Implementation target candidates:

* audit sink abstraction
* log-backed audit writer
* future storage registry

---

### `meshingress.audit.max-argument-length`

Look for truncation logic.

Expected behavior:

* Truncates serialized argument payloads to prevent log bloat and sensitive overexposure.

Implementation target candidates:

* audit serializer
* payload sanitizer
* logger wrapper

---

## secrets

### `meshingress.secrets.enabled`

Look for secret resolution.

Expected behavior:

* Master switch for secret resolution.
* If false, tools requiring secrets should be unavailable or fail clearly.

Implementation target candidates:

* secret resolver
* tool configuration resolver
* availability validator

---

### `meshingress.secrets.allow-env`

Look for `env:` secret references.

Expected behavior:

* Controls whether `env:NAME` secret refs are allowed.

Implementation target candidates:

* secret resolver
* `@McpSecret` processing
* availability validation

---

### `meshingress.secrets.allow-file`

Look for `file:` secret references.

Expected behavior:

* Controls whether file-backed secrets are allowed.

Implementation target candidates:

* secret resolver
* secret provider registry
* availability validation

---

### `meshingress.secrets.allow-inline`

Look for inline literal secret references.

Expected behavior:

* Controls whether inline secret values are allowed.
* Should default false.

Implementation target candidates:

* secret resolver
* secret provider registry
* validation layer

---

### `meshingress.secrets.redaction-placeholder`

Look for redaction placeholder.

Expected behavior:

* Placeholder used when serializing redacted secret values.

Implementation target candidates:

* redaction service
* audit serializer
* debug trace serializer

---

### `meshingress.secrets.fail-on-missing`

Look for missing secret handling.

Expected behavior:

* If true, missing required secrets should fail startup or mark affected tools unavailable.
* If false, missing secrets should produce controlled runtime failure.

Implementation target candidates:

* secret resolver
* registry validator
* availability condition evaluator

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

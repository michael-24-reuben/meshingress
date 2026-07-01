# Meshingress Property Implementation Registry

## Property Registry

| Property | Current Field | Intended Behavior | Existing Code Location | Implementation Target | Status | Notes |
| -------- | ------------- | ----------------- | ---------------------- | --------------------- | ------ | ----- |
| meshingress.identity.name | MeshingressProperties.identity().name() | Logical Meshingress runtime name (not Spring app name). | InternalMcpController initializes serverInfo with hardcoded name. | InternalMcpController.initialize, info/health metadata builders, audit metadata. | missing | Replace hardcoded "meshingress" in serverInfo. Consider using in logs and audit meta. |
| meshingress.identity.instance-id | MeshingressProperties.identity().instanceId() | Unique node instance ID for logs/audit/context. | None found. | McpCallContext enrichment, audit event builder, health/info endpoint. | missing | No instance identity injected anywhere. |
| meshingress.identity.environment | MeshingressProperties.identity().environment() | Environment metadata (dev/test/prod) for display and warnings. | None found. | health/info endpoint, audit metadata, dev safety warnings. | missing | Does not alter Spring profiles. |
| meshingress.identity.public-base-url | MeshingressProperties.identity().publicBaseUrl() | Canonical external URL for endpoints/capabilities. | None found. | InternalMcpController.initialize serverInfo or capabilities, info endpoint. | missing | Startup banner prints URLs from server host/port only. |
| meshingress.identity.node-role | MeshingressProperties.identity().nodeRole() | Runtime role metadata (edge-ingress, etc.). | None found. | health/info endpoint, capabilities payload, audit metadata. | missing | Not referenced in logs. |
| meshingress.mcp.websocket.enabled | MeshingressProperties.mcp().websocket().enabled() | Enable/disable MCP WebSocket endpoint registration. | None found. | McpWebSocketConfig bean registration gate. | missing | WebSocket endpoint always registered. |
| meshingress.mcp.websocket.path | MeshingressProperties.mcp().websocket().path() | Configure WebSocket endpoint path. | McpWebSocketConfig @Value uses property; MeshingressApplication prints from @Value. | McpWebSocketConfig, MeshingressApplication, tests/samples. | partially-implemented | Path is used, but not through MeshingressProperties; samples/docs hardcode /mcp/ws. |
| meshingress.mcp.websocket.allowed-origins | MeshingressProperties.mcp().websocket().allowedOrigins() | Configure allowed WebSocket origins. | McpWebSocketConfig @Value uses property string. | McpWebSocketConfig originPatterns. | partially-implemented | Uses comma-separated string; MeshingressProperties expects list. No prod safety warning. |
| meshingress.mcp.websocket.max-message-size | MeshingressProperties.mcp().websocket().maxMessageSize() | Limit inbound WebSocket payload size. | McpWebSocketHandler checks maxMessageSize(). | McpWebSocketHandler. | implemented | Applies per message; no session-level close on overflow. |
| meshingress.mcp.websocket.send-timeout | MeshingressProperties.mcp().websocket().sendTimeout() | Control outbound send timeout. | None found. | WebSocket session send wrapper or server container config. | missing | No async send timeout handling. |
| meshingress.mcp.websocket.idle-timeout | MeshingressProperties.mcp().websocket().idleTimeout() | Close idle WebSocket sessions. | None found. | WebSocket container/session registry. | missing | No idle/session tracking. |
| meshingress.mcp.websocket.require-auth | MeshingressProperties.mcp().websocket().requireAuth() | Enforce auth on WS handshake/usage. | None found. | Handshake interceptor or SecurityConfig integration. | missing | WebSocket accepts unauthenticated clients; no role header parsing for WS. |
| meshingress.tools.registry.enabled | MeshingressProperties.tools().registry().enabled() | Enable/disable tool registry and tool endpoints. | ToolRegistry always constructed and used. | ToolRegistry bean conditional; ToolsMcpController/ToolExecutor guard. | missing | tools/list and tools/call always active. |
| meshingress.tools.registry.fail-on-duplicate-tool-id | MeshingressProperties.tools().registry().failOnDuplicateToolId() | Fail on duplicate tool ID at startup. | InMemoryToolRegistry.registerHandler throws on duplicate function/handler IDs. | InMemoryToolRegistry registerHandler, McpToolAnnotationScanner scanning behavior. | partially-implemented | Always fails on duplicates; no configurable "warn+deterministic" path. |
| meshingress.tools.registry.fail-on-invalid-tool-id | MeshingressProperties.tools().registry().failOnInvalidToolId() | Fail or skip invalid tool IDs. | InMemoryToolRegistry.check validates names; McpToolAnnotationScanner validates @McpTool. | InMemoryToolRegistry/McpToolAnnotationScanner with property-driven behavior. | partially-implemented | Always fails/throws; no skip or warn option. |
| meshingress.tools.registry.include-disabled | MeshingressProperties.tools().registry().includeDisabled() | Control listing of disabled tools/functions. | ToolRegistry filters enabled only; RolesMcpController allows includeDisabled param. | ToolRegistry listPublicEnabledFunctions/listPublicEnabledTools; ToolsMcpController. | missing | Property not used; only per-request param for role listing. |
| meshingress.tools.registry.scan-on-startup | MeshingressProperties.tools().registry().scanOnStartup() | Startup scanning of tool beans. | InMemoryToolRegistry scans tool handlers in constructor. | InMemoryToolRegistry initialization path. | missing | Always scans at startup; no lazy or manual option. |
| meshingress.tools.registry.expose-private-tools | MeshingressProperties.tools().registry().exposePrivateTools() | Expose private tools to public listing/call. | ToolRegistry filters to ToolVisibility.PUBLIC. | ToolRegistry list/find methods, ToolsMcpController. | missing | Private/admin tools never exposed to public endpoints. |
| meshingress.tools.allow-list | MeshingressProperties.tools().allowList() | Allow only listed tools to be exposed/called. | None found. | ToolRegistry filter or ToolsMcpController/ToolExecutor guard. | missing | No allow-list enforcement. |
| meshingress.tools.deny-list | MeshingressProperties.tools().denyList() | Deny listed tools from list/call. | None found. | ToolRegistry filter or ToolsMcpController/ToolExecutor guard. | missing | Deny-list not enforced. |
| meshingress.tools.default-timeout | MeshingressProperties.tools().defaultTimeout() | Default tool timeout if @McpConfigureMapping.timeoutMs is 0. | McpConfigureMapping defines timeoutMs but no runtime enforcement. | ToolExecutor or dispatch pipeline. | missing | No timeout enforcement at tool execution. |
| meshingress.tools.default-audit | MeshingressProperties.tools().defaultAudit() | Default audit behavior for tools without @McpConfigureMapping.audit. | McpConfigureMapping.audit exists; no audit pipeline. | Tool execution pipeline and audit service. | missing | No tool call audit yet. |
| meshingress.tools.default-debug-trace | MeshingressProperties.tools().defaultDebugTrace() | Default debug trace for tool calls. | McpConfigureMapping.debugTrace exists; no trace pipeline. | Tool execution pipeline/logger. | missing | Must avoid secret leakage if implemented. |
| meshingress.dispatch.default-timeout | MeshingressProperties.dispatch().defaultTimeout() | Default dispatch timeout for non-tool routes or tool fallback. | None found. | Dispatch execution pipeline or ToolExecutor wrapper. | missing | No dispatch timeouts. |
| meshingress.dispatch.max-concurrent-calls | MeshingressProperties.dispatch().maxConcurrentCalls() | Max concurrent calls across dispatch. | None found. | Dispatch executor with semaphore or thread pool. | missing | No concurrency limits. |
| meshingress.dispatch.queue-capacity | MeshingressProperties.dispatch().queueCapacity() | Queue capacity when saturated. | None found. | Dispatch executor queue. | missing | No queue implementation. |
| meshingress.dispatch.reject-when-saturated | MeshingressProperties.dispatch().rejectWhenSaturated() | Reject vs queue when saturated. | None found. | Dispatch executor saturation policy. | missing | No saturation handling. |
| meshingress.dispatch.include-stacktrace | MeshingressProperties.dispatch().includeStacktrace() | Include stack traces in error responses. | McpDispatcher returns generic errors; JsonRpcException data only. | JsonRpcResponses or error mapper. | missing | Stack traces are currently not returned. |
| meshingress.dispatch.include-generated-at | MeshingressProperties.dispatch().includeGeneratedAt() | Toggle generatedAt in tool responses. | DispatchExecutionResult always adds generatedAt. | DispatchExecutionResult or ToolExecutor result post-processor. | missing | Property not honored; always included. |
| meshingress.dispatch.redact-errors | MeshingressProperties.dispatch().redactErrors() | Redact sensitive details in error responses. | McpDispatcher error responses use exception message. | JsonRpcResponses/exception handling. | missing | No redaction layer. |
| meshingress.security.enabled | MeshingressProperties.security().enabled() | Enable Meshingress security enforcement. | SecurityConfig permits all; no auth checks. | SecurityConfig and role/approval services. | missing | Security is effectively disabled regardless of property. |
| meshingress.security.mode | MeshingressProperties.security().mode() | Mode (dev/prod) to control posture. | None found. | SecurityConfig and validation warnings. | missing | No posture adjustments. |
| meshingress.security.require-authentication | MeshingressProperties.security().requireAuthentication() | Require auth for MCP tool calls. | None found. | SecurityConfig, McpController, WebSocket handshake interceptor. | missing | MCP endpoints currently anonymous. |
| meshingress.security.require-tool-approval | MeshingressProperties.security().requireToolApproval() | Require approval for tool usage. | None found. | Tool execution guard, approval store. | missing | No approval workflow. |
| meshingress.security.require-approval-for-privileged | MeshingressProperties.security().requireApprovalForPrivileged() | Require approval for privileged scopes. | None found. | Tool scope enforcement layer. | missing | Scopes not enforced. |
| meshingress.security.require-approval-for-critical | MeshingressProperties.security().requireApprovalForCritical() | Require approval for critical scopes. | None found. | Tool scope enforcement layer. | missing | Scopes not enforced. |
| meshingress.security.deny-unknown-scopes | MeshingressProperties.security().denyUnknownScopes() | Deny tools with unknown scopes. | None found. | Tool scope validation in registry or call guard. | missing | No scope validation. |
| meshingress.security.default-deny | MeshingressProperties.security().defaultDeny() | Default deny if no rules. | None found. | Authorization policy engine. | missing | No policy engine. |
| meshingress.scopes.audit-required-for-high-risk | MeshingressProperties.scopes().auditRequiredForHighRisk() | Require audit for high-risk scopes. | None found. | Scope policy + audit integration. | missing | No risk policy. |
| meshingress.scopes.explicit-approval-for-critical | MeshingressProperties.scopes().explicitApprovalForCritical() | Require explicit approval for critical scopes. | None found. | Scope policy + approval store. | missing | No approval workflow. |
| meshingress.scopes.allow-shell-execute | MeshingressProperties.scopes().allowShellExecute() | Permit shell execution scopes. | None found. | Scope policy enforcement at tool call. | missing | No scope gating. |
| meshingress.scopes.allow-files-delete | MeshingressProperties.scopes().allowFilesDelete() | Permit file delete scopes. | None found. | Scope policy enforcement at tool call. | missing | No scope gating. |
| meshingress.scopes.allow-network-inbound | MeshingressProperties.scopes().allowNetworkInbound() | Permit inbound network scopes. | None found. | Scope policy enforcement at tool call. | missing | No scope gating. |
| meshingress.audit.enabled | MeshingressProperties.audit().enabled() | Enable audit pipeline for tool calls. | ToolRegistry audit events exist for registry changes. | Tool execution audit publisher. | missing | Only registry audit events are recorded. |
| meshingress.audit.log-tool-calls | MeshingressProperties.audit().logToolCalls() | Log tool call metadata. | None found. | Audit publisher or structured logger. | missing | No tool call logging beyond basic info logs. |
| meshingress.audit.log-tool-results | MeshingressProperties.audit().logToolResults() | Log tool call results. | None found. | Audit publisher or structured logger. | missing | No tool result logging. |
| meshingress.audit.log-arguments | MeshingressProperties.audit().logArguments() | Log tool arguments (with redaction). | None found. | Audit publisher + redaction. | missing | Avoid logging secrets. |
| meshingress.audit.redact-secrets | MeshingressProperties.audit().redactSecrets() | Redact secrets from audit logs. | None found. | Audit redactor + secret resolver. | missing | No redaction in logs. |
| meshingress.audit.storage | MeshingressProperties.audit().storage() | Select audit storage backend (log/jsonl/db). | None found. | Audit sink implementation(s). | missing | No audit storage subsystem. |
| meshingress.audit.max-argument-length | MeshingressProperties.audit().maxArgumentLength() | Truncate logged arguments to a max length. | None found. | Audit logger. | missing | No argument logging. |
| meshingress.secrets.enabled | MeshingressProperties.secrets().enabled() | Enable secret resolution. | McpSecret annotation exists but no resolver. | Secret resolver service. | missing | Only validation of secret annotation present. |
| meshingress.secrets.allow-env | MeshingressProperties.secrets().allowEnv() | Allow env-based secret resolution. | None found. | Secret resolver. | missing | No secret source handling. |
| meshingress.secrets.allow-file | MeshingressProperties.secrets().allowFile() | Allow file-based secret resolution. | None found. | Secret resolver. | missing | No secret source handling. |
| meshingress.secrets.allow-inline | MeshingressProperties.secrets().allowInline() | Allow inline secret resolution. | None found. | Secret resolver. | missing | No secret source handling. |
| meshingress.secrets.redaction-placeholder | MeshingressProperties.secrets().redactionPlaceholder() | Placeholder used for redaction outputs. | None found. | Audit redactor / error redaction. | missing | No redaction pipeline. |
| meshingress.secrets.fail-on-missing | MeshingressProperties.secrets().failOnMissing() | Fail tool call on missing secrets. | None found. | Secret resolver or invocation guard. | missing | No secret lookup enforcement. |

## Architecture Summary

Meshingress loads `MeshingressProperties` via `@EnableConfigurationProperties` in `MeshingressApplication`, but most runtime behavior is still driven by hardcoded defaults or local `@Value` usage. The MCP HTTP and WebSocket transports dispatch requests, but tool execution, authorization, audit, secrets, and dispatch controls are not yet wired to configuration. Tool registry behavior is mostly fixed (fail on duplicates/invalid IDs, public-only exposure) with no property overrides. WebSocket message size is enforced via `MeshingressProperties`, while other WebSocket settings are unused.

## Cross-Cutting Flow Status

The flow from `application.properties -> MeshingressProperties -> server auto-configuration -> runtime behavior` is only partially supported. The properties bean is registered, but only a small subset of fields (WebSocket max message size) are actually consumed by runtime components.

## Bean Registration

`MeshingressProperties` is registered via `@EnableConfigurationProperties(MeshingressProperties.class)` in `MeshingressApplication`.

## Tool Access to Properties

prefer-readonly-facade

Rationale: server internals can inject `MeshingressProperties`, but tool modules should not receive full config (especially secrets). Provide a narrowed interface or resolved policy view instead.

## Missing Components / Classes

- WebSocket handshake interceptor or session configurator for auth, idle timeout, send timeout, and headers.
- Tool registry policy filter (allow/deny list, include disabled/private) and startup scan controls.
- Dispatch execution controller with concurrency limits, queueing, timeouts, and saturation policy.
- Audit pipeline for tool calls and results, including redaction and storage sink(s).
- Secret resolver with env/file/inline sources and redaction support.
- Authorization service (role/admin token verification, tool approval gating, scope enforcement).

## Recommended Implementation Order

1. Wire `MeshingressProperties` into transport and tool layers (WebSocket config, tool registry, executor).
2. Add dispatch executor with timeout + concurrency/queueing (use dispatch + tools defaults).
3. Implement allow/deny list and visibility handling in `ToolRegistry` / `ToolsMcpController`.
4. Add authorization service and scope enforcement (security + scopes properties).
5. Implement audit pipeline and redaction (audit + secrets properties).
6. Add secrets resolver and integrate with `@McpSecret` usage.
7. Update server metadata (identity/public-base-url) and capability payloads.

## Risky Defaults Before Production

- `meshingress.mcp.websocket.allowed-origins=*` allows any origin.
- `meshingress.security.require-authentication=false` and `SecurityConfig` permits all requests.
- No timeout enforcement for tool or dispatch execution.
- No concurrency limits or saturation handling for dispatch.
- No audit logging for tool calls; no redaction layer for secrets.
- Private/disabled tools are not gated by allow/deny policy (only by visibility defaults).

## Files / Classes to Modify Next

- `app/meshingress-server/src/main/java/dev/mrk/meshingress/config/McpWebSocketConfig.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/McpWebSocketHandler.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/internal/InternalMcpController.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/tools/ToolsMcpController.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/InMemoryToolRegistry.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/DefaultToolExecutor.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/security/SecurityConfig.java`
- `lib/meshingress-tool-framework/src/main/java/dev/mrk/meshingress/tools/framework/scanner/McpToolAnnotationScanner.java`
- `lib/meshingress-route-framework/src/main/java/dev/mrk/meshingress/route/framework/dispatch/McpHandlerMethodInvoker.java`
- New: audit service, secret resolver, authorization service, dispatch executor


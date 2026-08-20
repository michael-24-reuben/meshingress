# Application Properties TODO List
This checklist tracks the planned `meshingress.*` configuration surface while `MeshingressProperties` is being implemented.

The properties are grouped around Meshingress runtime identity, MCP transport, tool discovery, dispatch limits, security policy, scope enforcement, audit behavior, and secret resolution. The current `MeshingressProperties` bean already models these groups as nested records under `@ConfigurationProperties(prefix = "meshingress")`, so each TODO item should map directly to a typed property field. :contentReference[oaicite:0]{index=0}

## Implementation Intent

### identity
Defines how this Meshingress node identifies itself to logs, clients, tools, audit records, and future clustered/runtime views.

This should be treated as runtime metadata, not security identity. The `instance-id` should distinguish one running node from another, while `name`, `environment`, and `node-role` describe the service’s operational purpose.

- [ ] meshingress.identity.name=`meshingress`
- [ ] meshingress.identity.instance-id=`dev-node-01`
- [ ] meshingress.identity.environment=`dev`
- [ ] meshingress.identity.public-base-url=`http://100.121.15.11:4737`
- [ ] meshingress.identity.node-role=`edge-ingress`

### mcp
Controls the MCP transport endpoint.

The WebSocket settings should govern whether the MCP endpoint is enabled, where it is mounted, what origins may connect, and what resource limits apply to each connection.
Meshingress tool modules are intended to expose MCP tools through the server, so transport configuration is part of the public control-plane boundary. :contentReference[oaicite:1]{index=1}

- [ ] meshingress.mcp.websocket.enabled=`true`
- [x] meshingress.mcp.websocket.path=`/mcp/ws`
- [x] meshingress.mcp.websocket.allowed-origins=`*`
- [ ] meshingress.mcp.websocket.max-message-size=`1MB`
- [ ] meshingress.mcp.websocket.send-timeout=`30s`
- [ ] meshingress.mcp.websocket.idle-timeout=`5m`
- [ ] meshingress.mcp.websocket.require-auth=`false`

### tools
Controls tool registry behavior.

These properties should decide how strict startup discovery is, whether disabled/private tools are visible, and whether global defaults apply when individual tools do not specify timeout, audit, or debug settings. Tool modules already use annotations such as `@McpTool`, `@McpToolScopes`, and `@McpConfigureMapping`, so this group should act as the server-side policy layer over annotated metadata. :contentReference[oaicite:2]{index=2}

- [ ] meshingress.tools.registry.enabled=`true`
- [ ] meshingress.tools.registry.fail-on-duplicate-tool-id=`true`
- [ ] meshingress.tools.registry.fail-on-invalid-tool-id=`true`
- [ ] meshingress.tools.registry.include-disabled=`false`
- [ ] meshingress.tools.registry.scan-on-startup=`true`
- [ ] meshingress.tools.registry.expose-private-tools=`false`
- [ ] meshingress.tools.allow-list=``
- [ ] meshingress.tools.deny-list=``
- [ ] meshingress.tools.default-timeout=`30s`
- [ ] meshingress.tools.default-audit=`true`
- [ ] meshingress.tools.default-debug-trace=`false`

### dispatch
Controls execution safety around tool calls.

This group should protect the runtime from unbounded concurrency, queue growth, long-running calls, and excessive error detail leakage. It should be consumed by the dispatcher before invoking tool handlers.

- [ ] meshingress.dispatch.default-timeout=`30s`
- [ ] meshingress.dispatch.max-concurrent-calls=`32`
- [ ] meshingress.dispatch.queue-capacity=`256`
- [ ] meshingress.dispatch.reject-when-saturated=`true`
- [ ] meshingress.dispatch.include-stacktrace=`false`
- [ ] meshingress.dispatch.include-generated-at=`true`
- [ ] meshingress.dispatch.redact-errors=`true`

### security
Defines global authorization posture.

This group should answer: is security enabled, does the caller need authentication, do tool calls require approval, and should unknown or missing policy decisions fail closed.

- [ ] meshingress.security.enabled=`true`
- [ ] meshingress.security.mode=`dev`
- [ ] meshingress.security.require-authentication=`false`
- [ ] meshingress.security.require-tool-approval=`false`
- [ ] meshingress.security.require-approval-for-privileged=`true`
- [ ] meshingress.security.require-approval-for-critical=`true`
- [ ] meshingress.security.deny-unknown-scopes=`true`
- [ ] meshingress.security.default-deny=`true`

### scopes
Controls how declared tool scopes are enforced.

This layer should connect config policy to the existing `McpToolScope` model. The scope model already carries risk level, privileged status, audit recommendation, and helper methods for explicit approval and audit requirements. :contentReference[oaicite:3]{index=3}

- [ ] meshingress.scopes.audit-required-for-high-risk=`true`
- [ ] meshingress.scopes.explicit-approval-for-critical=`true`
- [ ] meshingress.scopes.allow-shell-execute=`false`
- [ ] meshingress.scopes.allow-files-delete=`false`
- [ ] meshingress.scopes.allow-network-inbound=`true`

### audit
Controls what gets recorded for tool execution.

Use this to decide whether tool calls, arguments, results, and redacted payloads are written to logs or a future audit sink. This should stay separate from ordinary application logging because tool calls may contain sensitive user input, file paths, secrets, or external API material.

- [ ] meshingress.audit.enabled=`true`
- [ ] meshingress.audit.log-tool-calls=`true`
- [ ] meshingress.audit.log-tool-results=`false`
- [ ] meshingress.audit.log-arguments=`true`
- [ ] meshingress.audit.redact-secrets=`true`
- [ ] meshingress.audit.storage=`log`
- [ ] meshingress.audit.max-argument-length=`8192`

### secrets
Controls which secret reference mechanisms are allowed.

Tool-level configuration already supports declaring secrets through `@McpConfigureMapping`, with secret refs such as environment-backed values. :contentReference[oaicite:4]{index=4} This property group should decide whether env/file/inline secret providers are enabled and how missing or redacted secrets behave.

- [ ] meshingress.secrets.enabled=`true`
- [ ] meshingress.secrets.allow-env=`true`
- [ ] meshingress.secrets.allow-file=`false`
- [ ] meshingress.secrets.allow-inline=`false`
- [ ] meshingress.secrets.redaction-placeholder=`****`
- [ ] meshingress.secrets.fail-on-missing=`true`


## Implementation Notes

- Keep this configuration in a reusable module such as `lib/meshingress-config`.
- Register the bean once from the server using `@EnableConfigurationProperties(MeshingressProperties.class)` or `@ConfigurationPropertiesScan`.
- Let tools receive this bean only if they have a legitimate `CONFIG_READ`-style need.
- Avoid exposing raw secrets through `MeshingressProperties`; expose secret policy and secret refs, not resolved secret values.
- Treat `allowed-origins=*`, `require-auth=false`, and `mode=dev` as development-only defaults.

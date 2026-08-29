# Nextcloud MCP Tool Runtime SDK Manual

Module: `nextcloud-sdk-tool-runtime`  
Artifact: `io.github.uriakleahcim.nextcloud:nextcloud-sdk-tool-runtime:0.0.1-SNAPSHOT`  
JPMS Module: `io.github.uriakleahcim.nextcloud.tool.runtime`

---

## Table of Contents

1. [Overview](#overview)
2. [Module Architecture & Encapsulation](#module-architecture--encapsulation)
3. [Maven Dependency Declaration](#maven-dependency-declaration)
4. [API Reference & Callable Blocks](#api-reference--callable-blocks)
   - [Tool Dispatcher (`ToolDispatcher`)](#tool-dispatcher)
   - [Tool Registry (`ToolRegistry`, `InMemoryToolRegistry`, `ToolRegistration`)](#tool-registry)
   - [Policy Interceptors (`ToolPolicyInterceptor`, `DefaultToolPolicyInterceptor`, `ToolPolicyDecision`)](#policy-interceptors)
   - [Argument Validation & Mapping (`ToolArgumentValidator`, `ToolArgumentMapper`)](#argument-validation--mapping)
   - [Runtime Execution Context (`ToolRuntimeContext`)](#runtime-execution-context)
5. [Complete SDK Usage Snippets](#complete-sdk-usage-snippets)

---

## Overview

The `nextcloud-sdk-tool-runtime` module provides the tool execution engine. It handles in-memory tool registration, input schema validation, security and rate-limiting policy interception, invocation routing, error normalization, and audit event emission.

---

## Module Architecture & Encapsulation

```java
module io.github.uriakleahcim.nextcloud.tool.runtime {
    requires transitive io.github.uriakleahcim.nextcloud.core;
    requires transitive io.github.uriakleahcim.nextcloud.tool.api;
    requires transitive io.github.uriakleahcim.nextcloud.security;
    requires com.fasterxml.jackson.databind;

    exports io.github.uriakleahcim.nextcloud.tool.runtime;

    opens io.github.uriakleahcim.nextcloud.tool.runtime to com.fasterxml.jackson.databind;
}
```

---

## Maven Dependency Declaration

```xml
<dependency>
    <groupId>io.github.uriakleahcim.nextcloud</groupId>
    <artifactId>nextcloud-sdk-tool-runtime</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

---

## API Reference & Callable Blocks

### Tool Dispatcher

#### [`ToolDispatcher`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-runtime/src/main/java/org/mcp/nextcloud/tool/runtime/ToolDispatcher.java)
- **Constructors**:
  - `new ToolDispatcher(ToolRegistry registry)`
  - `new ToolDispatcher(ToolRegistry registry, ToolArgumentValidator validator, ToolPolicyInterceptor policyInterceptor, ToolAuditSink auditSink)`
- **Methods**:
  - `List<ToolDescriptor> listTools()`: Returns all registered tool descriptors.
  - `ToolResult invoke(ToolId toolId, Map<String, Object> arguments, ToolRuntimeContext runtimeContext)`: Executes a tool through validation, policy evaluation, execution, and audit logging pipelines.

---

### Tool Registry

#### [`ToolRegistry`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-runtime/src/main/java/org/mcp/nextcloud/tool/runtime/ToolRegistry.java)
- `ToolRegistration register(ToolRegistration registration)`
- `Optional<ToolRegistration> find(ToolId toolId)`
- `List<ToolRegistration> list()`

#### [`InMemoryToolRegistry`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-runtime/src/main/java/org/mcp/nextcloud/tool/runtime/InMemoryToolRegistry.java)
- Thread-safe, synchronized implementation of `ToolRegistry`.

#### [`ToolRegistration`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-runtime/src/main/java/org/mcp/nextcloud/tool/runtime/ToolRegistration.java)
- `new ToolRegistration(ToolDescriptor descriptor, ToolHandler handler)`

---

### Policy Interceptors

#### [`ToolPolicyInterceptor`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-runtime/src/main/java/org/mcp/nextcloud/tool/runtime/ToolPolicyInterceptor.java)
- `ToolPolicyDecision evaluate(ToolDescriptor descriptor, PrincipalContext context)`

#### [`DefaultToolPolicyInterceptor`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-runtime/src/main/java/org/mcp/nextcloud/tool/runtime/DefaultToolPolicyInterceptor.java)
- Evaluates principal permissions, required tool scopes, and account accessibility.

#### [`ToolPolicyDecision`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-runtime/src/main/java/org/mcp/nextcloud/tool/runtime/ToolPolicyDecision.java)
- `static ToolPolicyDecision allow()`
- `static ToolPolicyDecision deny(String reason)`
- `boolean allowed()`, `String reason()`

---

### Argument Validation & Mapping

#### [`ToolArgumentValidator`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-runtime/src/main/java/org/mcp/nextcloud/tool/runtime/ToolArgumentValidator.java)
- `ToolValidationResult validate(ToolDescriptor descriptor, Map<String, Object> arguments)`: Checks required arguments, parameter data types (`STRING`, `INTEGER`, etc.), and allowed enumerated values.

---

### Runtime Execution Context

#### [`ToolRuntimeContext`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-runtime/src/main/java/org/mcp/nextcloud/tool/runtime/ToolRuntimeContext.java)
- `new ToolRuntimeContext(PrincipalContext principalContext, Map<String, Object> attributes)`

---

## Complete SDK Usage Snippets

### 1. Registering and Dispatching Tools
```java
import java.util.List;
import java.util.Map;
import java.util.Set;
import id.io.github.uriakleahcim.nextcloud.core.AccountId;
import id.io.github.uriakleahcim.nextcloud.core.InvocationId;
import id.io.github.uriakleahcim.nextcloud.core.PrincipalId;
import id.io.github.uriakleahcim.nextcloud.core.ToolId;
import io.github.uriakleahcim.nextcloud.security.Principal;
import io.github.uriakleahcim.nextcloud.security.PrincipalContext;
import io.github.uriakleahcim.nextcloud.security.Scopes;
import io.github.uriakleahcim.nextcloud.tool.api.ToolDescriptor;
import io.github.uriakleahcim.nextcloud.tool.api.ToolInputSchema;
import io.github.uriakleahcim.nextcloud.tool.api.ToolOutputSchema;
import io.github.uriakleahcim.nextcloud.tool.api.ToolParameter;
import io.github.uriakleahcim.nextcloud.tool.api.ToolResult;
import io.github.uriakleahcim.nextcloud.tool.api.ToolSecurity;
import io.github.uriakleahcim.nextcloud.tool.api.ToolValueType;
import io.github.uriakleahcim.nextcloud.tool.runtime.InMemoryToolRegistry;
import io.github.uriakleahcim.nextcloud.tool.runtime.ToolDispatcher;
import io.github.uriakleahcim.nextcloud.tool.runtime.ToolRegistration;
import io.github.uriakleahcim.nextcloud.tool.runtime.ToolRuntimeContext;

// 1. Initialize Registry and Dispatcher
InMemoryToolRegistry registry = new InMemoryToolRegistry();
ToolDispatcher dispatcher = new ToolDispatcher(registry);

// 2. Register a Tool
ToolId toolId = new ToolId("nextcloud_echo");
ToolDescriptor descriptor = new ToolDescriptor(
        toolId,
        "Echo",
        "Echoes the provided message",
        new ToolInputSchema(List.of(
                ToolParameter.required("msg", ToolValueType.STRING, "Message text")
        ), false),
        ToolOutputSchema.object(),
        new ToolSecurity(Set.of("nextcloud.files.read"), false),
        Map.of()
);

registry.register(new ToolRegistration(descriptor, invocation -> {
    String msg = (String) invocation.arguments().get("msg");
    return ToolResult.ok(Map.of("echo", msg));
}));

// 3. Dispatch Invocation with Principal Context
Principal principal = new Principal(new PrincipalId("user-1"), Set.of(Scopes.Files.READ), Set.of(), false);
PrincipalContext principalContext = new PrincipalContext(new InvocationId("inv-101"), principal, new AccountId("temporary"));
ToolRuntimeContext runtimeContext = new ToolRuntimeContext(principalContext, Map.of());

ToolResult result = dispatcher.invoke(toolId, Map.of("msg", "Antigravity"), runtimeContext);

if (result.success()) {
    System.out.println("Execution Success: " + result.structuredContent());
} else {
    System.err.println("Execution Denied/Failed: " + result.error().message());
}
```

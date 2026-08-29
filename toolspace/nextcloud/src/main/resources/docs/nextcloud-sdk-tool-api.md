# Nextcloud MCP Tool API SDK Manual

Module: `nextcloud-sdk-tool-api`  
Artifact: `io.github.uriakleahcim.nextcloud:nextcloud-sdk-tool-api:0.0.1-SNAPSHOT`  
JPMS Module: `io.github.uriakleahcim.nextcloud.tool.api`

---

## Table of Contents

1. [Overview](#overview)
2. [Module Architecture & Encapsulation](#module-architecture--encapsulation)
3. [Maven Dependency Declaration](#maven-dependency-declaration)
4. [API Reference & Callable Blocks](#api-reference--callable-blocks)
   - [Tool Handler SPI (`ToolHandler`)](#tool-handler-spi)
   - [Tool Invocations & Context (`ToolInvocation`, `ToolInvocationContext`)](#tool-invocations--context)
   - [Tool Results & Content Blocks (`ToolResult`, `ToolContent`)](#tool-results--content-blocks)
   - [Tool Schema Definitions (`ToolDescriptor`, `ToolInputSchema`, `ToolParameter`, `ToolValueType`)](#tool-schema-definitions)
   - [Tool Security Metadata (`ToolSecurity`)](#tool-security-metadata)
5. [Complete SDK Usage Snippets](#complete-sdk-usage-snippets)

---

## Overview

The `nextcloud-sdk-tool-api` module defines the core Service Provider Interfaces (SPI), tool descriptors, parameter schemas, execution invocation contexts, and structured content response models for building MCP capability tools.

---

## Module Architecture & Encapsulation

```java
module io.github.uriakleahcim.nextcloud.tool.api {
    requires transitive io.github.uriakleahcim.nextcloud.core;

    exports io.github.uriakleahcim.nextcloud.tool.api;
}
```

---

## Maven Dependency Declaration

```xml
<dependency>
    <groupId>io.github.uriakleahcim.nextcloud</groupId>
    <artifactId>nextcloud-sdk-tool-api</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

---

## API Reference & Callable Blocks

### Tool Handler SPI

#### [`ToolHandler`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-api/src/main/java/org/mcp/nextcloud/tool/api/ToolHandler.java)
- **Description**: Functional interface for executing an MCP tool.
- **Method**:
  - `ToolResult invoke(ToolInvocation invocation) throws Exception`

---

### Tool Invocations & Context

#### [`ToolInvocation`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-api/src/main/java/org/mcp/nextcloud/tool/api/ToolInvocation.java)
- **Components**:
  - `ToolId toolId()`: Identifier of the tool being executed.
  - `ToolInvocationContext context()`: Execution context.
  - `Map<String, Object> arguments()`: Immutable map of parsed input arguments.

#### [`ToolInvocationContext`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-api/src/main/java/org/mcp/nextcloud/tool/api/ToolInvocationContext.java)
- **Components**:
  - `InvocationId invocationId()`: Unique ID for this invocation.
  - `AccountId accountId()`: Target account ID (optional).
  - `String principalId()`: Calling principal ID.
  - `Map<String, Object> attributes()`: Contextual metadata and security tokens.

---

### Tool Results & Content Blocks

#### [`ToolResult`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-api/src/main/java/org/mcp/nextcloud/tool/api/ToolResult.java)
- **Components**: `boolean success()`, `List<ToolContent> content()`, `Object structuredContent()`, `ErrorResult error()`, `Map<String, Object> metadata()`.
- **Factory Methods**:
  - `static ToolResult ok(Object structuredContent)`: Creates successful result wrapping structured content.
  - `static ToolResult ok(List<ToolContent> content, Object structuredContent)`: Creates result with explicit content blocks and structured payload.
  - `static ToolResult failed(ErrorResult error)`: Creates failure result with error metadata.

#### [`ToolContent`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-api/src/main/java/org/mcp/nextcloud/tool/api/ToolContent.java)
- **Components**: `String type()`, `Object value()`, `Map<String, Object> metadata()`.
- **Factory Methods**:
  - `static ToolContent text(String text)`: Text content block (`type="text"`).
  - `static ToolContent json(Object value)`: JSON content block (`type="json"`).

---

### Tool Schema Definitions

#### [`ToolDescriptor`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-api/src/main/java/org/mcp/nextcloud/tool/api/ToolDescriptor.java)
- **Components**:
  - `ToolId id()`: Unique tool ID.
  - `String name()`: Human-readable name.
  - `String description()`: Detailed description for LLM prompting.
  - `ToolInputSchema inputSchema()`: Parameters definition.
  - `ToolOutputSchema outputSchema()`: Output shape definition.
  - `ToolSecurity security()`: Required scopes and risk classification.
  - `Map<String, Object> metadata()`: Custom tags and category mappings.

#### [`ToolInputSchema`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-api/src/main/java/org/mcp/nextcloud/tool/api/ToolInputSchema.java)
- `new ToolInputSchema(List<ToolParameter> parameters, boolean additionalProperties)`
- `static ToolInputSchema empty()`
- `Map<String, ToolParameter> parametersByName()`

#### [`ToolParameter`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-api/src/main/java/org/mcp/nextcloud/tool/api/ToolParameter.java)
- `static ToolParameter required(String name, ToolValueType type, String description)`
- `static ToolParameter optional(String name, ToolValueType type, String description)`

#### [`ToolValueType`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-api/src/main/java/org/mcp/nextcloud/tool/api/ToolValueType.java)
- Values: `STRING`, `INTEGER`, `NUMBER`, `BOOLEAN`, `ARRAY`, `OBJECT`.

---

### Tool Security Metadata

#### [`ToolSecurity`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-api/src/main/java/org/mcp/nextcloud/tool/api/ToolSecurity.java)
- `new ToolSecurity(Set<String> requiredScopes, boolean destructive)`
- `static ToolSecurity none()`

---

## Complete SDK Usage Snippets

### 1. Defining a Custom MCP Tool Descriptor
```java
import java.util.List;
import java.util.Map;
import java.util.Set;
import id.io.github.uriakleahcim.nextcloud.core.ToolId;
import io.github.uriakleahcim.nextcloud.tool.api.ToolDescriptor;
import io.github.uriakleahcim.nextcloud.tool.api.ToolInputSchema;
import io.github.uriakleahcim.nextcloud.tool.api.ToolOutputSchema;
import io.github.uriakleahcim.nextcloud.tool.api.ToolParameter;
import io.github.uriakleahcim.nextcloud.tool.api.ToolSecurity;
import io.github.uriakleahcim.nextcloud.tool.api.ToolValueType;

ToolDescriptor descriptor = new ToolDescriptor(
        new ToolId("nextcloud_files_download"),
        "Download File",
        "Downloads a file from Nextcloud storage as base64 or raw text.",
        new ToolInputSchema(List.of(
                ToolParameter.required("path", ToolValueType.STRING, "Remote file path to download"),
                ToolParameter.optional("account_id", ToolValueType.STRING, "Configured account identifier")
        ), false),
        ToolOutputSchema.object(),
        new ToolSecurity(Set.of("nextcloud.files.read"), false),
        Map.of("category", "files")
);

System.out.println("Tool: " + descriptor.name() + " (" + descriptor.id().value() + ")");
```

### 2. Implementing a Functional Tool Handler
```java
import java.util.Map;
import result.io.github.uriakleahcim.nextcloud.core.ErrorResult;
import io.github.uriakleahcim.nextcloud.tool.api.ToolHandler;
import io.github.uriakleahcim.nextcloud.tool.api.ToolResult;

ToolHandler handler = invocation -> {
    String path = (String) invocation.arguments().get("path");
    if (path == null || path.isBlank()) {
        return ToolResult.failed(new ErrorResult("invalid_argument", "Parameter 'path' is required", Map.of()));
    }
    
    // Perform operation
    Map<String, Object> payload = Map.of("path", path, "content", "Hello MCP World!");
    return ToolResult.ok(payload);
};
```

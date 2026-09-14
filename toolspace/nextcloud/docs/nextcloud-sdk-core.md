# Nextcloud MCP Core SDK Manual

Module: `nextcloud-sdk-core`  
Artifact: `io.github.uriakleahcim.nextcloud:nextcloud-sdk-core:0.0.1-SNAPSHOT`  
JPMS Module: `io.github.uriakleahcim.nextcloud.core`

---

## Table of Contents

1. [Overview](#overview)
2. [Module Architecture & Encapsulation](#module-architecture--encapsulation)
3. [Maven Dependency Declaration](#maven-dependency-declaration)
4. [API Reference & Callable Blocks](#api-reference--callable-blocks)
   - [Identity Types (`io.github.uriakleahcim.nextcloud.core.id`)](#identity-types-orgmcpnextcloudcoreid)
   - [Result Models (`io.github.uriakleahcim.nextcloud.core.result`)](#result-models-orgmcpnextcloudcoreresult)
   - [Exception Hierarchy (`io.github.uriakleahcim.nextcloud.core.error`)](#exception-hierarchy-orgmcpnextcloudcoreerror)
   - [Utility Primitives (`io.github.uriakleahcim.nextcloud.core.util`)](#utility-primitives-orgmcpnextcloudcoreutil)
5. [Complete SDK Usage Snippets](#complete-sdk-usage-snippets)

---

## Overview

The `nextcloud-sdk-core` module provides foundational types, immutable value records, structured error representations, precondition validators, and security masking utilities utilized across all Nextcloud MCP libraries, tool implementations, server endpoints, and CLI commands.

It contains zero framework runtime dependencies (such as Spring or HTTP frameworks) and only requires SLF4J for logging.

---

## Module Architecture & Encapsulation

```java
module io.github.uriakleahcim.nextcloud.core {
    requires org.slf4j;

    exports io.github.uriakleahcim.nextcloud.core.error;
    exports io.github.uriakleahcim.nextcloud.core.id;
    exports io.github.uriakleahcim.nextcloud.core.result;
    exports io.github.uriakleahcim.nextcloud.core.util;
}
```

- **Exported Packages**:
  - `io.github.uriakleahcim.nextcloud.core.id`: Strongly-typed identifier records.
  - `io.github.uriakleahcim.nextcloud.core.result`: Standard operation results, errors, pagination, and progress models.
  - `io.github.uriakleahcim.nextcloud.core.error`: Base runtime exceptions and API error abstractions.
  - `io.github.uriakleahcim.nextcloud.core.util`: Argument validation and secret masking utilities.

---

## Maven Dependency Declaration

```xml
<dependency>
    <groupId>io.github.uriakleahcim.nextcloud</groupId>
    <artifactId>nextcloud-sdk-core</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

---

## API Reference & Callable Blocks

### Identity Types (`io.github.uriakleahcim.nextcloud.core.id`)

#### [`AccountId`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-core/src/main/java/org/mcp/nextcloud/core/id/AccountId.java)
- **Description**: Strongly-typed immutable identifier representing a configured Nextcloud account key.
- **Callable**:
  - `new AccountId(String value)`: Validates that `value` is non-null and non-blank (trimmed automatically).
  - `String value()`: Returns the validated account ID string.

#### [`InvocationId`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-core/src/main/java/org/mcp/nextcloud/core/id/InvocationId.java)
- **Description**: Strongly-typed identifier for an individual MCP tool execution or RPC invocation.
- **Callable**:
  - `new InvocationId(String value)`: Validates non-blank invocation ID.
  - `String value()`: Returns the invocation ID string.

#### [`PrincipalId`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-core/src/main/java/org/mcp/nextcloud/core/id/PrincipalId.java)
- **Description**: Strongly-typed identifier for a calling user or service principal.
- **Callable**:
  - `new PrincipalId(String value)`: Validates non-blank principal ID.
  - `String value()`: Returns the principal ID string.

#### [`ToolId`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-core/src/main/java/org/mcp/nextcloud/core/id/ToolId.java)
- **Description**: Strongly-typed identifier for registered MCP tools (e.g. `nextcloud_files_read`).
- **Callable**:
  - `new ToolId(String value)`: Validates non-blank tool ID.
  - `String value()`: Returns the tool ID string.

---

### Result Models (`io.github.uriakleahcim.nextcloud.core.result`)

#### [`OperationResult<T>`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-core/src/main/java/org/mcp/nextcloud/core/result/OperationResult.java)
- **Description**: Container representing either a successful outcome containing `T value` or a failed outcome containing `ErrorResult error`.
- **Callable**:
  - `static <T> OperationResult<T> ok(T value)`: Creates a successful result container.
  - `static <T> OperationResult<T> failed(ErrorResult error)`: Creates a failed result container (requires non-null error).
  - `boolean success()`: True if the operation succeeded.
  - `T value()`: The payload value (null when failed).
  - `ErrorResult error()`: The failure metadata (null when successful).

#### [`ErrorResult`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-core/src/main/java/org/mcp/nextcloud/core/result/ErrorResult.java)
- **Description**: Structured immutable error information including machine code, human message, and metadata map.
- **Callable**:
  - `new ErrorResult(String code, String message, Map<String, Object> details)`: Constructs error result (details defaults to empty map if null).
  - `String code()`: Machine error identifier (e.g., `not_found`).
  - `String message()`: Human-readable message.
  - `Map<String, Object> details()`: Unmodifiable map of contextual diagnostic data.

#### [`PageResult<T>`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-core/src/main/java/org/mcp/nextcloud/core/result/PageResult.java)
- **Description**: Generic paginated list container with next-page token support.
- **Callable**:
  - `new PageResult(List<T> items, String nextPageToken)`: Constructs paginated container.
  - `List<T> items()`: Immutable list of items on current page.
  - `String nextPageToken()`: Pagination token for subsequent retrieval (nullable).

#### [`ProgressEvent`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-core/src/main/java/org/mcp/nextcloud/core/result/ProgressEvent.java)
- **Description**: Real-time progress update event emitted during long-running tasks.
- **Callable**:
  - `new ProgressEvent(String stage, String message, Instant occurredAt, Map<String, Object> attributes)`: Constructs progress event.
  - `String stage()`: Stage name.
  - `String message()`: Status message.
  - `Instant occurredAt()`: Timestamp of occurrence (defaults to `Instant.now()` if null).
  - `Map<String, Object> attributes()`: Context attributes.

---

### Exception Hierarchy (`io.github.uriakleahcim.nextcloud.core.error`)

```
java.lang.Throwable
 └── java.lang.Exception
      └── java.lang.RuntimeException
           └── NextcloudMcpException
                ├── NextcloudApiException
                ├── ToolExecutionException
                └── ConfigurationException
```

#### [`NextcloudMcpException`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-core/src/main/java/org/mcp/nextcloud/core/error/NextcloudMcpException.java)
- `new NextcloudMcpException(String code, String message)`
- `new NextcloudMcpException(String code, String message, Throwable cause)`
- `String code()`: Returns error classification code.

#### [`NextcloudApiException`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-core/src/main/java/org/mcp/nextcloud/core/error/NextcloudApiException.java)
- `new NextcloudApiException(String code, String message, int statusCode)`
- `int statusCode()`: HTTP status code from the Nextcloud server response.

#### [`ToolExecutionException`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-core/src/main/java/org/mcp/nextcloud/core/error/ToolExecutionException.java)
- `new ToolExecutionException(String code, String message)`
- `new ToolExecutionException(String code, String message, Throwable cause)`

#### [`ConfigurationException`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-core/src/main/java/org/mcp/nextcloud/core/error/ConfigurationException.java)
- `new ConfigurationException(String code, String message)`
- `new ConfigurationException(String code, String message, Throwable cause)`

---

### Utility Primitives (`io.github.uriakleahcim.nextcloud.core.util`)

#### [`Preconditions`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-core/src/main/java/org/mcp/nextcloud/core/util/Preconditions.java)
- `static String requireNonBlank(String value, String fieldName)`: Validates that `value` is non-null and not whitespace; returns trimmed string. Throws `IllegalArgumentException` otherwise.
- `static <T> T requireNonNull(T value, String fieldName)`: Validates that `value` is non-null; returns `value`. Throws `IllegalArgumentException` otherwise.

#### [`StringMasks`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-core/src/main/java/org/mcp/nextcloud/core/util/StringMasks.java)
- `static String maskSecret(String value)`: Masks sensitive values retaining first 2 and last 2 characters (or `********` if length <= 4).
- `static String maskKnownSecrets(String text, Collection<String> secrets)`: Replaces exact occurrences of known secret strings within text with masked versions.
- `static String maskPatterns(String text)`: Sanitizes Authorization headers (`Basic` / `Bearer`), URL credentials (`https://user:pass@host`), and key-value secret fields (`password=...`).

---

## Complete SDK Usage Snippets

### 1. Creating and Validating Strongly-Typed IDs
```java
import id.io.github.uriakleahcim.nextcloud.core.AccountId;
import id.io.github.uriakleahcim.nextcloud.core.InvocationId;
import id.io.github.uriakleahcim.nextcloud.core.PrincipalId;
import id.io.github.uriakleahcim.nextcloud.core.ToolId;

AccountId account = new AccountId("primary-user");
PrincipalId principal = new PrincipalId("admin-operator");
ToolId tool = new ToolId("nextcloud_files_read");
InvocationId invocation = new InvocationId("inv-98234-abc");

System.out.println("Running " + tool.value() + " for account " + account.value());
```

### 2. Constructing Operation Results
```java
import java.util.Map;
import result.io.github.uriakleahcim.nextcloud.core.ErrorResult;
import result.io.github.uriakleahcim.nextcloud.core.OperationResult;

// Successful outcome
OperationResult<String> successResult = OperationResult.ok("File uploaded successfully");
if (successResult.success()) {
    System.out.println("Result: " + successResult.value());
}

// Failure outcome
ErrorResult error = new ErrorResult("file_not_found", "The specified path does not exist", Map.of("path", "/missing.txt"));
OperationResult<String> failureResult = OperationResult.failed(error);
if (!failureResult.success()) {
    System.err.println("Error [" + failureResult.error().code() + "]: " + failureResult.error().message());
}
```

### 3. Redacting Logs and Sensitive Information
```java
import java.util.List;
import util.io.github.uriakleahcim.nextcloud.core.StringMasks;

String rawLog = "Connecting to https://user:superSecretPass@cloud.example.com with Authorization: Bearer abc123def456";
String sanitizedLog = StringMasks.maskPatterns(rawLog);
System.out.println(sanitizedLog);
// Prints: Connecting to https://user:********@cloud.example.com with Authorization: Bearer ********

String explicitSecret = "my-db-pass-12345";
String customLog = "Attempted login with pass " + explicitSecret;
System.out.println(StringMasks.maskKnownSecrets(customLog, List.of(explicitSecret)));
```

# Creating a Meshingress Tool Module

## Purpose

Meshingress tool modules are attachable Maven modules that expose MCP tools to the server. Each module lives under `toolspace/`, ships a Spring Boot auto-configuration entry, and declares one or more tool classes using Meshingress tool annotations or the SPI.

## Repository Layout

- `toolspace/<module-name>/` is the home for a tool module.
- `lib/meshingress-tool-api` exposes the tool SPI (`McpToolHandler`, `DispatchExecutionResult`).
- `lib/meshingress-tool-annotations` exposes annotation-based tool declarations.
- `app/meshingress-server` attaches toolspace modules as dependencies.

Examples in this repo:

- `toolspace/helloworld` (minimal annotation-based tool)
- `toolspace/powershell-cli-tool` (fully featured tool with structured output)
- `toolspace/instagram-api` (larger real-world tool)

## Required Dependencies

From `toolspace/powershell-cli-tool/pom.xml` and `app/meshingress-server/pom.xml`:

- `dev.mrk.meshingress:meshingress-tool-api` (SPI + result model)
- `dev.mrk.meshingress:meshingress-tool-annotations` (annotations)
- `org.springframework.boot:spring-boot-autoconfigure` (AutoConfiguration discovery)

## Minimal Module POM

```xml
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>dev.mrk.meshingress</groupId>
        <artifactId>meshingress</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <groupId>dev.mrk.toolspace</groupId>
    <artifactId>example-tool</artifactId>
    <name>example-tool</name>

    <properties>
        <maven.compiler.source>25</maven.compiler.source>
        <maven.compiler.target>25</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>dev.mrk.meshingress</groupId>
            <artifactId>meshingress-tool-api</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>dev.mrk.meshingress</groupId>
            <artifactId>meshingress-tool-annotations</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
        </dependency>
    </dependencies>
</project>
```

## Registering the Module with the Server

1. Add the module to the root `pom.xml` `<modules>` list.
2. Add a dependency on the tool module in `app/meshingress-server/pom.xml`.
3. Provide a Spring auto-configuration entry in the tool module so the server discovers your tool bean.

Example from `toolspace/powershell-cli-tool`:

```
toolspace/powershell-cli-tool/src/main/resources/META-INF/spring/
  org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Content:

```
dev.mrk.toolspace.powershellcli.PowerShellCliToolAutoConfiguration
```

## Minimal Tool Example

### Tool class

```java
package dev.mrk.toolspace.example;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.result.ResultContent;
import dev.mrk.meshingress.api.tools.annotation.McpConfigureMapping;
import dev.mrk.meshingress.api.tools.annotation.McpFunction;
import dev.mrk.meshingress.api.tools.annotation.McpFunctionParam;
import dev.mrk.meshingress.api.tools.annotation.McpSecret;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.api.tools.annotation.McpToolScopes;
import dev.mrk.meshingress.scopes.McpToolScope;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@McpTool(
        value = "example.annotated",
        title = "Annotated Example Tool",
        description = "Demonstrates a full annotation-based tool definition.",
        defaultFunction = "echo"
)
@McpToolScopes({
        McpToolScope.USER_READ,
        McpToolScope.CONFIG_READ
})
public class AnnotatedExampleTool {

    private final ObjectMapper objectMapper;

    public AnnotatedExampleTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @McpConfigureMapping(
            timeoutMs = 15_000,
            audit = true
    )
    @McpFunction(
            value = "echo",
            title = "Echo",
            description = "Echo a message back as text and structured JSON."
    )
    public DispatchExecutionResult echo(ExampleEchoArgs args, McpCallContext context) {
        ObjectNode payload = objectMapper.createObjectNode()
                .put("message", args.message())
                .put("sessionId", context.sessionId() == null ? "" : context.sessionId());

        return DispatchExecutionResult.builder()
                .appendContent(ResultContent.text("Echo: " + args.message()))
                .structuredContent(payload)
                .error(false)
                .build();
    }

    @McpConfigureMapping(
            timeoutMs = 30_000,
            debugTrace = true,
            secrets = {
                    @McpSecret(name = "apiKey", ref = "env:EXAMPLE_API_KEY")
            }
    )
    @McpToolScopes(McpToolScope.EXTERNAL_API_READ)
    @McpFunction(
            value = "summarize",
            title = "Summarize",
            description = "Summarize a list of items and return counts."
    )
    public DispatchExecutionResult summarize(
            @McpFunctionParam(value = "items", description = "Items to summarize")
            java.util.List<String> items,
            McpCallContext context
    ) {
        int count = items == null ? 0 : items.size();
        ObjectNode payload = objectMapper.createObjectNode()
                .put("count", count);

        return DispatchExecutionResult.builder()
                .appendContent(ResultContent.text("Summarized " + count + " items."))
                .structuredContent(payload)
                .error(false)
                .build();
    }
}
```

### Input record

```java
package dev.mrk.toolspace.example;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record ExampleEchoArgs(
        @McpInputField(
                value = "message",
                description = "Message to echo back.",
                required = true
        )
        String message
) {
}
```

### Auto-configuration

```java
package dev.mrk.toolspace.example;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
public class AnnotatedExampleToolAutoConfiguration {

    @Bean
    AnnotatedExampleTool annotatedExampleTool(ObjectMapper objectMapper) {
        return new AnnotatedExampleTool(objectMapper);
    }
}
```

### Auto-configuration imports

```
# toolspace/example-tool/src/main/resources/META-INF/spring/
# org.springframework.boot.autoconfigure.AutoConfiguration.imports

dev.mrk.toolspace.example.AnnotatedExampleToolAutoConfiguration
```

## Tool Annotation Reference

### `@McpTool`

- Target: type
- Required: `value` (tool ID), unless you rely on defaults
- Optional: `title`, `description`, `version`, `enabled`, `visibility`, `defaultFunction`, `handlerKey`, `dynamic`
- Tool ID pattern: `[a-z][a-z0-9]*(\.[a-z0-9]+)*`

Valid tool IDs:

- `helloworld.greeting.greet`
- `cli.powershell`

Invalid tool IDs:

- `HelloWorld`
- `tool-name`

### `@McpToolScopes`

- Target: type or method
- Optional: `value` array of `McpToolScope`
- Use at class level for baseline scopes and method level for additional scopes.

### `@McpConfigureMapping`

- Target: method
- Optional: `timeoutMs`, `audit`, `debugTrace`, `secrets`, `availabilityMode`
- `availabilityMode` controls how multiple availability checks are combined. If unsure, verify in the current tool framework implementation.

### `@McpSecret`

- Target: annotation entry within `@McpConfigureMapping`
- Required: `name`, `ref`
- Pattern: `name` is `[A-Za-z][A-Za-z0-9._-]*`, `ref` is `[A-Za-z0-9._:/-]+`

### `@McpToolAvailabilityCondition`

- Target: annotation type
- Required: `value` (an `AvailabilityCondition` implementation)
- Lets you create custom annotations that validate availability at discovery time.

Example:

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@McpToolAvailabilityCondition(RequiresEnvCondition.class)
public @interface RequiresEnv {
    String value();
}

public final class RequiresEnvCondition implements AvailabilityCondition<RequiresEnv> {
    @Override
    public List<String> validate(RequiresEnv annotation, AvailabilityValidationContext context) {
        String name = annotation.value();
        if (System.getenv(name) == null) {
            return List.of("Missing env var: " + name + " at " + context.location());
        }
        return List.of();
    }
}
```

## Function Annotation Reference

### `@McpFunction`

- Target: method
- Optional: `value`, `title`, `description`, `visibility`, `enabled`
- Use one tool class with multiple `@McpFunction` methods to expose multiple functions.

### `@McpFunctionParam`

- Target: parameter
- Required: `value`
- Optional: `description`, `required`, `implementation`
- Use when you want method parameters instead of a DTO/record class.

Example:

```java
@McpFunction(value = "sum", description = "Sum two numbers")
public DispatchExecutionResult sum(
        @McpFunctionParam(value = "left", description = "Left operand") int left,
        @McpFunctionParam(value = "right", description = "Right operand") int right,
        McpCallContext context
) {
    int total = left + right;
    return DispatchExecutionResult.builder()
            .appendContent(ResultContent.number(total))
            .structuredContent(objectMapper.createObjectNode().put("total", total))
            .error(false)
            .build();
}
```

## Input Schema and Parameters

Two supported styles appear in toolspace examples and annotations:

1. Record/DTO with `@McpInputField` on fields or record components.
2. Method parameters annotated with `@McpFunctionParam`.

### `@McpInputField`

- Target: field or record component
- Optional: `value`, `description`, `required`
- Use this for rich parameter metadata in a single input type.

### `@McpInputSchema`

- Target: type, method, or parameter
- Optional: `description`, `provider`
- Use a custom `McpJsonSchemaProvider` to produce JSON schema for inputs.

Example with schema provider:

```java
@McpInputSchema(provider = ExampleSchemaProvider.class)
public record ExampleEchoArgs(@McpInputField("message") String message) {
}

public final class ExampleSchemaProvider implements McpJsonSchemaProvider {
    @Override
    public ObjectNode schema(ObjectMapper objectMapper) {
        return objectMapper.createObjectNode()
                .put("type", "object")
                .set("properties", objectMapper.createObjectNode()
                        .set("message", objectMapper.createObjectNode()
                                .put("type", "string")
                                .put("description", "Message to echo")))
                .set("required", objectMapper.createArrayNode().add("message"));
    }
}
```

## Returning Results

Tools return `DispatchExecutionResult`. Each result can include:

- `content`: an array of `ResultContent` (text or JSON)
- `structuredContent`: optional JSON payload
- `_meta`: status/summary/error fields and generated timestamp

### Text result

```java
return DispatchExecutionResult.builder()
        .appendContent(ResultContent.text("Hello"))
        .error(false)
        .build();
```

### Structured JSON result

```java
ObjectNode payload = objectMapper.createObjectNode().put("status", "ok");
return DispatchExecutionResult.builder()
        .structuredContent(payload)
        .appendContent(ResultContent.object(payload))
        .error(false)
        .build();
```

### Error result

```java
return DispatchExecutionResult.builder()
        .error("EXAMPLE_FAILED", "Example failed")
        .status("failed")
        .summary("Example tool failed")
        .build();
```

## Errors and Metadata

`DispatchExecutionResult` emits `_meta` when any of these are set:

- `status`
- `summary`
- `errorCode`
- `errorMessage`

`_meta.generatedAt` is auto-populated if not present.

## Scopes and Permissions

Use `@McpToolScopes` to declare the least privilege needed by the tool.

Available `McpToolScope` values:

- Local: `LOCAL_READ`, `LOCAL_WRITE`
- Files: `FILES_READ`, `FILES_WRITE`, `FILES_DELETE`
- Network: `NETWORK_ACCESS`
- Database: `DATABASE_READ`, `DATABASE_WRITE`, `DATABASE_DELETE`
- Shell: `SHELL_EXECUTE`
- Email: `EMAIL_SEND`
- Users: `USER_READ`, `USER_WRITE`, `USER_DELETE`
- Config: `CONFIG_READ`, `CONFIG_WRITE`
- External APIs: `EXTERNAL_API_READ`, `EXTERNAL_API_WRITE`

Security checklist:

- Declare only the scopes you need.
- Avoid `SHELL_EXECUTE` unless absolutely required.
- Keep secrets out of source and examples.

## Secrets and Configuration

Use `@McpConfigureMapping` to declare secrets and configuration:

```java
@McpConfigureMapping(
        secrets = {
                @McpSecret(name = "apiKey", ref = "env:EXAMPLE_API_KEY")
        },
        timeoutMs = 20_000,
        audit = true
)
```

Do not hard-code secrets in tool source or README examples.

## Availability Conditions

- Use `@McpToolAvailabilityCondition` to create custom availability annotations.
- Use `availabilityMode = ALL | ANY` in `@McpConfigureMapping` to combine checks.
- The availability APIs live under `dev.mrk.meshingress.api.tools.annotation.availability`.

## Testing a Tool Module

- Build with the Maven wrapper in the repo root.
- Start the server and call `tools/list` and `tools/call` over the MCP endpoint documented in `AGENTS.md`.
- If a behavior is not obvious, verify against the current tool framework implementation.

## Common Mistakes

- Forgetting to add the module to the root `pom.xml` `<modules>` list.
- Missing the server dependency on the tool module in `app/meshingress-server/pom.xml`.
- Omitting `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- Using an invalid tool ID (fails the annotation pattern).
- Requesting overly broad scopes.
- Returning a `DispatchExecutionResult` without any content or structured payload.

## New Tool Module Checklist

- [ ] Create `toolspace/<name>/pom.xml` with the required dependencies.
- [ ] Add module entry to root `pom.xml`.
- [ ] Add tool module dependency to `app/meshingress-server/pom.xml`.
- [ ] Add a tool class annotated with `@McpTool` and `@McpFunction`.
- [ ] Add input DTO/record with `@McpInputField` or parameters with `@McpFunctionParam`.
- [ ] Add `@McpToolScopes` with least privilege.
- [ ] Add `@McpConfigureMapping` for timeouts, secrets, audit/debug as needed.
- [ ] Add auto-configuration class and `AutoConfiguration.imports` entry.
- [ ] Verify `tools/list` and `tools/call` see the new tool.


````
This is the description of what the code block changes:
<changeDescription>
Remove obsolete invocation name note from tool annotation reference.
</changeDescription>

This is the code block that represents the suggested code change:
```markdown
- Tool ID pattern: `[a-z][a-z0-9]*(\.[a-z0-9]+)*`

Valid tool IDs:
- `helloworld.greeting.greet`
- `cli.powershell`

Invalid tool IDs:
- `HelloWorld`
- `tool-name`
```


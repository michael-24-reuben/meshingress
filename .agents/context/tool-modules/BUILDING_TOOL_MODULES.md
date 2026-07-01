# Building Meshingress Tool Modules

Meshingress is a Java 25 Maven reactor. Tool modules live under `toolspace/<module-name>` and are discovered as Spring beans through Boot auto-configuration.

## Module integration contract

A new bundled module normally requires all of the following:

1. Add `toolspace/<module-name>` to the root `pom.xml` module list.
2. Give the module the repository parent and depend on `meshingress-tool-api`, `meshingress-tool-annotations`, and `meshingress-tool-framework` as needed. Annotation-based tools also use `spring-boot-autoconfigure`; add Jackson support when producing JSON results.
3. Implement an annotation-based tool class or the lower-level `McpToolHandler` SPI. Prefer annotations for normal multi-function tool modules.
4. Expose the tool as a bean from a module-local `@AutoConfiguration` class.
5. List that class in `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
6. Add the module dependency to `app/meshingress-tool-bundle/pom.xml` so the server receives it at startup.
7. Verify both discovery through `tools/list` and execution through `tools/call`.

Use `toolspace/helloworld` as the minimal working structure. Larger integration examples are available under `toolspace/voicebox`, `toolspace/webtoon-downloader`, and `toolspace/powershell-cli`.

## Annotation-based tool shape

- `@McpTool(value = "namespace", ...)` declares the tool namespace and metadata.
- `@McpToolMapping("tools")` maps the class into public tool dispatch.
- `@McpFunction(value = "action", ...)` exposes a callable function. Its public name is `namespace.action`.
- Use a typed argument record/class and `@McpInputField` or `@McpFunctionParam` to describe inputs.
- Accept `McpCallContext` only when request identity, authorization, session, or request metadata is needed.
- Return `DispatchExecutionResult`. Include human-readable `ResultContent.text(...)`; add `structuredContent` for machine-readable output.
- Declare the narrowest accurate `@McpToolScopes`. Method scopes are additive to class scopes.
- Use `@McpConfigureMapping` for timeout, secret references, audit, or debug tracing. Never hard-code credentials.
- Add availability or cache annotations only when their behavior is required and tested.

## Direct SPI alternative

Implement `McpToolHandler` when descriptor and raw `ObjectNode` argument handling must be controlled directly. Implement `descriptor()` and `call(ObjectNode, McpCallContext)`. Depend on `meshingress-tool-api`, not server implementation classes.

## Editing rules

- Keep public tool names stable after release.
- Treat input schemas as public contracts; make intentional compatibility decisions when fields change.
- Return expected tool failures in the established result/error shape instead of leaking stack traces or secrets.
- Keep transport, registry, installation, and role-management logic in the server; tool modules contain tool-specific behavior and module-local configuration.
- Preserve least privilege. Shell, filesystem writes, secrets, tokens, inbound network, policy, plugin, and deployment scopes require explicit justification.
- Add focused module tests and server MVC integration coverage for externally visible behavior.

## Verification

From the repository root on Windows:

```powershell
.\mvnw.cmd -pl toolspace/<module-name> -am test
.\mvnw.cmd -pl app/meshingress-server -am test
```

For focused server tests, quote Maven properties in PowerShell:

```powershell
.\mvnw.cmd -pl app/meshingress-server -am "-Dtest=McpControllerOutputTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Runtime request shapes:

```json
{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}
```

```json
{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"namespace.action","arguments":{}}}
```


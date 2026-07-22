# MCP Tool Module Lifecycle — `meshingress-config`

## Package Role

This package provides the typed root configuration model shared by the Meshingress runtime.

## User-Visible Contribution

Server operators can configure MCP transport, tool discovery and dispatch, security policy, scope enforcement, audit behavior, storage, and secret-resolution behavior through the `meshingress` property namespace.

## Position in the Feature Path

```text
application properties / environment
  -> MeshingressProperties
  -> server and runtime-loader configuration
  -> transport, tool, and policy behavior
```

## Entry Points

- `config.MeshingressProperties` — `@ConfigurationProperties(prefix = "meshingress")` root model.
- `meshingress-properties-todo.md` — tracked property-model coverage; it is not executable configuration.

## Configuration and Resources

The model is bound by the application. Main property sources are supplied by the server, including `app/meshingress-server/src/main/resources/application.properties`. This package does not establish source precedence or store secret values.

## Feature Contract

```yaml
prefix: meshingress
consumer: Spring configuration binding
contains: nested runtime and policy groups
restartRequired: unresolved from this package alone
```

## Dependencies

- Upstream: server property files, environment variables, and command-line configuration.
- Downstream: server configuration and `meshingress-tool-runtime-loader`.

## Failure Behavior

Bean-validation or binding failures prevent the application from receiving a valid typed configuration model.

## Verification

Run the server configuration-context tests after changing property records or validation annotations.

## Evidence and Open Questions

Confirmed by `MeshingressProperties` and its declared prefix. Effective source ordering and runtime mutability must be established by the consuming Spring Boot application.

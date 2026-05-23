# Todo

## API Contracts

- [x] Define `HTTPRequest<Q, P, B>`.
- [x] Define `HTTPResponse<T>`.
- [x] Define `McpErrorResponse`.
- [x] Define `McpRouteExecutionContext`.

## Annotations

- [x] Add `@McpRoute`.
- [x] Add `@McpConfigureMapping`.
- [x] Add `@McpSecret`.
- [x] Add `@McpRequestMiddleware` using middleware classes, not strings.
- [x] Add annotation-based MCP JSON-RPC dispatch mapping support.
- [ ] Add JavaDoc examples for string-based parameters.
- [x] Add JetBrains `@Pattern` annotations where helpful for string integrity.

## Middleware

- [x] Define `McpMiddleware<Q, P, B>` interface.
- [ ] Implement `ConfirmAuthToken` middleware.
- [ ] Implement `RequireRouteId` middleware.
- [ ] Implement `RequireWorkspaceAccess` or equivalent tenant/workspace middleware.
- [ ] Enforce one middleware concern per class.
- [ ] Ensure middleware order is deterministic and follows annotation order.

## Secrets

- [ ] Implement secret reference resolver.
- [ ] Validate secret refs at startup where possible.
- [ ] Prevent raw secrets from entering logs, errors, audit messages, or normal request attributes.

## Execution Framework

- [x] Implement route scanning/registry.
- [x] Implement route execution aspect/interceptor.
- [x] Implement annotation-based MCP JSON-RPC dispatch scanner/registry/invoker.
- [x] Replace `McpMethodController` dispatch ownership with route-framework dispatch metadata.
- [x] Inject server-authoritative route ID into request/context.
- [x] Run middleware pipeline.
- [ ] Resolve secrets before handler execution only when needed.
- [x] Convert framework exceptions into standard error responses.

## Validation

- [x] Add startup route registry validator.
- [x] Validate unique route IDs.
- [x] Validate route paths use actual server paths.
- [x] Validate middleware references.
- [x] Validate annotation values such as secret refs.

## Observability

- [x] Add structured logs for route lifecycle.
- [ ] Add debug trace mode per route.
- [ ] Add metrics for route duration, middleware duration, errors, and timeouts.
- [ ] Ensure sensitive values are redacted.

## Testing

- [ ] Unit test middleware interface and implementations.
- [x] Unit test startup validator failure cases.
- [x] Integration test controller route metadata attachment.
- [x] Unit test annotation-based MCP dispatch scanning, argument binding, and duplicate detection.
- [x] Integration test annotation-based MCP dispatch through `POST /mcp`.
- [ ] Integration test malformed annotation config fails fast at startup.

## Module Structure

- [x] Create `lib/meshingress-route-api` for stable contracts.
- [x] Create `lib/meshingress-route-annotations` for annotations and metadata enums.
- [x] Create `lib/meshingress-route-framework` for runtime scanning, validation, and execution.
- [x] Do not create a centralized `lib/meshingress-route` aggregate module.
- [x] Wire Maven parent modules in dependency order.
- [x] Ensure `meshingress-route-api` does not depend on Spring runtime classes.
- [x] Ensure `meshingress-route-annotations` does not contain execution logic.
- [x] Ensure `meshingress-route-framework` owns route scanning, validation, and execution behavior.
- [x] Keep app-specific controllers, middleware, policies, secrets, and external clients in `app/meshingress-server` unless they are clearly reusable.
- [x] Attach current `/mcp` server path metadata in `app/meshingress-server`.

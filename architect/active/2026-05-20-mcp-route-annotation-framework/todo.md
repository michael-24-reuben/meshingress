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
- [x] Add `@McpAvailabilityPolicy` meta-annotation.
- [x] Add specific availability annotations.
- [ ] Add JavaDoc examples for string-based parameters.
- [x] Add JetBrains `@Pattern` annotations where helpful for string integrity.

## Middleware

- [x] Define `McpMiddleware<Q, P, B>` interface.
- [ ] Implement `ConfirmAuthToken` middleware.
- [ ] Implement `RequireRouteId` middleware.
- [ ] Implement `RequireWorkspaceAccess` or equivalent tenant/workspace middleware.
- [ ] Enforce one middleware concern per class.
- [ ] Ensure middleware order is deterministic and follows annotation order.

## Availability

- [x] Implement `EnableWithinTimeRangesPolicy`.
- [x] Implement `EnableOnDaysPolicy`.
- [x] Implement `EnableWhenFeatureFlagOnPolicy`.
- [x] Support `McpAvailabilityMode.ALL`.
- [x] Consider `McpAvailabilityMode.ANY` only if a real route needs it.
- [ ] Ensure availability runs after authentication/authorization middleware.

## Secrets

- [ ] Implement secret reference resolver.
- [ ] Validate secret refs at startup where possible.
- [ ] Prevent raw secrets from entering logs, errors, audit messages, or normal request attributes.

## Execution Framework

- [x] Implement route scanning/registry.
- [x] Implement route execution aspect/interceptor.
- [x] Inject server-authoritative route ID into request/context.
- [x] Run middleware pipeline.
- [x] Evaluate availability annotations.
- [ ] Resolve secrets before handler execution only when needed.
- [x] Convert framework exceptions into standard error responses.

## Validation

- [x] Add startup route registry validator.
- [x] Validate unique route IDs.
- [x] Validate method signatures use exactly `HTTPRequest<Query, Params, Body>`.
- [x] Validate middleware references.
- [x] Validate availability policy references.
- [x] Validate annotation values such as time ranges, zones, feature flags, and secret refs.

## Observability

- [x] Add structured logs for route lifecycle.
- [ ] Add debug trace mode per route.
- [ ] Add metrics for route duration, middleware duration, availability decisions, errors, and timeouts.
- [ ] Ensure sensitive values are redacted.

## Testing

- [x] Unit test middleware interface and implementations.
- [x] Unit test availability policies.
- [x] Unit test startup validator failure cases.
- [x] Integration test controller execution path.
- [ ] Integration test unauthenticated requests cannot probe availability details.
- [ ] Integration test malformed annotation config fails fast at startup.

## Module Structure

- [x] Create `lib/meshingress-route-api` for stable contracts.
- [x] Create `lib/meshingress-route-annotations` for annotations and metadata enums.
- [x] Create `lib/meshingress-route-framework` for runtime scanning, validation, and execution.
- [x] Do not create a centralized `lib/meshingress-route` aggregate module.
- [x] Wire Maven parent modules in dependency order.
- [x] Ensure `meshingress-route-api` does not depend on Spring runtime classes.
- [x] Ensure `meshingress-route-annotations` does not contain execution logic.
- [x] Ensure `meshingress-route-framework` owns Spring integration and route execution behavior.
- [x] Keep app-specific controllers, middleware, policies, secrets, and external clients in `app/meshingress-server` unless they are clearly reusable.
- [x] Do not add route-framework dependencies or route implementation wiring to `app/meshingress-server` in the first slice.

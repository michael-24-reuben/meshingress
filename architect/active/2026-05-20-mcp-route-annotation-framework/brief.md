# MCP Route Annotation Framework

## Objective

Design and implement a Spring Boot MCP route execution model where controller methods use a stable request envelope:

```java
post(HTTPRequest<Query, Params, Body> request)
```

The controller should remain declarative and readable while route execution concerns are handled by a small framework layer.

## Target Controller Shape

```java
@PostMapping("/publish/{workspaceId}")
@McpRoute(
        id = "instagram.publish.post.v1",
        method = POST,
        path = "/mcp/tools/instagram/publish/{workspaceId}"
)
@McpRequestMiddleware({
        ConfirmAuthToken.class,
        RequireWorkspaceAccess.class,
        RequireRouteId.class
})
@McpConfigureMapping(
        secrets = {
                @McpSecret(name = "instagramApiToken", ref = "instagram-api-token")
        },
        availabilityMode = ALL,
        audit = true,
        debugTrace = true,
        timeoutMs = 20_000
)
@EnableWithinTimeRanges(
        zone = "America/New_York",
        ranges = {
                "09:00-17:00",
                "19:00-21:00"
        }
)
@EnableOnDays({
        MONDAY,
        TUESDAY,
        WEDNESDAY,
        THURSDAY,
        FRIDAY
})
@EnableWhenFeatureFlagOn("instagram.publish.enabled")
public HTTPResponse<PublishPostResponse> post(
        HTTPRequest<PublishPostQuery, PublishPostParams, PublishPostBody> request
) {
    ...
}
```

## Core Decisions

- Use class-based middleware references instead of string middleware names.
- Each middleware class should represent exactly one middleware unit.
- Middleware should implement a shared interface with a consistent calling method.
- Replace generic availability argument maps with individual availability annotations.
- Keep `@McpConfigureMapping` for common route execution settings only.
- Use server-authoritative route IDs from `@McpRoute`; do not trust client-provided route IDs.
- Add structured logs and metrics to reduce annotation-runtime opacity.
- Add startup validation to catch bad annotations, invalid route config, missing beans, and malformed values before serving traffic.

## Scope Boundary

This framework should handle route execution concerns:

- route identity
- request envelope
- middleware execution
- security middleware
- availability checks
- secret references
- audit/debug trace
- timeout metadata
- standard error behavior
- route registry validation
- observability hooks

It should not absorb domain/business logic, external API implementation logic, or complex dynamic policy DSL behavior.

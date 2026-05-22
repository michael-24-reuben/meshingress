# Context

## Background

The design started from a desired Spring Boot controller style where handlers receive a typed request envelope:

```java
post(HTTPRequest<Query, Params, Body> request)
```

Initial planning considered middleware by string names and availability policies configured inside `@McpConfigureMapping` through generic key/value args. The design was refined because string middleware names and generic availability args are flexible but not reliable enough for long-term framework use.

## Current Direction

The preferred model is:

- class-based middleware references
- one middleware class per middleware concern
- shared middleware interface
- specific availability annotations
- JavaDoc and JetBrains `@Pattern` for string parameter guidance
- structured logs and metrics to reduce runtime opacity
- startup validation to fail fast

## Existing Project Notes

The project uses a multi-module Maven layout with separate annotation/API/server modules. The server module depends on the tool API and annotations modules, plus Spring Boot web, security, validation, actuator, websocket, JPA, and OpenAPI dependencies. This layout fits the proposed split between annotations, API contracts, and server-side execution runtime.

## Important Design Constraints

- Java annotations cannot reference local methods directly.
- Java annotations cannot accept arbitrary heterogeneous annotation arrays inside a single member unless wrapped in a common annotation type.
- Specific availability annotations should be placed directly on the route method.
- `@McpConfigureMapping` should remain relatively shallow and should not become a nested policy DSL.
- Route ID must be authoritative from server annotation metadata, not client input.
- Availability should not replace authorization.
- Authentication/authorization middleware should run before availability to reduce information leakage.

## Module Structure Decision

Use exactly this three-module `lib/` split for the first route system implementation slice:

```txt
lib/meshingress-route-api
lib/meshingress-route-annotations
lib/meshingress-route-framework
```

Do not create a centralized catch-all module such as `lib/meshingress-route`, `lib/meshingress-route-core`, or a server-owned route module for this slice.

`meshingress-route-framework` is appropriate only for the runtime framework layer that owns route scanning, startup validation, middleware execution, availability evaluation, error mapping, structured logging, metrics, and Spring integration.

Stable contracts should live in `meshingress-route-api`. Declarative annotations should live in `meshingress-route-annotations`. Runtime execution machinery should live in `meshingress-route-framework`.

Do not introduce the implementation into `app/meshingress-server` yet. Server integration should be treated as a later bridge phase after the three library modules compile and are tested in isolation.

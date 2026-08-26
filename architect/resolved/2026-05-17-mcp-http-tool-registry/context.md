# Context

## Existing Project Shape

The current Maven project is a Spring Boot application named `meshingress` with dependencies suitable for an MCP HTTP server:

- WebMVC for HTTP controllers.
- Actuator for health/metrics.
- Security for authentication and authorization.
- Validation for DTO validation.
- JPA and PostgreSQL for persistence.
- Springdoc OpenAPI for API documentation.

The current `application.properties` name is also `meshingress`.

## Naming Concern

The name `meshingress` sounds like an ingress/proxy/networking service. If the primary role is an MCP server for structured engineering memory, `architect-mcp` is more precise.

Possible naming decision:

```txt
artifactId: architect-mcp
spring.application.name: architect-mcp
package root: dev.mrk.architectmcp
```

Alternative if keeping continuity:

```txt
artifactId: meshingress
spring.application.name: meshingress
service display name: architect-mcp
```

## Architect Memory Model

The project's `architect/` convention stores work records in lifecycle folders:

```txt
architect/pending/
architect/active/
architect/resolved/
architect/archived/
```

Entries contain files such as:

- `meta.json`
- `brief.md`
- `prd.md`
- `todo.md`
- `context.md`
- `plan.md`
- `notes.md`
- `blockers.md`
- `assessment.md`
- `fixes.md`
- `verification.md`
- `summary.md`

This MCP server should eventually expose that memory model through tools and resources.

## Recommended Initial MCP Tools

```txt
architect.entries.list
architect.entries.get
architect.entries.search
architect.entries.create
architect.entries.update_file
architect.entries.move
architect.entries.link
architect.entries.summarize
```

## Tool Risk Classes

Low risk:

```txt
architect.entries.list
architect.entries.get
architect.entries.search
```

Medium risk:

```txt
architect.entries.create
architect.entries.update_file
architect.entries.move
architect.entries.link
architect.entries.summarize
```

High risk / defer:

```txt
architect.entries.delete
admin/tools/delete with physical deletion
remote executable handler registration
```

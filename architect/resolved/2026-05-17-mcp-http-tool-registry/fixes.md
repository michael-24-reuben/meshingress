# Fixes

## Files Changed

- `pom.xml`
- `README.md`
- `src/main/resources/application.properties`
- `src/main/java/dev/mrk/meshingress/mcp/**`
- `src/main/java/dev/mrk/meshingress/security/SecurityConfig.java`
- `src/test/java/dev/mrk/meshingress/**`

## Implementation

- Added `McpController` with `POST /mcp`, `GET /mcp`, and `DELETE /mcp`.
- Added `McpDispatcher` for JSON-RPC validation, batch handling, notification suppression, and method routing.
- Added JSON-RPC response/error helpers and explicit error codes.
- Added in-memory tool registry types, descriptor model, patch model, audit events, and executor.
- Added admin registry service for tool checking, registration, update, disable/delete, listing, and reload.
- Added `architect.entries.list` as the first static read-only MCP tool.
- Added simple admin authorization support through `Authorization: Bearer dev-admin` or `X-Mcp-Admin: true`.
- Updated Boot 4 JSON integration to use Jackson 3 `tools.jackson.*` APIs.
- Added MVC tests for initialize, tools/list, tools/call, invalid protocol envelopes, admin authorization, registry mutation, and reserved `/mcp` method behavior.

## Build Configuration

- Changed the Maven release from Java 25 to Java 22 because the installed compiler is `javac 22.0.2`.
- Added `spring-boot-starter-jackson` explicitly because Spring Boot 4 separates Jackson auto-configuration from the WebMVC starter.
- Excluded datasource and Hibernate JPA auto-configuration until a database-backed registry is implemented.

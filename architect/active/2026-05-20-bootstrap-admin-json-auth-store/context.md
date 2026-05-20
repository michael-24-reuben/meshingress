# Context

## Current Behavior

MCP WebSocket authentication currently reads credential headers during the WebSocket handshake:

- `Authorization`
- `X-Secret-Key`
- `X-Auth-Token`
- optional role/session/request headers

The current validator compares those headers against Spring properties:

- `meshingress.mcp.auth.stub.access-token`
- `meshingress.mcp.auth.stub.secret-key`
- `meshingress.mcp.auth.stub.auth-token`

The sample flow writes those values into `temp/mcp-ws-demo/application-mcp-ws-demo.properties`, then asks the user to restart the server with that file as an additional Spring config location.

## Why This Needs Replacement

The current flow is only a transport-auth stub. It proves WebSocket headers can be checked, but it is too awkward for an application setup path:

- auth material is configured before the app can register an admin;
- credential changes require restart;
- the credential file is a Spring property file rather than app-owned state;
- there is no first-admin lifecycle;
- there is no path toward SQL-backed persistence except replacing the validator later.

## User Decisions

- The server should start with zero preconfigured MCP auth tokens.
- The server should write default admin details into a JSON store file.
- The JSON store should initially be otherwise empty except for the bootstrap password, based on the Spring Security generated password.
- First admin registration should use the JSON-stored bootstrap password as confirmation.
- `newPassword` should be optional because a generated password already exists.
- Generated MCP auth contents should be returned only once during registration.
- Future identity fields such as email should be considered now, even if not required in the MVP.
- MCP/public connections should not be globally halted just because no admin has registered, but admin actions must remain protected.
- Persistence should be behind store registry interfaces with getters, setters, lookups, and validation helpers.
- SQL should be a future implementation of the same registry contract.
- Once implementation starts, this architect record should stay open until SQL storage is implemented or explicitly split out.

## Related Entry

`architect/resolved/2026-05-19-mcp-websocket-auth` added the current authenticated `/mcp/ws` transport. This new entry supersedes the property-backed credential source while preserving the WebSocket header-auth transport shape.

## Open Implementation Notes

Spring Boot's generated password is currently implicit through Spring Security auto-configuration. The implementation should verify how that password is produced and whether the app can capture it cleanly. If direct capture is brittle, the project may need to generate and log/store its own bootstrap password while matching Spring Security's startup behavior closely enough for local development.

The JSON store is a local development and migration stepping stone. The store registry should make the JSON implementation disposable once SQL persistence exists.

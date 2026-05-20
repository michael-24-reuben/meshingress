# Bootstrap Admin JSON Auth Store

Replace the current property-backed MCP WebSocket credential technique with a first-run bootstrap flow.

The server should start without preconfigured MCP auth tokens. On startup, it should create a JSON auth store file, tentatively named `data/mcp-auth-store.json`, containing default admin bootstrap details. The initial admin password should be the Spring Security generated password already available during development, and the first admin registration should use that stored bootstrap password as confirmation.

The first admin registration should accept a username, optional new password, confirmation password, and future-ready identity fields such as email. After successful registration, the server should generate MCP credential material similar to the current demo property file values and return those credentials once in the registration response.

The JSON store is the first storage implementation, not the final design. The implementation should define store registry interfaces for admin and MCP auth records so a later SQL-backed implementation can replace the JSON implementation without changing controller or handshake logic.

When this work moves to active implementation, keep the architect entry open until the SQL-backed store is implemented or the project explicitly splits SQL migration into a separate linked entry.

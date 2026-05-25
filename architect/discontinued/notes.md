# Notes

## 2026-05-20

- Implementation started for the first phase: JSON-backed bootstrap admin and MCP auth credential storage.
- Local inspection confirmed Boot 4.0.6 keeps the generated development password in `org.springframework.boot.security.autoconfigure.SecurityProperties.User`.
- The first implementation should initialize `data/mcp-auth-store.json` from `SecurityProperties#getUser().getPassword()` when the file is missing.
- The existing WebSocket middleware already reads headers; the validator will be changed from property-backed comparison to store-backed validation.
- Added `AuthStoreRegistry` and `JsonAuthStoreRegistry` as the first persistence boundary for bootstrap admin and MCP credentials.
- Added `POST /api/v1/auth/bootstrap/admin` for first-admin registration.
- Registration stores admin metadata, supports optional password replacement, generates MCP credential triples, stores them, and returns the generated secrets once.
- `McpCredentialValidator` now validates WebSocket credentials through the store registry instead of Spring properties.
- `RoleAuthorizationService` now accepts store-backed admin bearer tokens, while preserving the legacy `dev-admin` bearer fallback for existing HTTP tests.
- Verification: `.\mvnw.cmd -pl app/meshingress-server -am test` passed with JDK 25.0.3; server module reported 38 tests, 0 failures, 0 errors.
- The entry remains active because SQL-backed persistence is still outstanding by design.

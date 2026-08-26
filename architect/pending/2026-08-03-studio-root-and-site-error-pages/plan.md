# Plan

1. Select the production Studio delivery model. The root path must not depend accidentally on a local Vite port in a deployed runtime.
2. Add the root route so browser navigation enters Studio using that selected model.
3. Add a site-owned error surface for browser navigation, including at least not-found and unexpected-server-error states.
4. Scope error handling so API and MCP clients retain machine-readable contracts instead of receiving HTML.
5. Add server route tests and browser verification for `/`, an unknown browser path, a failing page route, and representative API/MCP error responses.

## Non-goals

- Reworking the Studio feature itself.
- Changing workflow API contracts.
- Replacing existing JSON-RPC error behavior.
- Redirecting API or MCP callers to an HTML page.

# Context

At `GET /`, the server currently has no controller mapping and no static `index.html`. Spring therefore attempts static resource resolution, throws `NoResourceFoundException`, and shows its default `/error` fallback.

Workflow Studio is currently a separate Vite application under `app/meshingress-studio-web`, with a development server normally running on port 5173. The Meshingress server is configured on port 4737.

The owner direction is:

- `/` should open Workflow Studio.
- `/error` should soon be a separate page in the same site.

The implementation must distinguish browser navigation from API and MCP clients. HTML navigation may receive the site error surface; `/api/**`, `/mcp`, and other machine-consumed routes must retain their appropriate JSON, JSON-RPC, status, and header contracts.

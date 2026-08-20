# Meshingress Studio Web

The React and TypeScript home for the Meshingress Workflow Studio UI.

The current standalone prototype remains at
`generated/meshingress-html-client/v2.0/meshingress-workflow-studio.html`.
Migrate it here incrementally, keeping canvas/layout metadata in the frontend
and workflow definitions/runtime contracts in the server API.

## Run locally

```powershell
npm install
npm run dev
```

The development server normally starts at `http://localhost:5173`.

## Runtime configuration

The Studio loads `public/runtime-config.yaml` when it starts. Set
`meshingress.apiBaseUrl` there to point a deployed Studio at another server;
Vite copies this file to `runtime-config.yaml` beside the built `index.html`.

Workflow runs prefer the WebSocket path in `meshingress.workflowWebSocketPath`
for live node logs. If that socket cannot be opened before a run starts, Studio
uses the existing HTTP sample route instead. It never retries via HTTP after a
live run starts, avoiding a duplicate mutating workflow execution.

`VITE_MESHINGRESS_API_BASE_URL` remains a fallback for local development when
the YAML file is missing or invalid.

API code is intentionally split by server route family:

- `src/api/studio` for workflow definitions, publication, and execution.
- `src/api/storage` for workspace/file storage.
- `src/api/repository` for artifact repository integration.

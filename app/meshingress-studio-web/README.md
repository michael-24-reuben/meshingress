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

## API configuration

Copy `.env.example` to `.env.local` to point the Studio at another server.
`VITE_MESHINGRESS_API_BASE_URL` defaults to `http://localhost:4737`.

API code is intentionally split by server route family:

- `src/api/studio` for workflow definitions, publication, and execution.
- `src/api/storage` for workspace/file storage.
- `src/api/repository` for artifact repository integration.

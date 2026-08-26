# Meshingress Studio Web

The React and TypeScript home for the Meshingress Workflow Studio UI.

The current standalone prototype remains at
`generated/meshingress-html-client/v2.0/meshingress-workflow-studio.html`.
Migrate it here incrementally, keeping canvas/layout metadata in the frontend
and workflow definitions/runtime contracts in the server API.

## Run locally

The project includes an automated launch pipeline that runs pre-flight validation, cleans previous build artifacts, performs a fresh compilation (`tsc -b` and `vite build`), validates generated bundle assets, and launches the development server.

### Using the Launch Executables

**PowerShell (Windows):**
```powershell
.\launch.ps1
```

**Command Prompt / Batch:**
```cmd
launch.cmd
```

**NPM Scripts:**
```powershell
npm run launch
```

The development server starts at `http://localhost:5173`.

### Launcher Options & Flags

| Command / Flag | Description |
| :--- | :--- |
| `.\launch.ps1` | Full pipeline: Validate ➔ Clean ➔ Fresh Compile ➔ Validate Artifacts ➔ Run `npm run dev` |
| `.\launch.ps1 --build-only` | Run validate, clean, fresh compile, and artifact verification, then exit |
| `.\launch.ps1 --clean-only` | Clean `dist/`, `.vite`, and `.tsbuildinfo` caches and exit (or `npm run clean`) |
| `.\launch.ps1 --skip-build` | Skip clean and compile, launch dev server immediately |
| `.\launch.ps1 --port <port>` | Set custom Vite dev server port (e.g., `--port 3000`) |
| `.\launch.ps1 --host` | Expose development server on the local network |
| `.\launch.ps1 --help` | Display all available launcher options |

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

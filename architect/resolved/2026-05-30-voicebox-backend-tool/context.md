# Context

- Upstream Voicebox is a local-first FastAPI backend that can run with `python -m backend.main --host 127.0.0.1 --port 17493`.
- Useful REST endpoints for the Meshingress wrapper are `GET /health`, `GET /profiles`, `POST /speak`, `GET /generate/{id}/status`, and `POST /transcribe`.
- Upstream Voicebox also ships its own MCP server at `/mcp` with `voicebox.speak`, `voicebox.transcribe`, `voicebox.list_captures`, and `voicebox.list_profiles`; this conversion uses the REST surface so Meshingress can expose native local tool functions.
- Voicebox uses `X-Voicebox-Client-Id` for per-client voice binding. The wrapper defaults this to `meshingress` and lets deployments override it with `meshingress.voicebox.client-id`.

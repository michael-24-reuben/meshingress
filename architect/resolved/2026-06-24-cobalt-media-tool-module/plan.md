# Plan

1. Keep the vendored upstream source under `toolspace/cobalt/upstream/cobalt`.
2. Add a small Java wrapper that calls a configured Cobalt API base URL, defaulting to `http://127.0.0.1:9000`.
3. Expose `cobalt.info` for `GET /` and `cobalt.process` for Cobalt's main `POST /` processing endpoint.
4. Keep process startup outside the MCP wrapper for this slice; local Cobalt can be run from the vendored upstream with pnpm or Docker.
5. Attach the module through the root Maven reactor and `app/meshingress-tool-bundle`.
6. Verify with focused module/server tests.

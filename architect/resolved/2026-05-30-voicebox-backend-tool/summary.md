# Summary

Meshingress was converted to a backend-only Voicebox tool slice. The backend reactor now builds the Spring MCP server with a bundled `toolspace/voicebox` module that calls the local Voicebox FastAPI server at `http://127.0.0.1:17493` by default. Frontend assets were removed from the checkout surface, and focused MCP tests plus skip-test packaging pass.

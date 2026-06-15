# Fixes

- Added `toolspace/voicebox` with annotation-based functions for `health`, `list_profiles`, `speak`, `generation_status`, and `transcribe_file`.
- Rewired the root Maven reactor and `app/meshingress-tool-bundle` to include only the Voicebox tool module from `toolspace`.
- Added `meshingress.voicebox.base-url` and `meshingress.voicebox.client-id` defaults.
- Updated MCP controller tests from the removed Hello World bundle expectation to Voicebox tool discovery and unavailable-backend behavior.
- Removed the local `frontend/` tree and added `frontend/` to `.gitignore`.
- Removed retired sample/social toolspace source trees so `toolspace/` is focused on the Voicebox module.

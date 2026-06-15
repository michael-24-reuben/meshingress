# Plan

- Add a `toolspace/voicebox` module that follows the current Meshingress annotation-based tool pattern.
- Wire the module into the root Maven reactor and `app/meshingress-tool-bundle`.
- Update server MCP tests to expect the Voicebox tool instead of the previous Hello World sample.
- Remove or ignore frontend-only assets so the checkout is backend-focused.
- Verify with a focused Maven test/compile path.

# Implemented Change

- Removed `dev.mrk.meshingress.api.result.progress.hooks.WebSocketReporter`.
- Added `dev.mrk.meshingress.api.result.progress.hooks.MeshigressWebSocketReporter`.
- Updated `McpProgressReporter.webSocket(Consumer<ProgressUpdate>)` to construct the replacement reporter.
- Updated the progress draft README to use the replacement name.

`MeshigressWebSocketReporter` forwards planned, update, warning, successful-completion, and failed-completion events to the supplied sender. The factory rejects a null sender before reporter creation.

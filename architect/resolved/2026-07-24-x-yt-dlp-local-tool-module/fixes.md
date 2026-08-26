# Fixes

- Added local-only `toolspace/x-yt-dlp` with a Maven module, Boot auto-configuration, and annotation-based MCP functions.
- Vendored yt-dlp `2026.07.04` sources, yt-dlp-ejs `0.8.0`, package metadata, and licenses under the module.
- Added `media.ytdlp.inspect` and `media.ytdlp.download`.
- Restricted invocation to typed URL/format fields, fixed process arguments, module-local `PYTHONPATH`, Python `-S`, explicit Node selection, and module-contained download paths.
- Added the module to the root reactor and the local tool bundle, with a server discovery test.

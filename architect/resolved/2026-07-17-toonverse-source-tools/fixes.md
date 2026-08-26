# Fixes

- Added `toonverse.download-book(name, minChapterNumber, maxChapterNumber, ttlSeconds?, maxRequests?)`.
- Added guarded Toonverse media download support for page and cover URLs.
- Materialized each requested visible page under `chapters/<number>/`, with a `chapter.json` descriptor for that chapter.
- Wrote `cover.<extension>` when the source supplies a cover and a root `book.json` descriptor using `open-ink.book/v1`.
- Published the files through `ToolStorageWorkspace`, returning its `/storage/{sessionId}/{requestId}/files/` base URI and `book.json` path to the caller.
- Made source auto-configuration accept the optional tool-storage service so the module remains independently testable while the server supplies storage at runtime.
- Added the new public tool to the server MCP discovery assertion.
- Added a default storage cleanup scheduler interval so server test contexts that do not supply the property still start.

## Runtime failure correction

- Added positive `timeoutMs` from `@McpConfigureMapping` to annotated function metadata.
- Updated `McpDispatchExecutor` to use that timeout for `tools/call` after resolving the requested enabled function; all other MCP methods retain the global dispatch default.
- Added a source availability preflight for `download-book`, before opening the workspace, using Toonverse's `chapterNumbers` reader field.

# Todo

- [x] Create `toonverse.fetch` using generic `name` input and exact source-side title/slug resolution.
- [x] Create `toonverse.search` with name, genre, type, chapter, rating, author, status, sorting, and pagination inputs.
- [x] Return the fixed platform source identifier rather than a source work identifier.
- [x] Keep generic `book.image.*` and `book.text.*` classes out of public Spring/MCP registration.
- [x] Add source-client and source-tool tests for filtering and name resolution.
- [x] Add `toonverse.fetch-full` to return metadata plus the first full chapter page, including `total`, `limit`, and `offset`, while preserving metadata-only `toonverse.fetch`.
- [x] Complete the source request surface: page controls on `toonverse.fetch-full` and `toonverse.fetch-chapter(name, chapterNumber)` for the public reader endpoint.
- [x] Add `toonverse.fetch-chapters(name, minChapterNumber, maxChapterNumber)` for inclusive reader chapter ranges.
- [x] Add `toonverse.download-book` to materialize a selected inclusive chapter range into a published ToolStorageWorkspace, including book/chapter descriptors and page media.
- [x] Run focused server discovery and storage verification.
- [x] Resolve after source implementation and verification completed.
- [x] Honor `@McpConfigureMapping(timeoutMs)` during MCP dispatch so `toonverse.download-book` can use its configured 5-minute deadline rather than the 30-second global default.
- [x] Add focused regression coverage for configured operation timeouts and bounded Toonverse book downloads.

## Deferred

- [ ] Define response interfaces per response kind after the request surface is accepted.
- [ ] Reconfigure generic image/text book tools after individual source tools are accepted.
- [ ] Add further source packages under `dev.mrk.toolspace.openinklibrary.source.<source>`.

# Notes

## 2026-07-17 implementation

- Added `ToonverseTool`, `ToonverseFetchArgs`, `ToonverseSearchArgs`, and `ToonverseSearchRequest` under `source.toonverse`.
- Extended the checked-in Toonverse profile with the verified name-search and filtered-search endpoints.
- `toonverse.fetch` calls name-search first, requires an exact normalized title or slug match, then fetches metadata. It does not silently choose a fuzzy result.
- `toonverse.search` maps the supplied filter vocabulary to the current `/api/series` query contract. The documented `liibrary` spelling is accepted as a compatibility alias and sent as `library`.
- Module test command passed with 15 tests, including annotation scanning and a local auto-configuration context that registers only `ToonverseTool` as an `@McpTool` bean.
- The focused server test was attempted, but Maven stopped in `toolspace/helloworld` before compiling `x-open-ink-library` or the server: `HelloWorldTool.java:45` calls missing `HelloWorldGreetArgs#getName()`.

## 2026-07-17 full book response

- Initially extended `toonverse.fetch`, then corrected the contract after user review: `toonverse.fetch` remains metadata-only and `toonverse.fetch-full` calls the public chapters endpoint after metadata resolution.
- `toonverse.fetch-full` currently carries the source-owned chapter page response. Its request contract now accepts `limit`, `offset`, and `order`; response interface design is deferred.
- Verified the live Solo Leveling chapter endpoint returned `total=205`, `limit=50`, `offset=0`, and 50 chapter items.
- Module test command passed with 16 tests after asserting the metadata-only and complete-fetch payload shapes separately.

## 2026-07-17 request surface

- Added `ToonverseFetchFullArgs` with generic `name`, `limit`, `offset`, and `order` fields.
- Added `ToonverseFetchChapterArgs` with generic `name` and `chapterNumber` fields; source-side name resolution calls the verified `/api/reading/chapter/{slug}/{chapterNumber}` endpoint.
- No new response interfaces were added. The module continues to return raw source data through existing dispatch JSON handling until response-kind schemas are explicitly introduced.
- Module test command passed with 17 tests.

## 2026-07-17 chapter ranges

- Completed the user-provided `fetch-chapters` method skeleton using the existing `ToonverseFetchChaptersArgs` names: `minChapterNumber` and `maxChapterNumber`.
- The method resolves the work only once, fetches chapter reader payloads sequentially in ascending order, and returns raw existing source payloads without introducing a new response interface.
- The original range implementation mistakenly limited requests to 10 chapters; this was corrected after review. Range validation now rejects only null, negative, and inverted bounds.
- A local-source test verifies the intended `0..100` inclusive range, producing 101 reader requests and payloads.
- Module test command passed with 19 tests.

## 2026-07-18 download-book runtime failure

- A real `toonverse.download-book` request for chapters `1..300` returned JSON-RPC `-32603` instead of a source/storage result.
- The tool declares `@McpConfigureMapping(timeoutMs = 300_000)`, but `McpDispatchExecutor` always waits only for `meshingress.dispatch.default-timeout` (30 seconds). The configured handler timeout is not carried into the dispatch handler model.
- A large sequential chapter/media download therefore reaches the global 30-second deadline, is cancelled, and the dispatcher redacts the timeout as `Internal error`.
- The follow-up must carry operation timeout metadata into dispatch and apply it. The request should also be bounded before staging a potentially oversized multi-hundred-chapter download; the configured workspace staging budget is 256 MB by default, while the user-supplied range exceeds the known 202 chapter count.

# Context

- The earlier `2026-07-17-http-book-source-adapters` MVP registered generic `book.image.*` and `book.text.*` tools. This slice intentionally stops exposing those beans without deleting their implementation.
- The caller does not supply a source work ID to `toonverse.fetch`; it supplies `name`, and the source resolves an exact title or slug before fetching metadata.
- Successful source responses use `source: "toonverse"` to identify the platform. A work UUID remains inside source data and is never substituted for the platform name.
- The supplied `toolspace/x-open-ink-library/src/main/resources/toonverse-config.md` describes search filters. The current live browser bundle and public API were checked on 2026-07-17: name lookup is `GET /api/series/search?q=...`; filtered discovery is `GET /api/series?search=...`. The documented bare `/search` path is not currently available from the API host.
- Public metadata, chapter, and search endpoints were checked without an authorization token. This source tool accepts no caller token.
- `toonverse.fetch` is metadata-only. `toonverse.fetch-full` takes `limit`, `offset`, and `order`; `toonverse.fetch-chapter` takes `name` and `chapterNumber`, resolving the source slug before calling the reader endpoint. Response interfaces are intentionally deferred until the request surface is complete.
- `toonverse.fetch-chapters` takes `name`, `minChapterNumber`, and `maxChapterNumber`. The range is inclusive and ascending; it issues one reader request for every supplied chapter number, without an adapter-imposed range cap.

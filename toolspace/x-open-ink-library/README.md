# Open Ink Library

`x-open-ink-library` currently contributes one source-owned MCP tool family:

- `toonverse.fetch`: fetch metadata for one public Toonverse work by exact `name` or Toonverse slug.
- `toonverse.fetch-full`: fetch metadata plus a requested chapter page.
- `toonverse.fetch-chapter`: fetch one reader chapter by work name and chapter number.
- `toonverse.fetch-chapters`: fetch an inclusive range of reader chapter payloads.
- `toonverse.search`: discover public Toonverse works with `name`, genres, type,
  chapter range, rating range, author, status, sorting, and pagination filters.

Source-owned tools live beneath
`dev.mrk.toolspace.openinklibrary.source.<source>`. The first source is
Toonverse. Its profile lives in `src/main/resources/open-ink-library/toonverse.yaml`;
the source package owns its API paths, lookup rules, filters, and response shape.
Each successful response has `source: "toonverse"`; that value identifies the
website, never a work ID.

## Example calls

```json
{"name":"toonverse.fetch","arguments":{"name":"Solo Leveling"}}
```

```json
{"name":"toonverse.fetch-full","arguments":{"name":"Solo Leveling","limit":25,"offset":50,"order":"desc"}}
```

```json
{"name":"toonverse.fetch-chapter","arguments":{"name":"Solo Leveling","chapterNumber":191}}
```

```json
{"name":"toonverse.fetch-chapters","arguments":{"name":"Solo Leveling","minChapterNumber":191,"maxChapterNumber":192}}
```

```json
{"name":"toonverse.search","arguments":{"name":"leveling","type":"manhwa","minRating":4.5,"sortBy":"rating","limit":20}}
```

Both fetch operations require an exact normalized title or slug match and never choose a
fuzzy search result silently. Toonverse is currently public; no caller token is
accepted or persisted.

The request side is intentionally complete before response schemas are
introduced. `fetch-full` accepts generic page controls (`limit`, `offset`,
`order`); `fetch-chapter` resolves a work name to its Toonverse slug and calls
the reader endpoint with the supplied `chapterNumber`.
`fetch-chapters` resolves the work once and retrieves an inclusive, ascending
range of chapter numbers, issuing one reader request per chapter.

## Future generic book tools

The existing generic `book.image.*` and `book.text.*` classes remain in the
module but are deliberately not registered while individual source tools are
being established. Their future argument and result contracts will be
configured after source behavior is accepted.

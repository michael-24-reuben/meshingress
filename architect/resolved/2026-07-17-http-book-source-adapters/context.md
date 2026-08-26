# Context

## Discussion Evolution

### 1. Original concept: browser extraction

The initial idea used Playwright plus per-site selectors. Each metadata field would have an independent textual flow with regex normalization. This was intended to handle noisy values such as a status block containing both `Ongoing` and unrelated chapter text.

### 2. HTTP endpoint discovery

The design changed after identifying usable HTTP endpoints. HTTP extraction was judged easier and more durable than presentation-layer scraping.

The first source sample was Toonverse:

```text
https://api.toonverse.net/api/series/slug/solo-leveling
```

The source returns work/series metadata including title, cover, author, synopsis, publication type, status, ratings, alternate names, artist, publisher, year, chapter counts, genres, and engagement metrics.

### 3. Declarative operation model

A richer YAML model was explored where each operation described method, path, arguments, response selection, exports, and workflow bindings. Operations were considered function-like nodes, with parent result storage and a source-specific compiler.

### 4. Complexity correction

That model was rejected as excessive. It would require maintaining:

- an operation schema;
- argument-binding rules;
- node execution state;
- workflow DAG parsing;
- result persistence;
- compiler contracts;
- schema validation across every layer;
- debugging tools for the workflow engine itself.

The final decision is to keep workflow behavior in code.

## Final Decisions

1. Use HTTP requests instead of Playwright for the MVP.
2. Keep YAML narrow and value-oriented.
3. Keep source workflow and canonical mapping in Java.
4. Expose two general public tools: image books and text books.
5. Treat manga, manhwa, manhua, and webtoons as publication types under `IMAGE_BOOK`.
6. Inject source adapter beans into the compatible general tool.
7. Source adapters are internal providers, not duplicate annotated tools.
8. Use capability-specific interfaces so partial sources remain valid.
9. Use a source registry to validate IDs and dispatch safely.
10. Use canonical Java records and optional external JSON Schema validation.
11. Keep authorization execution-scoped and redacted.
12. Start with Toonverse as the image-book source.

## Tool Framework Constraints

The repository's actual tool policy must be followed:

- public tool classes use `@McpTool`;
- tool classes use `@McpToolMapping("tools")`;
- public methods use `@McpFunction`;
- inputs use `@McpInputField` records or `@McpFunctionParam`;
- scopes use `@McpToolScopes`;
- results use `DispatchExecutionResult`;
- JSON object content uses `ResultContent.object(...)`;
- JSON array content uses `ResultContent.array(...)`;
- beans are contributed through Spring auto-configuration imports.

The source adapter module participates in Spring discovery but should not be treated as an independent MCP function owner.

## Example Toonverse Data Flow

```text
book.image.metadata(source=toonverse, slug=solo-leveling)
  -> ImageBookTool resolves ToonverseImageBookSource
  -> ToonverseClient GET /api/series/slug/solo-leveling
  -> response selector $.data
  -> ToonverseMapper produces ImageBookMetadata
  -> tool serializes canonical result

book.image.chapters(source=toonverse, sourceBookId=<metadata.id>)
  -> ImageBookTool resolves Toonverse chapter capability
  -> ToonverseClient GET /api/series/{id}/chapters
  -> optional limit/order query parameters
  -> response selector $.data
  -> mapper produces chapter metadata list
```

A combined `extract` function may perform both calls internally:

```text
metadata -> metadata.id -> chapters -> canonical complete result
```

## YAML Philosophy

A source YAML file is a mutable API navigation profile, not an executable adapter.

Good YAML values:

```text
base URL
endpoint paths
HTTP methods
header defaults
path/query parameter names
JSON response wrappers
field selectors
selector fallback order
pagination defaults and maxima
authentication mode metadata
```

Bad YAML values:

```text
workflow dependencies
node IDs
foreach loops
conditions
JavaScript
regex programming pipelines
compiler definitions
result persistence rules
```

## Source Package Independence

A new image source should require:

```text
source YAML
source client
source mapper
source adapter implementation
auto-configuration
fixtures/tests
```

It should not require edits to `ImageBookTool` or `ImageBookSourceRegistry`.

## Canonical versus Source-Native Data

The canonical model should contain broadly reusable fields. Source-only fields may be retained under an optional source/catalog/metrics section rather than promoted into the required root schema.

For example, Toonverse fields such as:

```text
approvalStatus
reviewedById
submittedById
featured
trending
isPromoted
userLibrary count
chapterReads
```

are useful but are not core bibliographic metadata.

## Open Decisions

- final package and artifact names;
- exact public function split;
- whether `book.image.extract` is the default function;
- whether chapter page URLs are part of the MVP;
- whether image download is a separate function or module;
- whether raw source payload is returned, stored, or discarded;
- runtime versus test-only JSON Schema enforcement;
- configured secret references versus client-supplied authorization;
- pagination behavior for sources that require multiple requests.

## Safety and Maintenance Notes

- Do not document or imply bypassing access controls.
- Prefer documented/public endpoints where possible.
- Respect source rate limits and terms.
- Never place tokens in YAML fixtures.
- Keep captured response fixtures sanitized.
- Avoid logging complete upstream payloads when they can contain user/account data.
- Keep source selectors under tests so API drift is detected quickly.

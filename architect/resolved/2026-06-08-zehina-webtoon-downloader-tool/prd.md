# PRD: Zehina Webtoon Downloader Tool

## Problem

Meshingress needs a practical Web Comics tool candidate that can fetch public WEBTOON/manhwa series content and metadata through a narrow MCP interface. The upstream Zehina CLI already covers WEBTOON.com downloads, archive output, metadata export, retries, concurrency, and chapter ranges, so a wrapper is preferable to reimplementing a scraper.

## Goals

- Provide a Meshingress tool for user-directed WEBTOON series metadata inspection and export.
- Provide guarded download of public chapters for a specific series URL.
- Preserve upstream limits clearly, especially Daily Pass/app-only/inaccessible content.
- Keep downloads confined to a configured local output root.
- Return structured progress, output paths, and failure details to MCP callers.
- Avoid live-network dependency in automated tests by injecting a fake runner.

## Non-Goals

- Do not scrape all of WEBTOON.com.
- Do not bypass authentication, paywalls, Daily Pass, Fast Pass, app-only restrictions, DRM, region locks, or anti-bot controls.
- Do not vendor Zehina source code into Meshingress unless a later licensing and maintenance decision approves it.
- Do not add a broad manga/manhwa multi-source scraper in this entry.
- Do not store downloaded copyrighted images in repository-managed test fixtures.

## Requirements

- The wrapper must accept only explicit WEBTOON series URLs or approved URL shapes.
- The wrapper must support metadata-only mode before download.
- The wrapper must support chapter range arguments so callers can avoid unnecessary bulk downloads.
- The wrapper must enforce configured output-root containment and reject path traversal.
- The wrapper must expose concurrency/rate settings only within safe configured bounds.
- The wrapper must report upstream CLI version, availability, and command path through a health or inspect response.
- The wrapper must treat inaccessible chapters as expected upstream limitations, not hidden success.
- The wrapper must make copyright/authorization boundaries visible in tool descriptions and error messages.

## Acceptance Criteria

- `tools/list` includes the Zehina-backed functions with clear descriptions and input schemas.
- `webtoon.inspect` returns title, source URL, available chapter metadata, and upstream availability state without writing chapter images.
- `webtoon.export_metadata` writes metadata under the configured output root and returns file paths and counts.
- `webtoon.download_series` can download a selected public chapter range and return output archive/folder paths.
- Invalid domains, path traversal, missing CLI, unsupported content, and inaccessible chapters produce structured MCP errors.
- MVC tests cover the MCP contract with a fake CLI runner.
- No automated test requires a live WEBTOON network call.


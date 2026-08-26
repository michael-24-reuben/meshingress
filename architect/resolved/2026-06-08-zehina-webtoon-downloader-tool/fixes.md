# Fixes

## Files Changed

- `pom.xml`
- `app/meshingress-tool-bundle/pom.xml`
- `toolspace/webtoon-downloader/pom.xml`
- `toolspace/webtoon-downloader/DISCLAIMER.md`
- `toolspace/webtoon-downloader/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- `toolspace/webtoon-downloader/src/main/java/dev/mrk/toolspace/webtoon/*`
- `app/meshingress-server/src/test/java/dev/mrk/meshingress/mcp/WebtoonDownloaderToolMvcTests.java`

## Behavioral Changes

- Added a new `webtoon` MCP tool module with `webtoon.health`, `webtoon.inspect`, `webtoon.export_metadata`, and `webtoon.download_series`.
- Wired the module into the Maven reactor and `meshingress-tool-bundle`.
- Added configuration for executable path, output root, timeout, and concurrency caps.
- Added a `ProcessBuilder` runner abstraction so tests can inject a fake runner and production execution avoids shell string concatenation.
- Added URL validation, output-root containment, range validation, option allowlists, structured command results, timeout/non-zero handling, and missing-CLI reporting.
- Added an explicit module disclaimer for upstream/site volatility and authorization limits.

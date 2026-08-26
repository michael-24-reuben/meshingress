# Context

## Lookup Summary

The prior repo scan identified Zehina/Webtoon-Downloader as the best first candidate for a Web Comics-oriented tool because it is a small Python CLI focused on WEBTOON.com, supports full-series or chapter-range downloads, supports image folders/ZIP/CBZ/PDF output, and supports metadata export.

Live GitHub API check on 2026-06-08:

- `full_name`: `Zehina/Webtoon-Downloader`
- `html_url`: `https://github.com/Zehina/Webtoon-Downloader`
- `description`: `A fast CLI for downloading chapters of Webtoons`
- `license`: `MIT`
- `archived`: `false`
- `pushed_at`: `2026-06-07T17:47:38Z`
- `language`: `Python`
- `stars`: `355`

## Meshingress Fit

This is best modeled as an external CLI-backed tool rather than a native Java scraper. The wrapper should treat Zehina as an executable dependency and keep Meshingress responsible for validation, policy, structured MCP input/output, and safe filesystem boundaries.

Likely module target:

- `toolspace/webtoon-downloader`
- app bundle dependency in `app/meshingress-tool-bundle/pom.xml`
- focused MVC tests in `app/meshingress-server`

## Safety Boundary

The user requested a tool that can download manhwa/Web Comics content and metadata. The safe interpretation is user-directed archival of public, authorized, free content for a specific series URL.

The tool should explicitly refuse or fail closed for:

- site-wide crawling;
- paid, app-only, Daily Pass, Fast Pass, account-only, region-locked, or DRM-protected chapters;
- credential capture or credential replay;
- bypassing anti-bot, paywall, or access controls;
- writing outside the configured download root.

## Related Work

- `2026-05-23-runtime-tool-creation-and-registry`: tool creation/registration context.
- `2026-05-26-tool-runtime-loader`: runtime tool loading context.
- `2026-05-28-direct-registration-hardening`: active direct-registration security boundary.
- `2026-06-08-sql-tool-metadata-store`: possible future location for durable tool/job/download metadata.

## Activation Note

Before implementation, re-read the current toolspace examples. The live checkout includes annotation-based tools as well as `McpToolHandler` support, so the implementation should follow the most current local pattern rather than assuming an older contract.


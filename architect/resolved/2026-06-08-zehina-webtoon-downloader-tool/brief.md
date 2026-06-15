# Brief: Zehina Webtoon Downloader Tool

## Goal

Create a Meshingress tool package that wraps Zehina/Webtoon-Downloader for authorized, user-directed WEBTOON series metadata export and public chapter downloads.

## Candidate Upstream

- Repository: `https://github.com/Zehina/Webtoon-Downloader`
- Docs: `https://zehina.github.io/Webtoon-Downloader/`
- Language: Python
- License: MIT
- Observed status on 2026-06-08: not archived, recently pushed, CLI-oriented

## Intended Tool Surface

The first slice should expose a small MCP surface around a configured local CLI installation:

- `webtoon.inspect`: validate a WEBTOON series URL and return discovered title/chapter metadata without downloading images.
- `webtoon.export_metadata`: write metadata for a public series to a configured output directory, preferably JSON.
- `webtoon.download_series`: download all public chapters or an explicit chapter range for one user-provided WEBTOON series URL.

## Scope Boundary

This entry is a future implementation package. It should not be implemented during unrelated direct-registration hardening work.

The tool must not become a general piracy, credential-bypass, or site-wide crawler feature. "All content" means all public chapters for a specific user-provided series, not all content on WEBTOON.com and not Daily Pass, Fast Pass, app-only, paid, region-locked, DRM-protected, or authentication-only content.


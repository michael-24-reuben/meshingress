# Context

- The source checkout supplied local yt-dlp `2026.07.04` at `d9813a3da6959662841dfb34cad0ee6c07a65d1e`.
- Its local `yt-dlp-ejs 0.8.0` package provides the JavaScript solver files.
- The module stays local because `toolspace/x-*` is ignored in this checkout.
- Host Python and Node are runtime prerequisites; Python receives only the module-owned `vendor/python` path. Node is selected explicitly through `--js-runtimes node`.
- The MCP surface is intentionally constrained to metadata inspection and local download. It does not accept raw yt-dlp flags, cookie files, browser impersonation, custom headers, proxy settings, output paths, or hooks.

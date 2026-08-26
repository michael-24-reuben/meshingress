# Todo

- [x] Reconfirm the upstream Zehina/Webtoon-Downloader CLI flags, metadata output shape, license, and current maintenance status.
- [x] Decide whether Meshingress should call an installed CLI, a virtualenv-managed Python module, or a small sidecar process.
- [x] Define configuration properties for command path, output root, timeout, allowed domains, default save format, and concurrency caps.
- [x] Design input schemas for `webtoon.inspect`, `webtoon.export_metadata`, and `webtoon.download_series`.
- [x] Implement URL validation for WEBTOON series pages and reject non-WEBTOON domains by default.
- [x] Implement output-root containment and path traversal rejection.
- [x] Implement a runner abstraction that avoids shell string concatenation.
- [x] Add structured handling for missing CLI, non-zero exit codes, inaccessible chapters, and upstream rate limiting.
- [x] Add fake-runner tests for `tools/list` and `tools/call`.
- [x] Add documentation that the tool is for authorized/public content only and does not bypass paid, app-only, Daily Pass, or protected chapters.
- [x] Decide whether results should be registered in the future SQL tool metadata store or left as filesystem output only for the first slice.

## Resolution Note

The first slice leaves results as filesystem output under `meshingress.webtoon-downloader.output-root`. SQL job/download metadata remains future work under the separate SQL tool metadata store architect.

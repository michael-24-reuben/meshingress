# Plan

## Proposed Shape

Add a new module under `toolspace/webtoon-downloader` that registers a Spring bean tool and delegates to a small Java runner around the Zehina Python CLI.

Use the current Meshingress tool pattern when this entry is activated. The live repo contains both `McpToolHandler` and annotation-based tool registration support; existing toolspace modules such as `helloworld`, `powershell-cli`, and `voicebox` should be rechecked before implementation starts.

## Implementation Stages

1. Inspect the current toolspace module pattern and choose the narrowest matching wrapper style.
2. Add configuration properties for the CLI command, output root, allowed domains, default format, timeout, and max concurrency.
3. Implement a runner abstraction that builds CLI arguments without shell string concatenation.
4. Implement URL and output path validation before invoking the CLI.
5. Add tool functions for inspect, metadata export, and download.
6. Add tests with a fake runner that returns representative stdout/stderr and exit codes.
7. Attach the module through the tool bundle only after the wrapper has guarded behavior.
8. Add an optional manual smoke note for a public/free WEBTOON URL.

## Design Risks

- Upstream site markup or API changes can break the downloader.
- The CLI may expose options that are too broad for a safe MCP surface.
- Long-running downloads can outlive normal MCP request expectations.
- Downloaded files may be large and should not be placed inside the repository.
- Copyright and terms-of-service boundaries require explicit user-directed behavior.

## Verification Strategy

- Unit-test argument construction and path containment.
- MVC-test `tools/list` and `tools/call` using a fake runner.
- Integration-test CLI availability separately from live content downloads.
- Optional manual smoke can verify one public series metadata export when the user asks for implementation verification.


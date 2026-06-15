# Verification

## Automated

- `.\mvnw.cmd -pl app/meshingress-server -am "-Dtest=WebtoonDownloaderToolMvcTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Result: pass.
  - Coverage: `tools/list` includes Webtoon functions, fake CLI `webtoon.export_metadata` builds the guarded command and stays under output root, and path traversal is rejected before the runner executes.

- `.\mvnw.cmd -pl app/meshingress-server -am "-DskipTests" package`
  - Result: pass after stopping the prior Meshingress JAR process that held `target/meshingress.jar` open.

## Runtime

- Restarted with `meshingress-start.ps1 -Mode Jar`.
- Health endpoint on the bound address `http://100.121.15.11:4737/actuator/health` returned `UP`.
- `tools/list` on `http://100.121.15.11:4737/mcp` returned:
  - `webtoon.health`
  - `webtoon.inspect`
  - `webtoon.export_metadata`
  - `webtoon.download_series`
- `webtoon.inspect` accepted a representative WEBTOON list URL and returned the metadata command template without network download.
- `webtoon.health` reported the wrapper is installed but the external `webtoon-downloader` CLI is not currently available on PATH. This is expected unless the runtime dependency is installed or `meshingress.webtoon-downloader.command` is configured.

## Remaining Risks

- Live downloads were not executed because that would depend on WEBTOON network behavior and an installed external CLI.
- Upstream CLI/site behavior can change; the module disclaimer and health output make that visible.

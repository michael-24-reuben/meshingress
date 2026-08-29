# Notes

The former app remains a source of selected implementation details, not a template to copy wholesale.

## 2026-07-23 preservation checkpoint

`integrations/nextcloud/meshingress` was renamed to `integrations/nextcloud/meshingress-v1`. `appinfo/info.xml` remains present with its original `id` of `meshingress` and version `0.2.0`. No file contents were changed and nothing was deployed.

## Current implementation

The new `integrations/nextcloud/meshingress` implementation deliberately has no version suffix in its path, Architect title, lifecycle type strings, or public route names. It uses a fresh `meshingress_workspaces` / `meshingress_workspace_sources` schema rather than the legacy import tables. A workspace becomes durable `SEALED` before it is woken, sources persist individual completion/error state, and the worker scans `SEALED`, `QUEUED`, and expired `RUNNING` leases. Successful sources are not deleted when a different source retries.

Static verification passed for every new PHP file, the install helper passed `node --check`, and `git diff --check` reported no whitespace errors. No Nextcloud runtime, source URL, deployment, or reader request was executed.

## Live caller evidence

The updated `mcp-ws-download-book.js` was pushed in commits `cb664e1`, `50ad522`, and then run against the current Meshingress deployment. It delegated Toonverse `solo leveling` chapters 0 through 3: the MCP call accepted 4 chapters / 100 pages, and workspace `req_ea55e2f342e34390b0736560e317a514` progressed from `QUEUED` attempt 2 to `COMPLETED`. The foreground execution wrapper drops long-running Node processes after roughly 40 seconds, so the caller now supports resume mode (`MESHINGRESS_SESSION_ID` plus `MESHINGRESS_REQUEST_ID`) and reconnects status polling rather than submitting a duplicate book.

This proves the caller and currently deployed provider can complete a delegated workspace. It does **not** prove the new source at `integrations/nextcloud/meshingress` is deployed; deployment and reader verification remain separate work.

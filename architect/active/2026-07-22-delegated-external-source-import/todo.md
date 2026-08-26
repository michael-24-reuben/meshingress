# Todo

- [x] Add delegated source contracts to `meshingress-tool-api` without converting the SPI to an abstract class.
- [x] Add configuration and lifecycle-policy support for `delegated-external`.
- [x] Implement OPEN/SEALED Nextcloud workspace records and OCS endpoints.
- [x] Implement durable Nextcloud source/job metadata and the OCS adapter.
- [x] Implement reservation, native upload, source append, seal, and status behavior.
- [x] Integrate Toonverse source registration plus native `chapter.json` and `book.json` uploads.
- [x] Repair the Nextcloud file-size metadata write path.
- [x] Run focused Java tests and PHP syntax checks.
- [x] Restore Tailscale SSH authorization, deploy the Nextcloud app migration, and run one live delegated workspace import.
- [x] Rebuild and restart the Meshingress server so its running JAR contains the delegated-external adapter implementation.
- [x] Invoke one small valid `toonverse.download-book` range through MCP and verify a terminal JSON-RPC result contains a Nextcloud request/workspace identifier.
- [x] Poll the returned identifier to `COMPLETED` or an explicit retryable `FAILED` result; verify the final manifest has non-zero byte sizes and viewer-safe MIME metadata.
- [x] Replace protected Nextcloud DAV links with a bounded, viewer-safe stream capability that does not copy the workspace.
- [ ] Add a retained retry/status contract only after the current adapter is proven to produce a durable identifier on both success and failure.

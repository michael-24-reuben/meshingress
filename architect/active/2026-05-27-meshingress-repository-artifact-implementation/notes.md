# Notes

## 2026-05-28

- Use `repository/` as the property-backed repository root for reviewed artifacts and metadata.
- Keep generated/uploaded JAR storage out of `toolspace/` and `lib/`; those directories represent source modules and reactor libraries.
- Start with a fake scanner behind the scanner interface so upload, quarantine, review, and publication flows can be verified before external tools such as Syft, Grype, Trivy, ClamAV, YARA, Semgrep, and Cosign are wired.
- Track direct MCP registration hardening separately so repository implementation does not absorb unrelated minor cleanup.

## Verification

- `.\mvnw.cmd -pl app/meshingress-repository -am test` passed with 1 repository flow test.
- `.\mvnw.cmd -pl app/meshingress-server -am test` passed with 52 server/reactor tests after preserving Spring configuration binding for `MeshingressProperties`.

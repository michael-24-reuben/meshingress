# Notes

## 2026-05-28

- Use `repository/` as the property-backed repository root for reviewed artifacts and metadata.
- Keep generated/uploaded JAR storage out of `toolspace/` and `lib/`; those directories represent source modules and reactor libraries.
- Start with a fake scanner behind the scanner interface so upload, quarantine, review, and publication flows can be verified before external tools such as Syft, Grype, Trivy, ClamAV, YARA, Semgrep, and Cosign are wired.
- Track direct MCP registration hardening separately so repository implementation does not absorb unrelated minor cleanup.

## 2026-05-29

- Approved artifacts remain in `repository/artifacts` as the repository-owned source of truth. The MCP server copies verified artifacts into `meshingress.repository.runtime-cache-root` before loading; installed runtime copies are cache material, not the authoritative reviewed artifact.
- Approved scopes belong in signed publication records. Runtime `application.properties` should hold local policy toggles and signing/repository locations, not a central list of repository-approved scopes.
- Added `roles/tools/installPublication` to accept an approved signed publication record, verify its HMAC signature, verify the repository artifact SHA-256, enforce runtime policy, copy the JAR into the runtime cache, activate it through `ToolRuntimeLoader`, and persist a `PUBLICATION_RECORD` registration.
- Canonicalized `ArtifactPublicationRecord.publishedAt` to UTC so records signed by the repository verify after JSON deserialization in the server.
- Scope review must include artifact-behavior inference. Examples include treating `new File(...)` / `Path.of(...)` as filesystem review signals, `Files.read*` as `FILES_READ`, `Files.write*` as `FILES_WRITE`, `Files.delete*` as `FILES_DELETE`, `ProcessBuilder` / `Runtime.exec` as `SHELL_EXECUTE`, and sockets/HTTP clients as network scope evidence. The first implementation can be heuristic; the point is to catch under-declared or sneaky tools before publication.
- Scope inference should be backed by a rule catalog, but regex should be a last-resort matcher. Prefer source-aware tools such as Semgrep when source is available, bytecode analyzers such as ASM or SootUp for JAR-only artifacts, and SpotBugs/FindSecBugs findings as extra evidence. Regex rules such as `new\\s+File\\s*\\(` should be low-confidence review hints unless no structured analyzer is available.
- Reviewed `scopes-integration.analysis.md` and adopted CodeQL as a preferred source/build-aware scope analyzer, not the canonical scope policy store. The repository should own a versioned scope-to-lookup catalog, generate CodeQL query packs from that catalog, import normalized CodeQL findings into `inferredScopes`, and keep ASM/SootUp bytecode analysis for uploaded JARs that do not have source or a reproducible build.

## Verification

- `.\mvnw.cmd -pl app/meshingress-repository -am test` passed with 1 repository flow test.
- `.\mvnw.cmd -pl app/meshingress-server -am test` passed with 52 server/reactor tests after preserving Spring configuration binding for `MeshingressProperties`.
- `.\mvnw.cmd -pl app/meshingress-server -am "-Dtest=McpPublicationInstallTests" "-Dsurefire.failIfNoSpecifiedTests=false" test` passed with 3 publication-install tests.
- `.\mvnw.cmd -pl app/meshingress-server -am test` passed with 55 server/reactor tests.

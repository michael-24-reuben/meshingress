# Verification

## Commands

- `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=RuntimeToolCacheTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Result: pass; 5 tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=McpPublicationInstallTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Result: pass; 13 tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=RuntimeToolCacheTests,McpPublicationInstallTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Result: pass; 18 tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- `git diff --check -- app/meshingress-server/src/main/java/dev/mrk/meshingress/server/install/RepositoryArtifactFetcher.java app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/RoleToolService.java app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/params/RolesToolInstallPublicationParams.java app/meshingress-server/src/test/java/dev/mrk/meshingress/server/install/RuntimeToolCacheTests.java architect/PAS.md architect/ASSIGNMENT.md architect/resolved/2026-06-14-runtime-publication-install-hardening`
  - Result: pass; no whitespace errors. Git reported LF-to-CRLF warnings for edited files.

## Coverage Added

- Publication records can be fetched from the repository API by coordinate.
- Fetch requests send the configured repository role.
- Coordinate-based publication fetch fails clearly when repository API base URL is not configured.
- Existing inline signed publication install behavior remains wired and passing through the MCP controller path.

## Remaining Risks

- Full end-to-end coordinate install against a live `meshingress-repository` process was not run in this slice; focused fetch tests and existing MCP install tests cover the split boundaries.
- Scanner/sandbox evidence and publication eligibility policy remain deferred follow-up work.
- The worktree contains broad prior dirty/untracked changes that were preserved.

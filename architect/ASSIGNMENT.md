# Architect Assignment

## Assignment Status
- assignmentStatus: active
- lastUpdatedAt: 2026-06-28T01:07:17-04:00
- updatedBy: Codex
- currentBranch: codex/chapter-2-embedded-assessment-enrichment
- expectedBranch: codex/chapter-2-embedded-assessment-enrichment
- objectiveId: 2026-06-28-spotbugs-embedded-java-assessment-adapter
- objectiveTitle: SpotBugs Embedded Java Assessment Adapter
- objectiveStatus: active; next focused embedded Java scanner slice.

## Current Architect Entries
- active: architect/active/2026-06-28-spotbugs-embedded-java-assessment-adapter
- resolved: architect/resolved/2026-06-27-grype-vulnerability-scanner-adapter
- resolved: architect/resolved/2026-06-27-scanner-process-runner-policy
- parentBacklog: architect/active/2026-05-27-meshingress-repository-artifact-implementation
- completed: architect/resolved/2026-06-18-publication-eligibility-policy
- completed: architect/resolved/2026-06-14-repository-scanner-sandbox-pipeline
- completed: architect/resolved/2026-06-05-embedded-assessment-enrichment
- completed: architect/resolved/2026-05-28-direct-registration-hardening
- blockedBy: none known.
- shouldNotTouch: unrelated dirty files, older stale active entries unless selected by PAS/user, scanner adapters other than SpotBugs in the next slice, host scanner installation, or any policy shortcut that treats uploaded `requestedScopes` as trusted authority.

## Work Completed In This Run
- Completed required startup reads and branch/status checks.
- Confirmed branch `codex/chapter-2-embedded-assessment-enrichment` matches expected branch and is not `main`.
- Preserved existing dirty/untracked worktree state.
- Implemented, verified, and resolved `2026-06-27-grype-vulnerability-scanner-adapter`.
- Added configurable Grype vulnerability scanner support through `ScannerProcessRunner`.
- Added Grype JSON vulnerability parsing into normalized `Finding` records.
- Added severity threshold mapping for `BLOCKED`, `REVIEW`, and `PASSED`.
- Added normalized missing/unavailable Grype failure evidence.
- Added raw Grype report retention under artifact assessment directories.
- Wired Grype into repository assessment only when `grype-vulnerability-scanner` is configured in the scanner pipeline.
- Added focused artifact-security and repository tests.
- Marked parent backlog items for Grype integration and vulnerability severity thresholds complete.
- Created active next objective `2026-06-28-spotbugs-embedded-java-assessment-adapter`.
- Updated PAS and this assignment file.

## Active Files

| File | State | Reason |
|---|---|---|
| `architect/active/2026-06-28-spotbugs-embedded-java-assessment-adapter/` | active | Current selected objective for next run. |
| `architect/resolved/2026-06-27-grype-vulnerability-scanner-adapter/` | resolved | Completed objective from this run. |
| `architect/active/2026-05-27-meshingress-repository-artifact-implementation/` | active parent/backlog | Broad source context; do not implement directly as a catch-all. |

## Files Changed By This Run

| File | State | Reason |
|---|---|---|
| `lib/meshingress-artifact-security/src/main/java/dev/mrk/meshingress/artifact/security/GrypeScannerAdapter.java` | added | Grype adapter. |
| `lib/meshingress-artifact-security/src/main/java/dev/mrk/meshingress/artifact/security/GrypeScannerOptions.java` | added | Grype adapter options. |
| `lib/meshingress-artifact-security/src/main/java/dev/mrk/meshingress/artifact/security/GrypeSeverity.java` | added | Severity threshold helper. |
| `lib/meshingress-artifact-security/src/main/java/dev/mrk/meshingress/artifact/security/ScannerRequest.java` | changed | Added optional raw report directory. |
| `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/config/MeshingressRepositoryConfiguration.java` | changed | Added process runner and Grype adapter wiring. |
| `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/config/MeshingressRepositoryProperties.java` | changed | Added Grype config properties. |
| `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/ArtifactService.java` | changed | Passed raw report directory and filtered scanners by configured pipeline. |
| `app/meshingress-repository/src/main/resources/application.properties` | changed | Added Grype defaults. |
| `lib/meshingress-artifact-security/src/test/java/dev/mrk/meshingress/artifact/security/GrypeScannerAdapterTests.java` | added | Focused adapter tests. |
| `app/meshingress-repository/src/test/java/dev/mrk/meshingress/repository/artifact/ArtifactRepositoryGrypeScannerTests.java` | added | Repository Grype wiring test. |
| `architect/resolved/2026-06-27-grype-vulnerability-scanner-adapter/` | moved/changed | Resolution record. |
| `architect/active/2026-06-28-spotbugs-embedded-java-assessment-adapter/` | added | Next objective record. |
| `architect/active/2026-05-27-meshingress-repository-artifact-implementation/todo.md` | changed | Parent checklist update. |
| `architect/PAS.md` | changed | Required closeout update. |
| `architect/ASSIGNMENT.md` | changed | Required closeout update. |

## Unfinished Files

| File | State | Remaining Work |
|---|---|---|
| `architect/active/2026-06-28-spotbugs-embedded-java-assessment-adapter/` | active | Implement and verify. |
| `lib/meshingress-artifact-security/` | Grype complete | Next: SpotBugs dependency/API validation and adapter. |
| `app/meshingress-repository/` | Grype wiring complete | Next: SpotBugs config/wiring only if needed. |

## Decisions Made
- The Grype adapter objective is complete and resolved.
- The next focused objective is an embedded SpotBugs Java assessment adapter.
- The next objective should validate the SpotBugs library invocation path before changing repository behavior.
- FindSecBugs and other scanners remain deferred unless the SpotBugs base adapter lands cleanly.

## Tests Run

| Command | Result |
|---|---|
| `.\mvnw.cmd -pl lib\meshingress-artifact-security -am "-Dtest=GrypeScannerAdapterTests,ScannerProcessRunnerTests" "-Dsurefire.failIfNoSpecifiedTests=false" test` | pass; 6 tests, 0 failures/errors/skips; `BUILD SUCCESS`. |
| `.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=ArtifactRepositoryGrypeScannerTests,ArtifactRepositoryScannerPipelineTests" "-Dsurefire.failIfNoSpecifiedTests=false" test` | first run failed before tests because test fake runner bean name collided with production `scannerProcessRunner`; fixed. |
| `.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=ArtifactRepositoryGrypeScannerTests,ArtifactRepositoryScannerPipelineTests" "-Dsurefire.failIfNoSpecifiedTests=false" test` | pass; 2 tests, 0 failures/errors/skips; `BUILD SUCCESS`. |

## Blockers
- None known.

## Risks
- Broad pre-existing dirty/untracked worktree state remains.
- Grype was verified with sample JSON and fake runner, not a real host Grype installation.
- SpotBugs dependency/API compatibility with Java 25 is not yet validated.

## Exact Next Action
Implement the SpotBugs embedded Java assessment slice: validate the SpotBugs dependency and invocation API against a controlled fixture JAR, add a configurable scanner adapter only if stable, normalize findings, and verify with focused tests.

## Verification Baseline

```powershell
$env:JAVA_HOME='C:\Users\jbeas\scoop\apps\temurin25-jdk\current'
.\mvnw.cmd -pl lib\meshingress-artifact-security -am "-Dtest=GrypeScannerAdapterTests,ScannerProcessRunnerTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=ArtifactRepositoryGrypeScannerTests,ArtifactRepositoryScannerPipelineTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

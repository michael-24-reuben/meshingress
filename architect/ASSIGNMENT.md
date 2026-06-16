# Architect Assignment

## Assignment Status
- assignmentStatus: active
- lastUpdatedAt: 2026-06-16T01:03:17-04:00
- updatedBy: Codex
- currentBranch: codex/chapter-2-embedded-assessment-enrichment
- expectedBranch: codex/chapter-2-embedded-assessment-enrichment
- objectiveId: 2026-06-14-repository-review-security-audit
- objectiveTitle: Repository Review Security and Audit
- objectiveStatus: active

## Current Architect Entries
- primary: architect/active/2026-06-14-repository-review-security-audit
- pendingFollowUps: architect/pending/2026-06-14-repository-scanner-sandbox-pipeline, architect/pending/2026-06-14-publication-trust-provenance-policy, architect/pending/2026-06-14-runtime-publication-install-hardening
- recentlyVerifiedResolved: architect/resolved/2026-06-05-embedded-assessment-enrichment, architect/resolved/2026-06-08-sql-tool-metadata-store, architect/resolved/2026-05-28-direct-registration-hardening, architect/resolved/2026-06-14-repository-fake-scanner-removal
- related: architect/resolved/2026-05-27-meshingress-repository-artifact-implementation, architect/pending/2026-05-23-runtime-tool-creation-and-registry
- blockedBy: none
- shouldNotTouch: unrelated dirty worktree files, external scanner CLI integrations, scanner sandbox pipeline, publication provenance policy, and runtime install hardening unless explicitly promoted.

## Current Objective
- goal: Secure repository upload/review/publication APIs and make lifecycle transitions auditable.
- scope: Repository endpoint role gates, API-only review visibility, and durable lifecycle event metadata for actor/request/state/reason.
- nonGoals: External scanner sandboxing, dependency-aware SBOM enrichment, stronger publication signing, runtime install hardening, direct registration SQL migration, and UI unless selected after the API/security foundation is complete.
- completionCriteria: Unauthorized users cannot approve or publish; pending review artifacts can be listed through a role-gated API; repository lifecycle events record actor, request id, timestamp, old state, new state, and reason; focused repository tests pass.

## Last Run Summary
- runEndedAt: 2026-06-16T01:03:17-04:00
- workCompleted: Switched from `main` to expected branch `codex/chapter-2-embedded-assessment-enrichment`; added API-only pending review queue support; added a SQL-backed queue read contract and response item; verified the queue preserves `requestedScopes`, inferred scopes, approved scopes, and denied scopes as separate fields.
- workPartiallyCompleted: Reject/revoke/delete/restore lifecycle operations remain open. Metrics/UI and the scanner/provenance/runtime follow-up entries remain deferred.
- testsRun: `.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=ArtifactRepositoryFlowTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- testResult: pass; repository flow tests ran 3 tests with 0 failures, 0 errors, and 0 skips.
- commitCreated: no
- commitHash: none

## Active Files

| File | State | Reason |
|---|---|---|
| `architect/PAS.md` | changed this run | Live persistent state now records the completed review queue slice and next lifecycle action. |
| `architect/ASSIGNMENT.md` | changed this run | Handoff now resumes at the remaining lifecycle transition work. |
| `architect/active/2026-06-14-repository-review-security-audit/` | active | Entry remains active; review queue is complete but reject/revoke/delete/restore are not. |
| `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/ArtifactReviewQueueItem.java` | added | Queue response item for artifact metadata plus scanner assessment results. |
| `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/ArtifactController.java` | changed | Adds `GET /artifact/reviews/pending` behind `RepositoryAction.READ`. |
| `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/ArtifactService.java` | changed | Exposes the queue through `reviewQueue()`. |
| `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/store/ArtifactMetadataStore.java` | changed | Adds `findPendingReviewArtifacts()`. |
| `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/store/SqlArtifactMetadataStore.java` | changed | Reads `REVIEW_PENDING` artifacts joined to stored assessment payloads. |
| `app/meshingress-repository/src/test/java/dev/mrk/meshingress/repository/artifact/ArtifactRepositoryFlowTests.java` | changed | Adds queue authorization, queue content, scope separation, and post-approval removal assertions. |

## Unfinished Files

| File | State | Remaining Work | Safe Next Action |
|---|---|---|---|
| `architect/active/2026-06-14-repository-review-security-audit/todo.md` | active | Reject/revoke/delete/restore lifecycle operations remain open. | Select the smallest next transition, likely reject. |
| `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/ArtifactController.java` | partial | No reject/revoke/delete/restore endpoints yet. | Add one endpoint after state semantics are chosen. |
| `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/ArtifactService.java` | partial | No transition methods for reject/revoke/delete/restore. | Add the selected transition and lifecycle event append. |
| `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/store/ArtifactMetadataStore.java` | partial | No lifecycle history read API. | Add only if required by tests or endpoint response. |
| `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/store/SqlArtifactMetadataStore.java` | partial | Lifecycle history is not exposed. | Keep append-only unless the next slice needs history reads. |

## Next Files To Touch

| File | Planned Change | Depends On |
|---|---|---|
| `architect/active/2026-06-14-repository-review-security-audit/plan.md` | Refine transition ordering if needed. | State semantics decision. |
| `architect/active/2026-06-14-repository-review-security-audit/todo.md` | Mark the selected lifecycle transition complete when implemented. | Implementation. |
| `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/ArtifactController.java` | Add the next transition endpoint. | Transition selection. |
| `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/ArtifactService.java` | Apply state update and lifecycle audit event. | Transition selection. |
| `app/meshingress-repository/src/test/java/dev/mrk/meshingress/repository/artifact/ArtifactRepositoryFlowTests.java` | Add focused transition assertions. | Endpoint/service implementation. |

## Decisions Made
- Decision: The review queue endpoint remains in `ArtifactController` for now.
  - Evidence: active architect notes and `ArtifactController`.
- Decision: `GET /artifact/reviews/pending` is an authenticated read surface using the existing read role policy.
  - Evidence: `ArtifactController` and focused tests.
- Decision: Queue responses use existing artifact metadata and scanner results rather than a new mutable review record.
  - Evidence: `ArtifactReviewQueueItem` and `SqlArtifactMetadataStore`.

## Blockers
- Blocker:
  - Impact: none
  - Required resolution: none
  - Recorded in: none

## Risks
- Risk: Existing dirty worktree contains unrelated edits and untracked files.
  - Mitigation: Do not clean, revert, move, or delete unrelated files.
- Risk: Header-based auth is only the first authorization slice.
  - Mitigation: Preserve the policy boundary for future stronger auth.
- Risk: Lifecycle transition semantics can spill into provenance/runtime policy.
  - Mitigation: Keep the next run focused on one repository-local transition.

## Next Action
Continue the active repository security/audit architect by adding the smallest missing lifecycle transition, preferably `reject` for `REVIEW_PENDING` artifacts: define the request shape, require reviewer/admin authorization, update artifact trust status to `REJECTED`, append actor/request-id lifecycle metadata, and verify with `ArtifactRepositoryFlowTests`.

## Resume Commands

```bash
git status
git branch --show-current
./mvnw.cmd -pl app\meshingress-repository -am "-Dtest=ArtifactRepositoryFlowTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

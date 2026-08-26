# Notes

## 2026-06-15

- Activated from PAS after the 11am run advanced the assignment pointer.
- Endpoint inventory:
  - `POST /artifact/{groupId}/{artifactId}/{version}` uploads into quarantine.
  - `GET /artifact/{groupId}/{artifactId}/{version}/metadata` reads stored artifact metadata.
  - `POST /artifact/{groupId}/{artifactId}/{version}/assess` runs repository assessment and stores scanner results.
  - `GET /artifact/{groupId}/{artifactId}/{version}/assessment` reads scanner results.
  - `POST /artifact/{groupId}/{artifactId}/{version}/approve` performs human review approval.
  - `POST /artifact/{groupId}/{artifactId}/{version}/publish` signs the publication record.
  - `GET /artifact/{groupId}/{artifactId}/{version}/publication` reads the publication record.
- First-slice role model should remain header-based and local to the repository app: `X-Repository-Role` accepts `uploader`, `reviewer`, `publisher`, and `admin`; `admin` may perform all repository actions.
- Implemented `RepositoryAccessPolicy`, `RepositoryRequestContext`, and `RepositoryAccessDeniedException`.
- `ArtifactController` now gates:
  - `upload` with `uploader` or `admin`.
  - `assess` and `approve` with `reviewer` or `admin`.
  - `publish` with `publisher` or `admin`.
  - read endpoints with any repository role.
- Added `ArtifactLifecycleEvent` and changed SQL lifecycle event writes to persist `actor` and `request_id` with existing state/reason/timestamp fields.
- Added schema migration statements for existing lifecycle event tables: `actor` defaults to `unknown`, `request_id` is nullable.
- Verification passed: `.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=ArtifactRepositoryFlowTests" "-Dsurefire.failIfNoSpecifiedTests=false" test` with 3 tests.
- Decision: no new chapter/branch is needed for this slice; continue on `codex/chapter-2-embedded-assessment-enrichment` until the repository security/audit architect is either resolved or intentionally split.

## 2026-06-16

- Added API-only pending review queue support in the repository app.
- Chose `ArtifactController` for the first queue endpoint because all current repository artifact endpoints live there and this slice does not introduce separate review workflow commands yet.
- Added `ArtifactReviewQueueItem` with the stored `ArtifactRecord` plus scanner assessment results.
- Added `ArtifactMetadataStore.findPendingReviewArtifacts()` and implemented it in `SqlArtifactMetadataStore` with a SQL join over artifact metadata and assessment rows, filtered to `REVIEW_PENDING`.
- Added `GET /artifact/reviews/pending` and gated it through the existing `RepositoryAction.READ` role policy.
- Updated `ArtifactRepositoryFlowTests` to assert unauthenticated queue access is denied, reviewer queue access returns pending artifact metadata plus `cyclonedx-sbom` and `bytecode-scope-scanner` assessment evidence, uploaded `requestedScopes` remain separate from inferred/approved/denied scopes, and the artifact leaves the queue after approval.
- Verification passed: `.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=ArtifactRepositoryFlowTests" "-Dsurefire.failIfNoSpecifiedTests=false" test` with 3 tests.

## 2026-06-16 Reject Slice

- Added `POST /artifact/{groupId}/{artifactId}/{version}/reject` in `ArtifactController`.
- Added `RepositoryAction.REJECT` and allowed reviewer/admin roles through the existing repository access policy.
- Added `ArtifactLifecycleGuard.requireAssessedBeforeRejection(...)` so reject cannot bypass the durable `ASSESS` lifecycle event.
- Added `ArtifactService.reject(...)` to move only `REVIEW_PENDING` artifacts to `REJECTED`, preserve requested and inferred scope evidence, keep approved scopes empty, carry denied scopes from the reviewer request, and append a `REJECT` lifecycle event with actor/request id.
- Added focused `ArtifactRepositoryFlowTests` coverage for reject stage-order enforcement, role authorization, pending queue removal, rejected publish blocking, and persisted `REJECT` audit metadata.
- Verification passed: `.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=ArtifactRepositoryFlowTests" "-Dsurefire.failIfNoSpecifiedTests=false" test` with 4 tests.

## 2026-06-17 Revoke Slice

- Added `POST /artifact/{groupId}/{artifactId}/{version}/revoke` in `ArtifactController`.
- Added `RepositoryAction.REVOKE` and allowed publisher/admin roles through the existing repository access policy.
- Added `ArtifactLifecycleGuard.requirePublishedBeforeRevocation(...)` so revoke cannot bypass the durable `PUBLISH` lifecycle event.
- Added `ArtifactService.revoke(...)` to mark the artifact metadata `REVOKED`, persist a re-signed publication record with `revoked: true`, reject repeat revocation, and append a `REVOKE` lifecycle event with actor/request id/state/reason.
- Added focused `ArtifactRepositoryFlowTests` coverage for revoke stage-order enforcement, role authorization, persisted revoked publication metadata, repeat-revoke blocking, and persisted `REVOKE` audit metadata.
- Verification passed: `.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=ArtifactRepositoryFlowTests" "-Dsurefire.failIfNoSpecifiedTests=false" test` with 5 tests.

## 2026-06-17 Delete/Restore Slice

- Added `DELETED` as a non-installable artifact trust status.
- Added admin-only `RepositoryAction.DELETE` and `RepositoryAction.RESTORE` actions.
- Added `POST /artifact/{groupId}/{artifactId}/{version}/delete` and `POST /artifact/{groupId}/{artifactId}/{version}/restore`.
- Delete is soft-delete/state-only: it changes artifact metadata to `DELETED`, does not remove the stored artifact blob, rejects repeat deletion, and blocks deletion of an active non-revoked publication.
- Restore requires the current artifact to be `DELETED`, reads the latest durable `DELETE` lifecycle event, and restores the prior trust status from that event's `from_state`.
- Added a latest lifecycle event lookup to the SQL metadata store for restore.
- Added focused `ArtifactRepositoryFlowTests` coverage for restore-before-delete rejection, active-publication delete rejection, admin-only role gates, soft-delete persistence, restore, and persisted `DELETE`/`RESTORE` audit metadata.
- Verification passed: `.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=ArtifactRepositoryFlowTests" "-Dsurefire.failIfNoSpecifiedTests=false" test` with 6 tests.

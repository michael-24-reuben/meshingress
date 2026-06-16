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

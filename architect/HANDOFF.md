# Assignment Handoff

## Assignment Status
- assignmentStatus: idle
- lastUpdatedAt: 2026-08-29T17:15:47-04:00
- updatedBy: Codex
- currentBranch: development
- expectedBranch: development
- objectiveId: 2026-08-29-storage-provider-nodes-and-nextcloud-sdk-extraction
- objectiveTitle: Storage Provider Nodes and Nextcloud SDK Extraction
- objectiveStatus: pending

## Current Architect Entries
- primary: architect/pending/2026-08-29-storage-provider-nodes-and-nextcloud-sdk-extraction
- terminalEntry: architect/resolved/2026-08-28-tool-distribution-modules
- related: architect/resolved/2026-05-27-server-side-tool-registration-phases; architect/active/2026-07-14-tool-provisioning-module
- pendingFollowUps: finalize the `McpNode` grammar, provider discovery and selection, provider lifetime/access model, scope enforcement, and Nextcloud SDK adapter boundary.
- blockedBy: user design decisions; source implementation is deliberately out of scope.
- shouldNotTouch: main branch, pre-existing staged generated artifacts, source, provider properties, and runtime wiring.

## Objective Scope
- goal: Define provider nodes and the path to extract Nextcloud and local storage from native app ownership into module-owned capabilities.
- scope: Architecture record only: capability contracts, node taxonomy, discovery, typed internal composition, configuration ownership, cache exception, authorization questions, and the external SDK boundary.
- nonGoals: Java implementation, Maven dependency changes, application-property migration, service discovery endpoints, provider construction, and test changes.
- completionCriteria: The pending record captures the agreed direction and the decisions required before a separate implementation record can be activated.
- currentLifecycleState: pending.

## Last Run Summary
- runEndedAt: 2026-08-29T17:15:47-04:00
- outcome: completed as documentation-only planning.
- workCompleted: Read the filtered session dialogue, reviewed related Architect entries and the present storage/Nextcloud layout, and created the pending design record.
- workPartiallyCompleted: The raw transcript named by the user was not found at the supplied repository-relative path; the filtered dialogue was available and recorded as the primary reference.
- testsRun: none; no source behavior changed.
- testResult: not applicable.
- verificationSetup: Metadata parse and record reread.
- commitCreated: no
- commitHash: n/a

## Files Changed By This Run

| File | State | Reason | Verification |
|---|---|---|---|
| architect/pending/2026-08-29-storage-provider-nodes-and-nextcloud-sdk-extraction/{meta.json,brief.md,context.md,prd.md,plan.md,todo.md} | added | Capture the agreed boundaries, current evidence, open decisions, requirements, and subsequent planning sequence. | Metadata parse and record reread. |
| architect/{ASSIGNMENT.md,HANDOFF.md} | modified | Keep root handoff state aligned with the pending design record. | Reread. |

## Existing Dirty Work Preserved

| Area | State | Handling | Reason |
|---|---|---|---|
| target/agents/mockito-core.jar | staged | preserved | Pre-existing generated artifact. |
| toolspace/x-open-ink-library/target/agents/mockito-core.jar | staged | preserved | Pre-existing generated artifact. |
| toolspace/yt-dlp/vendor/python/yt_dlp/**/__pycache__/*.pyc | staged | preserved | Pre-existing vendor-generated artifacts. |
| Existing source edits and unrelated Architect lifecycle maintenance | dirty | preserved | Outside this design-only record. |

## Unfinished Files

| File | State | Remaining Work | Safe Next Action |
|---|---|---|---|
| lib/meshingress-tool-api/src/test/java/dev/mrk/meshingress/api/result/DispatchExecutionResultTests.java | needs-review | Existing wire-shape expectation fails the full baseline. | Create a new architect entry before diagnosing or changing it. |

## Design Decisions Recorded

- Every real storage provider, including local storage, moves to an attachable module; native cache storage stays temporary infrastructure only.
- `ToolStorageService` and `StorageFileApi` remain provider-neutral capability contracts, not node registrations.
- A concrete `ToolStorageService` implementation is a provider node. It composes with consumers in-process through typed capability access, not an MCP call.
- `@McpNode` is the agreed umbrella term. Its exact Java annotation grammar remains open.
- Provider-specific configuration, credentials, constructor inputs, and scopes belong to the provider module. They must not remain in provider-specific `meshingress.storage.*` application properties.

## Blockers

- Implementation is intentionally blocked until the user approves the open design decisions in the pending record.

## Risks

- Risk: `@McpNode` has an agreed role but no approved Java annotation grammar.
  - Mitigation: Do not change annotation APIs until its kind model and backward compatibility are decided.
- Risk: Passing provider identity through response results could conflict with caching, replay, and instance lifetime.
  - Mitigation: Keep live typed resolution separate from `DispatchExecutionResult` until those semantics are designed.
- Risk: The sibling Nextcloud SDK has an independent runtime/API model and documentation/version drift.
  - Mitigation: Approve an adapter/dependency strategy before adding any Maven dependency.
- Risk: The full test baseline remains red at an unmodified API test.
  - Mitigation: Preserve the earlier terminal record; do not attribute that failure to this planning work.

## Verification Details

- commandsRun: read-only Architect and source review; JSON metadata parse; record reread.
- result: the planning record and root handoff state are internally documented. No build or test was run because no executable artifact changed.
- knownGap: The raw transcript was not present at the supplied repository-relative path. The filtered dialogue was available and used.
- preservedBaseline: The earlier full-suite API assertion failure remains outside this record.

## Repository State

- gitStatusSummary: Existing dirty source/generated/Architect work preserved; this run adds only the pending record and updates Architect root handoff metadata.
- branch: development
- safeDirectoryRequired: no
- commitCreated: no
- commitHash: n/a

## Next Action

Continue the design discussion through `architect/pending/2026-08-29-storage-provider-nodes-and-nextcloud-sdk-extraction`. Once the open decisions are approved, create and activate a source-level implementation record before modifying the application or modules.

## Resume Commands

```powershell
git status --short
Get-Content architect\pending\2026-08-29-storage-provider-nodes-and-nextcloud-sdk-extraction\context.md
Get-Content architect\pending\2026-08-29-storage-provider-nodes-and-nextcloud-sdk-extraction\todo.md
```

## Handoff Integrity Checklist

- [x] Pending design entry is recorded.
- [x] No implementation was performed.
- [x] Existing dirty worktree boundaries are recorded.
- [x] Open decisions and next action are listed.

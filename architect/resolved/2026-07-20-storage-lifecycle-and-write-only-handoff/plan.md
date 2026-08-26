# Plan

## 1. Finalize the configuration and property model

1. Replace the single filesystem-oriented storage property record with lifecycle, local, external, target, and shared metadata records matching `storage.properties.md`.
2. Bind `lifecycle` to a closed enum and reject `external-local` structurally.
3. Validate lifecycle/target capability combinations at startup.
4. Keep external target connection settings provider-specific; keep SQL metadata configuration shared.
5. Decide and document the precedence of `table-prefix` and explicit table names. Explicit table names should be exact identifiers, with the prefix used only to derive defaults.

## 2. Add the storage operation model

1. Place provider-agnostic file-operation contracts in the appropriate shared storage API module.
2. Define operation requests, write handles, file keys, metadata, capabilities, receipts, and stable result categories.
3. Implement the complete method vocabulary from the PRD on the common API.
4. Require callers to inspect capabilities before use; enforce denied/unsupported results again at the backend boundary.
5. Separate locally recorded audit metadata from foreign-object observation methods.

## 3. Refactor the local workspace backend

1. Adapt the existing local `WorkspaceFiles` behavior to the new operation contract.
2. Preserve same-filesystem staging and atomic local publication for `local-local`.
3. Enforce create-only semantics for final paths.
4. Keep local deletion restricted to uncommitted staging, locally published expiry/exhaustion cleanup, and explicit local lifecycle management.

## 4. Implement external handoff adapters

1. Introduce a provider adapter boundary that resolves a named external target.
2. Implement target capability declarations before allowing a target to be selected.
3. Implement create-only upload and receipt capture for the first selected provider.
4. Add WebDAV only when conditional create is demonstrably enforced by the target server; do not accept a potentially overwriting `PUT` as compliant.
5. Add Google Drive and OneDrive upload-session adapters only with capability tests that prove their allowed write sequence and prohibit post-publication access.

## 5. Persist lifecycle and audit state

1. Extend the shared metadata schema using the configured SQL table names.
2. Record local staging, local publication, external handoff, conflict, and finalization events without storing bytes or secrets.
3. Ensure externalized records leave active local workspace quotas while remaining available for audit.
4. Ensure cleanup cannot issue foreign operations.

## 6. Verify

1. Unit-test every lifecycle/target validation combination.
2. Unit-test capability declarations and explicit denied/unsupported results for every operation.
3. Test local create collision, staging write, completion, retrieval, expiry, and cleanup.
4. Test external handoff receipt persistence, collision failure, no foreign cleanup, and no foreign observation call.
5. Run focused server/module tests and then the Maven reactor tests appropriate to the changed modules.

## Risks and controls

| Risk | Control |
|---|---|
| A provider API can overwrite despite a `create-only` label. | Require an enforceable provider primitive and reject unsupported targets at startup. |
| A generic interface accidentally invites foreign reads. | Capabilities plus backend enforcement; use locally recorded handoff metadata for audit queries. |
| Audit records become physical-quota records. | Model externalized records separately from active local workspaces. |
| Provider upload sessions require multiple writes. | Permit operations only while a write handle is uncommitted; revoke post-completion mutation. |
| Existing local behavior regresses during refactor. | Preserve and extend focused workspace storage tests before adding external adapters. |

# Implementation Notes

## 2026-08-20 activation

- Implementation is authorized on `development`.
- Preserve the unrelated dirty Studio/MCP/tool-module edits already present in the worktree.
- The root Architect assignment currently belongs to the separate Studio-login objective, so this entry must not replace its assignment or handoff state.
- Start at the client-side `local-workspace.ts`, `WorkspaceDialog.tsx`, and `WorkflowStudioPage.tsx` boundary. The new-workspace flow needs a writable parent handle and must activate the generated child handle.

## 2026-08-20 implementation slice

- Added `workspace-layout.ts`, which pairs the immutable `workflow-project` layout v1 structural contract with its exact scaffold directories and root-file templates.
- `createLocalWorkspaceProject(...)` now validates the requested child folder name, requires a writable File System Access parent handle, rejects an existing directory or file at the target name, writes only V1-relative paths, validates the generated tree, and returns the child handle.
- The dialog now calls the selected path a parent destination, retains creation failures in the dialog, and explains that the generated root is `<folder>/<workspace name>`.
- `WorkflowStudioPage` now uses the dedicated writable parent picker, creates the child project, and activates/persists that child rather than the selected parent.
- Focused in-memory File System Access verification passed for V1 tree/manifest generation, returned-child activation input, target collision rejection, denied-write non-mutation, and invalid names.
- `npx tsc --project tsconfig.app.json --noEmit --noUnusedLocals false --noUnusedParameters false --ignoreDeprecations 6.0` and `npx vite build` passed.
- The normal `npm run build` remains blocked before feature type checking by existing TypeScript 6 `baseUrl` deprecation configuration. `npm run lint` remains blocked by pre-existing hook violations in `components/panel-catalog.tsx`; no workspace-feature lint errors were reported.
- Live browser interaction could not be completed because the in-app browser bridge timed out while attaching to the local Vite server. The locally started Vite helper was stopped afterward.

## 2026-08-20 schema-cache extension

- The v1 runtime space now scaffolds `var/cache/tool-output-schemas/declared/` and `var/cache/tool-output-schemas/observed/`. Both remain generated, browse-only, and ignored through the existing `/var/` rule.
- This cache is intentionally schema-only: it holds the tool identity, source, fingerprint, capture time, and JSON Schema, never the original tool result or any result values.

## 2026-08-22 Saved tools extension

- Saved tool bookmarks are workspace-local state at `.meshingress/saved-tools.json`, an ignored local-state space already created by the V1 layout. The V2 document stores sorted, de-duplicated `{ toolId, moduleToolId }` references only; it never snapshots registered-tool metadata.
- On workspace activation or restoration, Studio reads those references and resolves the Saved tree against the live registered-tool catalog. The composite reference prevents two modules with the same callable tool ID being merged. A missing state file means no saved tools; malformed state reports an Editor error instead of being silently overwritten. The V1 ID-only document remains readable for the just-introduced format.
- The node context action now saves/removes the callable tool for the entire active workspace and updates every matching node’s Saved state. No writable File System Access handle means the action reports that a local workspace must be opened; browser `localStorage` remains restricted to recent-workspace metadata.
- `npm run build` and `git diff --check` passed. `npm run lint` remains blocked by the existing `panel-catalog.tsx` hook-rule violations.

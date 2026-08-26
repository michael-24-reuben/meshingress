# Context

## Existing Studio behavior

- `TopBar.tsx` already renders `.brand-menu` and routes `File -> New workspace` to `onNewWorkspace`.
- `WorkspaceDialog.tsx` already collects a name and a directory selection, but says Studio will not create or write files.
- `WorkflowStudioPage.tsx` currently calls `activateWorkspace(name, selection)` for creation, so it opens the selected folder as the workspace and reports that no files were created.
- `local-workspace.ts` uses the browser File System Access API where available, persists directory handles in client IndexedDB, and falls back to a directory-input picker for browsing. Its current handle model exposes read permission only.

The feature must replace the current create behavior without regressing explicit client folder selection, stored-handle restoration, recents, or the Explorer's local-only boundary.

## Layout package model

A layout package is immutable by identity and version:

```text
<layout id>/<layout version>/
  contract.yaml
  scaffold/
```

`scaffold/` is the exact generated template tree. `contract.yaml` maps that version's paths to semantic Studio spaces and policies. They are released, reviewed, and consumed together.

The first package is proposed as `workflow-project/v1`, generating:

```text
<workspace-name>/
  meshingress.project.yaml
  README.md
  src/workflows/
  src/transforms/
  src/integrations/
  resources/assets/
  resources/templates/
  resources/fixtures/
  config/environments/
  tests/workflows/
  tests/fixtures/
  var/
  .meshingress/
  .gitignore
  .env.example
```

`meshingress.project.yaml` records the selected layout ID and version. Studio opens a project by discovering and validating this manifest, then loading the exact matching contract. A layout v2 is a distinct package with its own scaffold, contract, and deliberate migration definition; it does not alter v1's meaning.

## Security and filesystem boundary

- The folder chosen in the dialog is a parent destination, not the workspace root.
- The workspace name is a single directory name. It must reject blank values, path separators, dot segments, and platform-invalid names before any write.
- Creation must request or verify write permission for the chosen parent. Read-only handles may still be used for opening existing workspaces.
- All generated paths are fixed layout-relative paths and must remain beneath the new workspace root. Do not accept arbitrary destination paths from the manifest or UI.
- If the target directory already exists, stop with a clear conflict rather than merge or overwrite.
- If creation cannot complete, preserve the created directory for explicit user inspection/retry; do not recursively delete client content automatically.
- Browser fallback environments that can only enumerate folders must clearly report that new-workspace creation requires a writable File System Access implementation. They must not claim the scaffold was created.

## Related but separate work

`2026-08-12-versioned-tool-workspace-delegation` concerns version-bound server/runtime storage for published tool artifacts. This feature concerns client-local Studio projects and must not conflate the two ownership models.

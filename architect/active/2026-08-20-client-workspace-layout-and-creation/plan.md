# Plan

1. Define the typed layout-package registry and its V1 `workflow-project` scaffold/contract assets in the Studio-owned frontend boundary.
2. Extend local filesystem abstractions to distinguish read-only opening from parent-directory `readwrite` creation, including mockable browser-handle types.
3. Implement name validation, collision detection, containment checks, and idempotent materialization of only the V1 generated paths.
4. Change `WorkspaceDialog` wording and callbacks so its folder field explicitly means parent destination and its submit result is a created project handle, not the parent selection.
5. Change `WorkflowStudioPage` creation wiring to activate, persist, index, and record the newly created child project only after scaffold and contract validation succeed.
6. Add opening/validation support that discovers `meshingress.project.yaml` and resolves the exact layout ID/version before applying space-specific Studio behavior.
7. Add unit coverage for V1 generation, invalid names, target conflicts, containment, write-permission denial, unsupported fallback behavior, child-root activation, persisted recents, and V1/V2 compatibility separation.
8. Perform browser verification with a real parent-folder selection, confirm generated paths, reload the new child project, and confirm no client project content was sent to server storage.

## Migration rule

Never mutate a released layout package in place. A structural change creates a new layout version with a separately reviewed scaffold, contract, and optional migration artifact.

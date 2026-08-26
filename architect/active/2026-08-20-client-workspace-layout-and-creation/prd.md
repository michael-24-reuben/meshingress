# Product Requirements: Client Workspace Layout Packages and Creation

## Layout registry

Studio ships or otherwise resolves a trusted layout registry. Each registered package contains:

| Item | Responsibility |
|---|---|
| layout ID | Stable family name, for example `workflow-project`. |
| layout version | Immutable structural release, beginning at `1`. |
| scaffold | The exact directory/file template generated into a new project. |
| structural contract | Semantic space mapping, policies, required paths, ignored paths, and validation rules. |
| migration metadata | Optional explicit path from one version to another; absent means no automatic upgrade path. |

The scaffold and contract must be selected together. A contract cannot point to a scaffold from another layout version.

## V1 structural contract

The `workflow-project/v1` contract must at minimum establish these semantic spaces:

| Space | Relative root | Role | Studio policy |
|---|---|---|---|
| workflows | `src/workflows` | workflow source | Browse, edit, validate, and run eligible workflow definitions. |
| transforms | `src/transforms` | transform source | Browse, edit, and validate reusable transforms. |
| integrations | `src/integrations` | integration definitions | Browse, edit, and validate project-local tool bindings. |
| resources | `resources` | assets | Browse and reference as workflow inputs; never execute as workflows. |
| configuration | `config` | non-secret configuration | Browse, edit, and validate safe configuration. |
| tests | `tests` | test source | Browse, edit, validate, and execute only as tests. |
| runtime | `var` | generated output | Studio-generated and Git-ignored; never source-controlled project input. |
| local state | `.meshingress` | local Studio state | Studio-generated, Git-ignored, and not a portable project source of truth. |

Secrets are excluded from the contract's configuration space. The initial project may contain `.env.example`, but actual `.env` files and secret-named files are local/private and ignored.

## New workspace flow

1. The user opens `.brand-menu` then selects `File -> New workspace`.
2. The dialog requires a workspace name and a selected parent folder.
3. Before creating anything, Studio validates the name and asks for `readwrite` permission on the selected parent handle.
4. Studio resolves the v1 layout package and confirms `<parent>/<name>` does not already exist.
5. Studio creates the child project directory, materializes only the approved scaffold files/directories, and writes the root manifest with `layout.id` and `layout.version`.
6. Studio validates the generated project against that same v1 contract.
7. Studio stores a client-local handle for the newly created child directory, updates recents, and activates/opens that child as the workspace.
8. The user-facing status identifies the new project path/name, never the parent as the opened workspace.

## Loading and compatibility

Opening a workspace discovers `meshingress.project.yaml` at its root. Studio reads the declared layout ID/version, obtains that exact contract, validates the tree, and treats validation failures as actionable diagnostics. It must not reinterpret a v1 project through a v2 contract.

An explicit future `Upgrade workspace layout` flow may apply an authored migration. It must preview changes, require client approval, preserve files outside managed layout paths, and record the new layout version only after verification.

## Acceptance criteria

- `File -> New workspace` creates a named child project under the selected parent, never directly in or as the selected parent.
- The generated root contains a v1 manifest and every required v1 scaffold path.
- The created child project, not its parent, is immediately shown in Explorer, persisted as the recent workspace, and restored as the workspace handle.
- Existing target directories are never merged into or overwritten.
- Invalid names and unavailable write capability fail before filesystem mutation.
- V1 and a later V2 retain distinct contracts and scaffolds; opening V1 remains V1-compatible.
- The project tree remains client-local and no project file content is sent to Meshingress server storage by this feature.

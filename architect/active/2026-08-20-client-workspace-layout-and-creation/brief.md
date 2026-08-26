# Client Workspace Layout Packages and Creation Flow

## Goal

Create client-owned Meshingress workspaces from a paired, versioned layout package. A layout package contains both the scaffold tree to generate and the structural contract Studio uses to interpret it.

## Required entry point

In the Studio `.brand-menu`, `File -> New workspace` prompts for:

1. a workspace name; and
2. a parent folder selected by the client.

On confirmation, Studio creates the project as `<selected parent>/<workspace name>/`, applies the chosen layout package, and opens that newly created project root. It must not instead load the parent folder the client selected.

## Ownership boundary

The client owns the project files and keeps them available offline. Meshingress stores only browser-local access state where necessary; it must not make the project tree server-owned or persist client files to the server by default.

## Non-goals

- Do not implement the feature in this planning entry.
- Do not introduce server-backed project storage, project-file upload, or server-side filesystem access.
- Do not silently migrate an existing workspace to a newer layout version.
- Do not modify unrelated dirty Studio, MCP, or tool-module work.

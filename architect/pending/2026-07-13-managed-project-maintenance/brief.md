# Managed Project Maintenance and Wrapped Vendor Locking

## Goal

Replace the stale Mockito-only maintenance flow with a controlled project-maintenance capability that behaves like an IDE reviewer: it inspects code and environment health, validates declared dependencies and managed assets, and performs repairs only within explicit ownership boundaries.

## Desired Outcome

The project has a repeatable, reviewable way to determine whether it can build and run with required managed assets, without silently upgrading dependencies, replacing user-managed tools, or deleting stored data.

## Non-Goals

- Do not implement the maintenance command in this entry.
- Do not automatically upgrade Maven dependencies, wrapped vendors, tool assets, or application code.
- Do not modify or remove `repository/`, user-provided `tools/` content, runtime data, secrets, or unrelated dirty work.
- Do not revive the obsolete Mockito download workflow.

# Implementation Notes

The record was reopened on 2026-07-18 because the prior terminal state was premature. The approved architecture became the implementation blueprint and is now delivered with six focused passing tests. The final resolution records real source implementation, not planning completion.

## Workspace Contract Revision

The implemented opaque-token, single-object lease is now superseded. Storage is a short-lived tool-output workspace rooted at `tools/{sessionId}/{requestId}/files/`, with a generated manifest at `files/manifest.json`. Tool modules receive the workspace through the tool API and publish named relative files. Public retrieval is explicit: `/storage/{sessionId}/{requestId}/files/{relativePath}` for `GET` and `HEAD`.

# Product Requirements

## Outcome

A Toonverse download is reliably stored in one Nextcloud workspace and can be rendered by an explicit Open Ink reader.

## Functional Requirements

- Stream source content from Meshingress to authenticated Nextcloud WebDAV without Base64 transport or a Nextcloud-side fetch worker.
- Support direct `PUT` and Nextcloud WebDAV chunked-upload v2.
- Persist a durable publication status and bounded retry state in Meshingress.
- Publish no playable manifest until all expected content is present and verified.
- Preserve existing deployed workspaces and the current app until the replacement passes an end-to-end render test.

## Non-Goals

- Deleting the current Nextcloud app, database tables, or existing workspaces in this entry.
- Treating the generic Nextcloud document previewer as an Open Ink reader.
- Modifying unrelated tool, auth, audit, or MCP-progress work.

## Acceptance Test

A one-chapter fixture is downloaded once, its descriptor resolves through the reader, its chapter JSON resolves, at least one image byte stream is rendered, and the completed workspace is visible in Nextcloud.


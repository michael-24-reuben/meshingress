# Delegated Workspace Contract — Implemented Form

This document records the implemented form of the pre-delegation proposal. The canonical publish-facing reference is `integrations/nextcloud/meshingress/README.md`.

```text
RESERVE OPEN WORKSPACE
      ↓
UPLOAD NATIVE FILES
      ↓
APPEND HTTPS SOURCES
      ↓
SEAL ONCE
      ↓
NEXTCLOUD IMPORT WORKER
      ↓
MIXED COMPLETED MANIFEST
```

## Identity and reservation

`POST /ocs/v2.php/apps/meshingress/api/v1/delegated-workspaces` accepts `sessionId` and `requestId`. Authentication plus `X-Meshingress-Tool-Id` determine ownership; callers do not send `toolId`, a title, or a selectable root path in JSON.

The request ID is the workspace ID and opaque managed root suffix. The app returns an OPEN `meshingress.delegated-workspace/v1` response with an absolute WebDAV `workspaceUri`.

## Native files

`PUT /delegated-workspaces/{workspaceId}/files` accepts a JSON payload:

```json
{
  "path": "book.json",
  "contentType": "application/json",
  "contentBase64": "..."
}
```

This is intentionally not a path-in-URL/raw-body endpoint: Nextcloud 34's OCS request abstraction does not expose the raw body to this controller. The app validates path ownership, collisions, and reserved manifest names.

## Delegated files and publication

`POST /delegated-workspaces/{workspaceId}/sources` accepts explicit `{url, path}` entries, where `url` is credential-free HTTPS. `POST /seal` creates exactly one existing durable source-import job. `GET /by-request/{requestId}` returns state or the final storage manifest.

Native output and imported files appear together in the manifest. The live verification produced three JPEGs with non-zero sizes plus native `book.json`.

## Historical differences

The earlier proposal's caller-selected `dws_*` workspace IDs, root-level outlet/name values, response URI fields, raw native PUT shape, and relative-source/base-url handling were not adopted. They are not supported by the deployed reservation API.

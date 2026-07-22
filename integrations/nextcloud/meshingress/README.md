# Meshingress Nextcloud App

The `meshingress` app owns `Workspace/Meshingress/` and is the external provider for:

```properties
meshingress.storage.lifecycle=delegated-external
```

New integrations use the reserved-workspace API. Nextcloud owns delegated media bytes; Meshingress sends only native tool output and HTTPS source references.

## Authentication

Every request requires an authenticated Nextcloud user and:

```text
OCS-APIRequest: true
```

Every reservation mutation also requires a stable lowercase `X-Meshingress-Tool-Id` header. The app derives workspace ownership and tool identity from authentication plus that header; request JSON cannot override them.

## Current reserved-workspace API

```text
POST /ocs/v2.php/apps/meshingress/api/v1/delegated-workspaces?format=json
PUT  /ocs/v2.php/apps/meshingress/api/v1/delegated-workspaces/{workspaceId}/files?format=json
POST /ocs/v2.php/apps/meshingress/api/v1/delegated-workspaces/{workspaceId}/sources?format=json
POST /ocs/v2.php/apps/meshingress/api/v1/delegated-workspaces/{workspaceId}/seal?format=json
GET  /ocs/v2.php/apps/meshingress/api/v1/delegated-workspaces/by-request/{requestId}?format=json
```

### 1. Reserve

```json
{
  "sessionId": "tool-session-123",
  "requestId": "req_book_123"
}
```

The request ID is the opaque workspace ID and determines the managed root:

```text
Workspace/Meshingress/storage/<tool-id>/req_book_123/
```

The response is an OPEN workspace with `workspaceId`, `workspaceUri`, `sessionId`, `requestId`, `toolId`, `sourceCount`, and `createdAt`.

### 2. Upload native output

Use JSON rather than a raw request body because Nextcloud 34 does not expose raw OCS controller content publicly:

```json
{
  "path": "book.json",
  "contentType": "application/json",
  "contentBase64": "eyJ0eXBlIjoib3Blbi1pbmsuYm9vay92MSJ9Cg=="
}
```

`path` must be a relative file path. `manifest.json` and `manifest-nci_*.json` are reserved. Native files and delegated source paths cannot collide.

### 3. Append delegated sources

```json
{
  "sources": [
    {
      "url": "https://source.example.com/chapters/001/page-001.jpg",
      "path": "chapters/0001/001.jpg"
    }
  ]
}
```

URLs must be credential-free HTTPS URLs. Paths are relative, explicit, and validated atomically as a batch. Appending sources does not start the worker.

### 4. Seal and poll

`POST .../{workspaceId}/seal` makes the reservation immutable and creates exactly one durable source-import job. Poll the `by-request` endpoint. It returns queued/running/failed workspace state and, on success, the canonical `meshingress.tool-storage-manifest/v1` manifest.

The final manifest contains native files and Nextcloud-downloaded sources together. It has no `expiresAt`, because Nextcloud owns the content.

## Legacy compatibility API

The endpoints below remain available for existing clients only. They must not be used by new `delegated-external` integrations.

```text
POST /ocs/v2.php/apps/meshingress/api/v1/workspace/initialize?format=json
POST /ocs/v2.php/apps/meshingress/api/v1/source-imports?format=json
GET  /ocs/v2.php/apps/meshingress/api/v1/source-imports/{jobId}?format=json
```

The legacy multi-source form queues a job immediately and may be paired with direct WebDAV native-file uploads. It will remain until a separately announced compatibility/deprecation decision.

## Generated smoke test

`generated/meshingress-nextcloud-tool-calling/nextcloud-upload-test.js` exercises reserve -> native upload -> source append -> seal -> poll. It requires an environment value rather than an inline password:

```powershell
$env:NEXTCLOUD_PRIMARY_AUTH = 'Basic <base64(userId:appPassword)>'
node generated/meshingress-nextcloud-tool-calling/nextcloud-upload-test.js
```

## Server-side installation

Use `install-nextcloud-app.js` from a machine with Node.js, OpenSSH `ssh`/`scp`, and SSH-agent access to the host.

```powershell
$env:MESHINGRESS_NEXTCLOUD_DEPLOY_HOST = 'nextcloud-host.example'
$env:MESHINGRESS_NEXTCLOUD_DEPLOY_USER = 'linux-user'
$env:MESHINGRESS_NEXTCLOUD_DEPLOY_CONTAINER = 'nextcloud-app.v1'
node integrations/nextcloud/meshingress/install-nextcloud-app.js --replace-existing
```

The installer stages/lints the app, applies migrations, refreshes the managed source-import worker, and preserves unrelated cron entries. Restart the Nextcloud app container after replacement when the active PHP route cache has not reloaded the new routes.

# Meshingress Nextcloud delegated workspaces

This is the current Nextcloud app source. `../meshingress-v1/` is preserved only as the prior implementation reference and is not part of this app's runtime.

Meshingress sends native metadata and credential-free HTTPS source references. Nextcloud owns the download, retry, storage, and final manifest.

```text
RESERVE -> OPEN -> [native files | source batches]* -> SEALED -> QUEUED -> RUNNING -> COMPLETED | FAILED
```

`SEALED` is durable before worker wake-up. The dedicated worker also scans sealed records, so a failed enqueue cannot strand a workspace.

## OCS routes

All routes require an authenticated Nextcloud user and `OCS-APIRequest: true`. Mutations also require a stable lowercase `X-Meshingress-Tool-Id` header.

```text
POST /ocs/v2.php/apps/meshingress/api/v1/delegated-workspaces?format=json
PUT  /ocs/v2.php/apps/meshingress/api/v1/delegated-workspaces/{workspaceId}/files?format=json
POST /ocs/v2.php/apps/meshingress/api/v1/delegated-workspaces/{workspaceId}/sources?format=json
POST /ocs/v2.php/apps/meshingress/api/v1/delegated-workspaces/{workspaceId}/seal?format=json
GET  /ocs/v2.php/apps/meshingress/api/v1/delegated-workspaces/{workspaceId}?format=json
GET  /ocs/v2.php/apps/meshingress/api/v1/delegated-workspaces/by-request/{requestId}?format=json
```

`requestId` is required when reserving and is the opaque managed workspace ID. Retrying the same reservation is idempotent for the same authenticated user and tool.

```json
{
  "requestId": "req_book_123",
  "sessionId": "optional-mcp-session"
}
```

While the workspace is `OPEN`, native files and source batches can be posted in either order. Native files use JSON/Base64 because Nextcloud 34 does not expose a raw OCS request body to this controller.

```json
{
  "path": "book.json",
  "contentType": "application/json",
  "contentBase64": "eyJ0eXBlIjoib3Blbi1pbmsuYm9rIn0="
}
```

Delegated sources have explicit destinations. `contentType`, `expectedByteSize`, and `expectedSha256` are optional per-source configuration; the worker verifies supplied size and digest before completing the source.

```json
{
  "sources": [
    {
      "url": "https://source.example/chapters/001/page-001.jpg",
      "path": "chapters/0001/001.jpg",
      "contentType": "image/jpeg",
      "expectedByteSize": 184920,
      "expectedSha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    }
  ]
}
```

The append response assigns a durable `sourceId` for each accepted path. It is an audit/retry correlation value; callers do not choose it.

The app accepts only credential-free HTTPS URLs and relative, traversal-free file paths. `manifest.json` is reserved. A source succeeds independently and remains complete across later retries; a source has three attempts. If any source exhausts its attempts, the workspace becomes `FAILED` with a safe error. On success the app writes `manifest.json` and returns the same reader-ready manifest from status polling.

## Worker

The background job is only a wake-up. For predictable processing, run the dedicated durable scanner:

```powershell
php occ meshingress:workspace:work --watch --interval=1 --no-interaction --quiet
```

It claims one `SEALED` or `QUEUED` record at a time. A stopped process leaves a 15-minute lease; a later scanner safely reclaims it. This protects against both generic Nextcloud queue starvation and duplicate wake-ups.

## Installation

The included helper is deliberately deploy-only when invoked. It uses SSH-agent authentication and refuses to overwrite an installed app unless `--replace-existing` is explicit. It does not restart Nextcloud or embed credentials.

```powershell
$env:MESHINGRESS_NEXTCLOUD_DEPLOY_HOST = 'nextcloud-host.example'
$env:MESHINGRESS_NEXTCLOUD_DEPLOY_USER = 'linux-user'
node integrations/nextcloud/meshingress/install-nextcloud-app.js --replace-existing
```

After deployment, start the dedicated worker through the host's service manager or an explicitly reviewed supervisor configuration. Do not rely on the caller process to stream or retain source bytes.

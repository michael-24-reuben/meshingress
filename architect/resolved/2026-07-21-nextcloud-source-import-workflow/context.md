# Research Context and Decision

## Finding

Nextcloud Flow is a user-configured, event-triggered engine: apps expose entities, events, checks, and operations, then a user creates rules. It is suitable for optional actions after an imported file is created, but it is not a general HTTP job API and is not the right owner of an inbound `url + path` request.

The correct implementation is a dedicated Nextcloud app named `meshingress`. Source import is its first operation, not its identity:

1. An authenticated OCS route receives an import request and returns `202 Accepted` with a durable job identity.
2. The controller records the request and schedules a `QueuedJob` through `IJobList`.
3. The job uses `OCP\\Http\\Client\\IClientService` to fetch the URL. This client follows Nextcloud proxy/security configuration and includes remote-host validation.
4. The job uses `IRootFolder` and the Node API to find or create folders and write the completed file under the selected `Workspace/Meshingress/` root.
5. A status route returns the terminal state and downloaded-file metadata. Meshingress records the returned job ID and considers the source-import operation complete only after a terminal successful status.

The sources establish that Nextcloud supports OCS routes through `routes.php` / `OCSController`, `QueuedJob` registration through `IJobList`, the HTTP-client factory, and the filesystem Node API. No documented built-in endpoint was found that safely provides arbitrary remote URL import with a caller-selected destination, so this must be a custom app rather than configuration of Flow or WebDAV.

## Workspace Contract

The owner selects one existing Nextcloud folder, currently `Workspace/`, as the parent workspace. The app creates and manages this namespace beneath it:

```text
Workspace/
└── Meshingress/
    ├── storage/
    │   └── <tool-id>/
    ├── docs/
    ├── generated/
    ├── repository/
    │   ├── artifacts/
    │   ├── quarantine/
    │   └── vendor/
    └── var/
        └── logs/
```

These are user-visible Nextcloud folders, not server filesystem paths. The app may create missing managed directories but must neither write outside `Workspace/Meshingress/` nor take ownership of sibling `Workspace/` content.

`storage/` is the initial destination for externally sourced tool output. The authenticated Meshingress caller supplies a stable tool identity with each operation; the app uses it as the next segment, so a requested relative path cannot select another tool's storage namespace. This is the official Nextcloud workspace, therefore the resolved destination is always `storage/<tool-id>/<path>` and never contains Meshingress session IDs or request IDs.

## Recommended Minimal Contract

### Submit one import

`POST /ocs/v2.php/apps/meshingress/api/v1/source-imports`

Required request metadata: authenticated Meshingress caller plus `X-Meshingress-Tool-Id`. The body remains minimal:

```json
{
  "url": "https://source.example/media/file.mp4",
  "path": "Instagram/example/file.mp4"
}
```

`path` is relative to `Workspace/Meshingress/storage/<tool-id>/`. It is not a server filesystem path and may not escape that root. With tool ID `instagram.fetch`, this example writes to `Workspace/Meshingress/storage/instagram.fetch/Instagram/example/file.mp4`.

Success response (`202 Accepted`):

```json
{
  "jobId": "nci_...",
  "state": "QUEUED"
}
```

### Read import status

`GET /ocs/v2.php/apps/meshingress/api/v1/source-imports/{jobId}`

Successful terminal response:

```json
{
  "jobId": "nci_...",
  "state": "COMPLETED",
  "sourceUrl": "https://source.example/media/file.mp4",
  "path": "storage/instagram.fetch/Instagram/example/file.mp4",
  "fileId": 12345,
  "size": 1048576,
  "mimeType": "video/mp4",
  "etag": "...",
  "sha256": "...",
  "completedAt": "2026-07-21T13:00:00Z"
}
```

The response is intentionally asynchronous. The caller can submit many independent `{url, path}` pairs, but a batch endpoint is a later convenience; it should be expressed as an array of the same minimal items and still return individual job IDs and outcomes.

## Meshingress Fit

Source import is a Meshingress workspace operation, not another storage lifecycle. The planned lifecycle model removes `EXTERNAL_EXTERNAL`; it should not be replaced with a provider-upload-session or source-pull variant. Existing `LOCAL_ASYNC_EXTERNAL` remains the byte-owning path for tools that already produce local files and need a write-only WebDAV handoff. Link-producing tools use the Nextcloud `meshingress` app instead, supplying their tool identity and a `{url, path}` source-import request.

The Nextcloud app is intentionally broader than source import. Future workspace operations may target `docs/`, `generated/`, `repository/artifacts/`, `repository/quarantine/`, `repository/vendor/`, and `var/logs/`, but each needs its own API and authorization contract. No later operation inherits broad write access merely because it is inside the same workspace.

## Sources Consulted

- https://docs.nextcloud.com/server/stable/developer_manual/digging_deeper/flow.html
- https://docs.nextcloud.com/server/stable/developer_manual/basics/routing.html
- https://docs.nextcloud.com/server/stable/developer_manual/server/externalapi.html
- https://docs.nextcloud.com/server/25/developer_manual/basics/backgroundjobs.html
- https://docs.nextcloud.com/server/stable/developer_manual/digging_deeper/http_client.html
- https://docs.nextcloud.com/server/stable/developer_manual/digging_deeper/security.html
- https://docs.nextcloud.com/server/stable/developer_manual/basics/storage/filesystem.html

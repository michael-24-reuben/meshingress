# Draft Contract

This file defines a proposed contract for review. It is not an implemented API.

## MCP Structured Content

Recommended node shape:

```json
{
  "type": "storage.lease/v1",
  "storageId": "stg_opaque-management-id",
  "url": "https://meshingress.example/storage/opaque-access-token",
  "mimeType": "application/pdf",
  "byteSize": 482193,
  "createdAt": "2026-07-17T22:41:58-04:00",
  "expiresAt": "2026-07-17T23:11:58-04:00",
  "remainingRequests": 3,
  "checksum": {
    "algorithm": "sha-256",
    "value": "base64url-or-lowercase-hex"
  },
  "suggestedFilename": "article.pdf"
}
```

## Required Fields

| Field | Type | Meaning |
|---|---|---|
| `type` | string | Constant `storage.lease/v1`. |
| `url` | string | Absolute or server-relative opaque retrieval URL, according to public-base-URL policy. |
| `mimeType` | string | Valid media type selected by trusted storage policy. |
| `byteSize` | integer | Exact stored byte count. |
| `expiresAt` | RFC 3339 timestamp | Hard expiry time. |
| `remainingRequests` | integer | Retrievals available at the instant the node is emitted. |

## Optional Fields

| Field | Type | Meaning |
|---|---|---|
| `storageId` | string | Opaque management identifier usable only with authenticated deletion. |
| `createdAt` | RFC 3339 timestamp | Publication time. |
| `checksum` | object | Integrity digest computed during ingestion. |
| `suggestedFilename` | string | Sanitized display filename, never a path. |

## Contract Rules

- `url` contains the access capability; `storageId` does not.
- The database stores a cryptographic hash of the access token, not the raw token.
- `remainingRequests` is informational and can become stale immediately under concurrent access.
- `expiresAt` is authoritative even when `remainingRequests` is positive.
- The node contains no source provenance that could disclose credentials or private addresses.
- The lease node may be nested in a tool-specific result, but its internal schema remains unchanged.

## Retrieval Response

Recommended successful `GET` headers:

```http
Content-Type: application/pdf
Content-Length: 482193
Content-Disposition: attachment; filename="article.pdf"
Cache-Control: no-store
X-Content-Type-Options: nosniff
```

Recommended successful `HEAD` returns the same representation headers without a body.

## Status Semantics

Proposed defaults:

| Condition | Response |
|---|---|
| Available token | `200 OK` |
| Unknown, expired, exhausted, deleted, or revoked token | `404 Not Found` |
| Method unsupported | `405 Method Not Allowed` |
| Range request while ranges are disabled | `416 Range Not Satisfiable` |
| Authenticated deletion succeeds or target is already absent | `204 No Content` |
| Ingestion accepted and published synchronously | `201 Created` |
| Quota or limit rejection | `413 Payload Too Large` or `507 Insufficient Storage`, selected by failure cause |

Uniform `404` retrieval behavior is recommended to reduce token-state disclosure.

## Range Requests

Proposed version-one posture:

- do not advertise `Accept-Ranges`
- reject `Range` requests
- add range semantics only after request-count accounting and concurrent segment retrieval are explicitly designed

This avoids media clients consuming a lease through multiple implicit partial requests.

# Meshingress Repository API Draft

Status: draft documentation of the currently implemented REST surface.

The repository accepts, assesses, reviews, publishes, revokes, soft-deletes, and restores artifacts. Publishing creates a signed repository publication record; it does not install the artifact into `meshingress-server`. Runtime installation remains a separate server operation.

## Base URL and conventions

Examples use:

```text
http://localhost:8080
```

Artifact coordinates are encoded in the path:

```text
/artifact/{groupId}/{artifactId}/{version}
```

All currently implemented successful operations return HTTP `200 OK`.

### Request headers

| Header | Required in practice | Purpose |
|---|---:|---|
| `X-Repository-Role` | Yes | Authorizes the operation. Multiple roles may be separated by commas or whitespace. |
| `X-Repository-Actor` | No | Audit actor. Defaults to `unknown`. |
| `X-Request-Id` | No | Correlation value persisted with lifecycle audit events. |
| `Authorization` | No | Captured in request context, but the current access policy authorizes from `X-Repository-Role`. |
| `Content-Type` | For bodies | `multipart/form-data` for upload and `application/json` for review/lifecycle bodies. |

### Role permissions

| Operation | Allowed role |
|---|---|
| Read metadata, assessment, queue, or publication | Any non-empty role |
| Upload | `uploader` |
| Assess, approve, or reject | `reviewer` |
| Publish or revoke | `publisher` |
| Delete or restore | `admin` |
| Any operation | `admin` |

## Common response objects

### Artifact record

Upload, metadata, assess, approve, reject, delete, and restore return an `ArtifactRecord`:

```json
{
  "coordinate": {
    "groupId": "dev.mrk.tools",
    "artifactId": "powershell-cli",
    "version": "1.0.0",
    "classifier": null,
    "packaging": "jar"
  },
  "type": "TOOL_MODULE",
  "trustStatus": "REVIEW_PENDING",
  "artifactUri": "meshingress-repository://artifact/dev.mrk.tools/powershell-cli/1.0.0/powershell-cli-1.0.0.jar",
  "artifactChecksum": {
    "algorithm": "SHA-256",
    "value": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
  },
  "files": [
    {
      "path": "powershell-cli-1.0.0.jar",
      "size": 48231,
      "checksum": {
        "algorithm": "SHA-256",
        "value": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
      },
      "executable": false
    }
  ],
  "scopes": {
    "requestedScopes": ["SHELL_EXECUTE", "FILES_READ"],
    "inferredScopes": ["FILES_READ"],
    "approvedScopes": [],
    "deniedScopes": []
  },
  "assessment": {
    "status": "clean",
    "scanners": ["cyclonedx-sbom", "bytecode-scope-scanner"],
    "findingCount": 0,
    "summary": {
      "sbom": {
        "format": "CycloneDX",
        "componentCount": 3
      }
    }
  },
  "createdAt": "2026-06-18T18:00:00-04:00",
  "updatedAt": "2026-06-18T18:01:00-04:00"
}
```

Values such as paths, checksums, timestamps, scanner summaries, and findings are illustrative and depend on the uploaded artifact and repository configuration.

Possible `type` values:

```text
TOOL_MODULE
CLI_HARNESS
AVAILABILITY_ANNOTATION
AVAILABILITY_POLICY
SCOPE_POLICY
```

Possible `trustStatus` values:

```text
RECEIVED
QUARANTINED
SCANNING
REVIEW_PENDING
APPROVED_TRUSTED
APPROVED_LIMITED
REJECTED
BLOCKED_MALWARE
BLOCKED_POLICY
BLOCKED_VULNERABILITY
SUPERSEDED
DELETED
REVOKED
```

### Review request

Approve, reject, revoke, delete, and restore accept an optional review body:

```json
{
  "approvedScopes": ["FILES_READ"],
  "deniedScopes": [
    {
      "scope": "SHELL_EXECUTE",
      "reason": "Shell execution is not approved for this release."
    }
  ],
  "trustStatus": "APPROVED_LIMITED",
  "reviewer": "repository-reviewer",
  "notes": "Approved with a reduced runtime scope."
}
```

Fields may be omitted. Null scope lists become empty lists, and null reviewer or notes values become empty strings. Approval specifically requires `trustStatus` to be `APPROVED_TRUSTED` or `APPROVED_LIMITED`.

### Publication record

Publish, revoke, and publication lookup return an `ArtifactPublicationRecord`:

```json
{
  "coordinate": {
    "groupId": "dev.mrk.tools",
    "artifactId": "powershell-cli",
    "version": "1.0.0",
    "classifier": null,
    "packaging": "jar"
  },
  "type": "TOOL_MODULE",
  "trustStatus": "APPROVED_LIMITED",
  "artifactUri": "meshingress-repository://artifact/dev.mrk.tools/powershell-cli/1.0.0/powershell-cli-1.0.0.jar",
  "artifactChecksum": {
    "algorithm": "SHA-256",
    "value": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
  },
  "scopePolicy": {
    "requestedScopes": ["SHELL_EXECUTE", "FILES_READ"],
    "inferredScopes": ["FILES_READ"],
    "approvedScopes": ["FILES_READ"],
    "deniedScopes": [
      {
        "scope": "SHELL_EXECUTE",
        "reason": "Shell execution is not approved for this release."
      }
    ]
  },
  "scanSummary": {
    "status": "clean",
    "scanners": ["cyclonedx-sbom", "bytecode-scope-scanner"],
    "findingCount": 0,
    "summary": {}
  },
  "provenance": {
    "sourceRepo": "",
    "sourceCommit": "",
    "generatedBy": "meshingress-repository",
    "generatorCommit": ""
  },
  "revoked": false,
  "publishedAt": "2026-06-18T22:05:00Z",
  "signatureKeyId": "local-dev-hmac",
  "signatureAlgorithm": "HmacSHA256",
  "signature": "base64-or-encoded-signature-value"
}
```

## Operations

### 1. Upload an artifact

```http
POST /artifact/dev.mrk.tools/powershell-cli/1.0.0?type=TOOL_MODULE&packaging=jar&requestedScopes=SHELL_EXECUTE&requestedScopes=FILES_READ
Content-Type: multipart/form-data
X-Repository-Role: uploader
X-Repository-Actor: build-agent
X-Request-Id: req-upload-1
```

PowerShell example:

```powershell
$headers = @{
    'X-Repository-Role'  = 'uploader'
    'X-Repository-Actor' = 'build-agent'
    'X-Request-Id'       = 'req-upload-1'
}

$form = @{
    file = Get-Item '.\powershell-cli-1.0.0.jar'
}

Invoke-RestMethod `
    -Method Post `
    -Uri 'http://localhost:8080/artifact/dev.mrk.tools/powershell-cli/1.0.0?type=TOOL_MODULE&packaging=jar&requestedScopes=SHELL_EXECUTE&requestedScopes=FILES_READ' `
    -Headers $headers `
    -Form $form
```

Response: `200 OK` with an `ArtifactRecord`. The current flow returns `trustStatus: "QUARANTINED"` after upload.

Defaults:

- `type=TOOL_MODULE`
- `packaging=jar`
- no requested scopes
- multipart limit: 256 MB

### 2. Read artifact metadata

```http
GET /artifact/dev.mrk.tools/powershell-cli/1.0.0/metadata
X-Repository-Role: reviewer
```

Response: `200 OK` with the current `ArtifactRecord`.

### 3. Assess an artifact

```http
POST /artifact/dev.mrk.tools/powershell-cli/1.0.0/assess
X-Repository-Role: reviewer
X-Repository-Actor: assessment-agent
X-Request-Id: req-assess-1
```

There is no request body.

Response: `200 OK` with an updated `ArtifactRecord`. The normal successful flow advances the artifact to `REVIEW_PENDING`, records scanner summaries, and adds inferred scopes.

### 4. Read detailed assessment results

```http
GET /artifact/dev.mrk.tools/powershell-cli/1.0.0/assessment
X-Repository-Role: reviewer
```

Response: `200 OK` with an array of scanner results:

```json
[
  {
    "scanner": "cyclonedx-sbom",
    "scannerVersion": "1.6",
    "status": "PASSED",
    "findings": [],
    "rawSummary": {
      "rawReport": "cyclonedx-sbom.json"
    },
    "rawReportPath": "repository/sbom/dev/mrk/tools/powershell-cli/1.0.0/cyclonedx-sbom.json"
  },
  {
    "scanner": "bytecode-scope-scanner",
    "scannerVersion": "",
    "status": "REVIEW",
    "findings": [
      {
        "severity": "INFO",
        "code": "SCOPE_INFERRED",
        "message": "Artifact behavior implies FILES_READ.",
        "path": "dev.mrk.tools.PowerShellCliTool"
      }
    ],
    "rawSummary": {},
    "rawReportPath": null
  }
]
```

Scanner statuses are `PASSED`, `REVIEW`, `BLOCKED`, or `FAILED`.

### 5. List pending reviews

```http
GET /artifact/reviews/pending
X-Repository-Role: reviewer
```

Response: `200 OK` with review queue items:

```json
[
  {
    "artifact": {
      "coordinate": {
        "groupId": "dev.mrk.tools",
        "artifactId": "powershell-cli",
        "version": "1.0.0",
        "classifier": null,
        "packaging": "jar"
      },
      "type": "TOOL_MODULE",
      "trustStatus": "REVIEW_PENDING",
      "artifactUri": "meshingress-repository://artifact/dev.mrk.tools/powershell-cli/1.0.0/powershell-cli-1.0.0.jar",
      "artifactChecksum": {
        "algorithm": "SHA-256",
        "value": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
      },
      "files": [],
      "scopes": {
        "requestedScopes": ["SHELL_EXECUTE", "FILES_READ"],
        "inferredScopes": ["FILES_READ"],
        "approvedScopes": [],
        "deniedScopes": []
      },
      "assessment": null,
      "createdAt": "2026-06-18T18:00:00-04:00",
      "updatedAt": "2026-06-18T18:01:00-04:00"
    },
    "assessment": []
  }
]
```

The nested artifact and assessment objects use the complete shapes documented above; fields are abbreviated here only by example values.

### 6. Approve an artifact

```http
POST /artifact/dev.mrk.tools/powershell-cli/1.0.0/approve
Content-Type: application/json
X-Repository-Role: reviewer
X-Repository-Actor: repository-reviewer
X-Request-Id: req-approve-1

{
  "approvedScopes": ["FILES_READ"],
  "deniedScopes": [
    {
      "scope": "SHELL_EXECUTE",
      "reason": "Not approved for this release."
    }
  ],
  "trustStatus": "APPROVED_LIMITED",
  "reviewer": "repository-reviewer",
  "notes": "Approved with limited scope."
}
```

Response: `200 OK` with an `ArtifactRecord` whose trust status is `APPROVED_TRUSTED` or `APPROVED_LIMITED` and whose approved/denied scopes reflect the decision.

Precondition: the artifact must first be assessed and be `REVIEW_PENDING`.

### 7. Reject an artifact

```http
POST /artifact/dev.mrk.tools/powershell-cli/1.0.0/reject
Content-Type: application/json
X-Repository-Role: reviewer
X-Repository-Actor: repository-reviewer
X-Request-Id: req-reject-1

{
  "deniedScopes": [
    {
      "scope": "SHELL_EXECUTE",
      "reason": "Shell access is not approved for this artifact."
    }
  ],
  "reviewer": "repository-reviewer",
  "notes": "Rejected during manual review."
}
```

Response: `200 OK` with an `ArtifactRecord` whose `trustStatus` is `REJECTED`.

Precondition: the artifact must first be assessed and be `REVIEW_PENDING`.

### 8. Publish an approved artifact

```http
POST /artifact/dev.mrk.tools/powershell-cli/1.0.0/publish
X-Repository-Role: publisher
X-Repository-Actor: publisher-agent
X-Request-Id: req-publish-1
```

There is no request body.

Response: `200 OK` with a signed `ArtifactPublicationRecord` and `revoked: false`.

Preconditions:

- the artifact has been assessed;
- its trust status is `APPROVED_TRUSTED` or `APPROVED_LIMITED`;
- repository lifecycle checks permit publication.

### 9. Read a publication record

```http
GET /artifact/dev.mrk.tools/powershell-cli/1.0.0/publication
X-Repository-Role: publisher
```

Response: `200 OK` with the current `ArtifactPublicationRecord`.

### 10. Revoke a publication

```http
POST /artifact/dev.mrk.tools/powershell-cli/1.0.0/revoke
Content-Type: application/json
X-Repository-Role: publisher
X-Repository-Actor: publisher-agent
X-Request-Id: req-revoke-1

{
  "reviewer": "publisher-agent",
  "notes": "Publication revoked after a policy finding."
}
```

Response: `200 OK` with a newly signed `ArtifactPublicationRecord` containing:

```json
{
  "trustStatus": "REVOKED",
  "revoked": true
}
```

The actual response is the complete publication object, not only these two fields.

Preconditions: a publication must exist, must not already be revoked, and the artifact must have an approved trust status before the revocation transition.

### 11. Soft-delete an artifact

```http
POST /artifact/dev.mrk.tools/powershell-cli/1.0.0/delete
Content-Type: application/json
X-Repository-Role: admin
X-Repository-Actor: repository-admin
X-Request-Id: req-delete-1

{
  "reviewer": "repository-admin",
  "notes": "Soft delete requested by repository administration."
}
```

Response: `200 OK` with an `ArtifactRecord` whose `trustStatus` is `DELETED`.

This is a metadata lifecycle transition. The current tested behavior retains the physical artifact file. A published artifact must be revoked before deletion.

### 12. Restore a soft-deleted artifact

```http
POST /artifact/dev.mrk.tools/powershell-cli/1.0.0/restore
Content-Type: application/json
X-Repository-Role: admin
X-Repository-Actor: repository-admin
X-Request-Id: req-restore-1

{
  "reviewer": "repository-admin",
  "notes": "Restore the artifact to its pre-delete trust status."
}
```

Response: `200 OK` with an `ArtifactRecord`. The trust status is restored from the persisted delete lifecycle event rather than reset to a fixed status.

Precondition: the current artifact status must be `DELETED`.

## Error responses

Repository and lifecycle failures return `application/problem+json` with HTTP `400 Bad Request`:

```json
{
  "type": "about:blank",
  "title": "Repository request failed",
  "status": 400,
  "detail": "artifact must be assessed before approval",
  "instance": "/artifact/dev.mrk.tools/powershell-cli/1.0.0/approve"
}
```

Authorization failures return HTTP `403 Forbidden`:

```json
{
  "type": "about:blank",
  "title": "Repository access denied",
  "status": 403,
  "detail": "repository role is not allowed to publish",
  "instance": "/artifact/dev.mrk.tools/powershell-cli/1.0.0/publish"
}
```

Representative lifecycle failure details include:

```text
artifact must be assessed before approval
artifact must be assessed before rejection
artifact must be assessed before publication
only approved artifacts can be published
artifact must be published before revocation
publication is already revoked
published artifacts must be revoked before deletion
artifact is already deleted
artifact must be DELETED before restore
publication record not found
```

## Typical lifecycle

```text
upload -> QUARANTINED
assess -> REVIEW_PENDING
approve -> APPROVED_TRUSTED or APPROVED_LIMITED
publish -> signed publication record
revoke -> REVOKED publication and artifact
delete -> DELETED metadata state
restore -> pre-delete trust state
```

Alternative review path:

```text
upload -> assess -> reject -> REJECTED
```

Runtime activation is intentionally outside this API. A consumer retrieves and verifies the signed publication record, then invokes the separate `meshingress-server` publication-install flow.

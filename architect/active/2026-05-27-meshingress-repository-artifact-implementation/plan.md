# Implementation Plan

## Phase 0: Module Skeleton

Add modules:

```txt
app/meshingress-repository
lib/meshingress-artifact-model
lib/meshingress-artifact-storage
lib/meshingress-artifact-security
lib/meshingress-artifact-publication
```

Update root `pom.xml`:

```xml
<module>lib/meshingress-artifact-model</module>
<module>lib/meshingress-artifact-storage</module>
<module>lib/meshingress-artifact-security</module>
<module>lib/meshingress-artifact-publication</module>
<module>app/meshingress-repository</module>
```

## Phase 1: Artifact Model

Implement in `lib/meshingress-artifact-model`:

```txt
ArtifactCoordinate
ArtifactClassifier
ArtifactPackaging
MeshingressArtifactType
ArtifactTrustStatus
ArtifactRecord
ArtifactManifest
ArtifactChecksum
ArtifactFileEntry
ArtifactExecutableEntry
ArtifactScopeDeclaration
ArtifactAssessmentSummary
ArtifactReviewDecision
ArtifactPublicationRecord
ArtifactProvenance
ArtifactSbomRef
ArtifactSignatureRef
```

Suggested Java records:

```java
public record ArtifactCoordinate(
        String groupId,
        String artifactId,
        String version,
        String classifier,
        String packaging
) {}

public record ArtifactChecksum(
        String algorithm,
        String value
) {}

public record ArtifactScopeDeclaration(
        List<String> requestedScopes,
        List<String> inferredScopes,
        List<String> approvedScopes,
        List<DeniedScope> deniedScopes
) {}
```

## Phase 2: Storage Abstraction

Implement in `lib/meshingress-artifact-storage`:

```txt
ArtifactStorage
ArtifactBlobStore
ArtifactMetadataStore
ArtifactIndexStore
ArtifactQuarantineStore
FileSystemArtifactStorage
```

Initial development backend:

```txt
repository/
├─ artifacts/
├─ metadata/
├─ indexes/
├─ quarantine/
├─ assessments/
├─ reviews/
├─ sbom/
├─ attestations/
└─ signatures/
```

Storage rules:

- Raw upload is immutable.
- Extracted quarantine copy is disposable.
- Approved artifact blob is content-addressed.
- Review events are append-only.
- Publication records are immutable except revocation overlays.

## Phase 3: Repository App

Create `app/meshingress-repository` Spring Boot app.

Suggested package tree:

```txt
app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/
├─ MeshingressRepositoryApplication.java
├─ config/
│  └─ MeshingressRepositoryProperties.java
├─ artifact/
│  ├─ ArtifactController.java
│  ├─ ArtifactService.java
│  ├─ ArtifactQueryService.java
│  └─ ArtifactUploadService.java
├─ assessment/
│  ├─ AssessmentController.java
│  ├─ AssessmentService.java
│  ├─ AssessmentPipeline.java
│  └─ AssessmentJobService.java
├─ review/
│  ├─ ReviewController.java
│  ├─ ReviewService.java
│  └─ ReviewDecisionService.java
├─ publication/
│  ├─ PublicationController.java
│  ├─ PublicationService.java
│  └─ PublicationRecordSigner.java
├─ scanner/
│  ├─ ScannerAdapter.java
│  ├─ ScannerResult.java
│  ├─ syft/SyftScanner.java
│  ├─ grype/GrypeScanner.java
│  ├─ trivy/TrivyScanner.java
│  ├─ clamav/ClamAvScanner.java
│  ├─ yara/YaraScanner.java
│  ├─ semgrep/SemgrepScanner.java
│  └─ secrets/SecretScanner.java
└─ web/
   ├─ ErrorResponse.java
   └─ RepositoryExceptionHandler.java
```

## Phase 4: HTTP Routes

Artifact browsing:

```http
GET /artifact
GET /artifact/{groupId}
GET /artifact/{groupId}/{artifactId}
GET /artifact/{groupId}/{artifactId}/{version}
```

Upload:

```http
POST /artifact/{groupId}/{artifactId}/{version}
```

Metadata:

```http
GET /artifact/{groupId}/{artifactId}/{version}/metadata
GET /artifact/{groupId}/{artifactId}/{version}/manifest
GET /artifact/{groupId}/{artifactId}/{version}/checksums
GET /artifact/{groupId}/{artifactId}/{version}/sbom
GET /artifact/{groupId}/{artifactId}/{version}/attestations
```

Assessment and review:

```http
POST /artifact/{groupId}/{artifactId}/{version}/assess
GET  /artifact/{groupId}/{artifactId}/{version}/assessment
POST /artifact/{groupId}/{artifactId}/{version}/review
POST /artifact/{groupId}/{artifactId}/{version}/approve
POST /artifact/{groupId}/{artifactId}/{version}/reject
POST /artifact/{groupId}/{artifactId}/{version}/revoke
```

Publication:

```http
POST /artifact/{groupId}/{artifactId}/{version}/publish
GET  /artifact/{groupId}/{artifactId}/{version}/publication
```

## Phase 5: Security Scanner Integration

Install external tools as runtime dependencies in the repository host, not as Java dependencies unless a stable Java SDK exists.

Recommended adapters:

| Adapter | Tool | Purpose |
|---|---|---|
| `SyftScanner` | Syft | Generate SBOM from filesystem/archive/JAR/container context. |
| `GrypeScanner` | Grype | Scan SBOM/filesystem for known vulnerabilities. |
| `TrivyScanner` | Trivy | Scan vulnerabilities, misconfigurations, secrets, filesystem/repository targets. |
| `ClamAvScanner` | ClamAV | Malware/trojan/virus signature scanning. |
| `YaraScanner` | YARA | Pattern-based binary/text suspicious indicator scanning. |
| `SemgrepScanner` | Semgrep | Source/static security rule scanning. |
| `CosignSigner` | Cosign | Sign and verify publication records/artifacts. |
| `ScorecardScanner` | OpenSSF Scorecard | Evaluate upstream repo hygiene when source repo is known. |

All scanner adapters should return a normalized `ScannerResult`:

```java
public record ScannerResult(
        String scanner,
        String scannerVersion,
        ScannerStatus status,
        List<Finding> findings,
        Map<String, Object> rawSummary,
        Path rawReportPath
) {}
```

Failure policy:

- scanner unavailable => `REVIEW_PENDING` or `BLOCKED_POLICY` depending on strictness;
- malware hit => `BLOCKED_MALWARE`;
- critical vulnerability above threshold => `BLOCKED_VULNERABILITY`;
- forbidden scope/behavior => `BLOCKED_POLICY`;
- inconclusive result => `REVIEW_PENDING`.

## Phase 6: Scope Assessment

Implement requested/inferred/approved scope flow:

```txt
artifact manifest requested scopes
        +
static/dry-run inferred behavior
        +
repository policy
        +
reviewer decision
        ↓
approved scopes in publication record
```

Rules:

- `requestedScopes` are supplied by the artifact.
- `inferredScopes` are produced by analysis.
- `approvedScopes` are the only scopes the runtime may honor.
- `deniedScopes` must include reasons.
- `requestedScopes != approvedScopes` is valid and expected.

## Phase 7: Publication Record

Create `ArtifactPublicationRecord`:

```json
{
  "coordinate": {
    "groupId": "dev.mrk.tools",
    "artifactId": "generated-blender",
    "version": "1.0.0"
  },
  "type": "GENERATED_TOOL_MODULE",
  "trustStatus": "APPROVED_LIMITED",
  "artifact": {
    "uri": "meshingress-repository://artifact/dev.mrk.tools/generated-blender/1.0.0/tool.jar",
    "sha256": "..."
  },
  "scopePolicy": {
    "requestedScopes": ["PROCESS_EXECUTE", "FILES_READ", "FILES_WRITE"],
    "approvedScopes": ["PROCESS_EXECUTE", "FILES_READ"],
    "deniedScopes": [
      {
        "scope": "FILES_WRITE",
        "reason": "Write behavior not covered by tests."
      }
    ]
  },
  "scanSummary": {
    "malware": "clean",
    "criticalVulnerabilities": 0,
    "highVulnerabilities": 0,
    "secretsFound": false,
    "suspiciousRules": []
  },
  "sbom": {
    "format": "CycloneDX",
    "uri": "meshingress-repository://artifact/dev.mrk.tools/generated-blender/1.0.0/sbom.cdx.json",
    "sha256": "..."
  },
  "provenance": {
    "sourceRepo": "https://github.com/vendor/project",
    "sourceCommit": "abc123",
    "generatedBy": "cli-anything",
    "generatorCommit": "def456"
  }
}
```

Sign this record with Cosign or a configured repository signing key.

## Phase 8: MCP Runtime Integration

In `app/meshingress-server`, add an installer component that consumes only publication records.

Suggested package:

```txt
app/meshingress-server/src/main/java/dev/mrk/meshingress/server/install/
├─ ToolPublicationClient.java
├─ PublicationRecordVerifier.java
├─ ArtifactInstaller.java
├─ RuntimeToolCache.java
└─ InstallPolicyEvaluator.java
```

Runtime install checks:

```txt
1. publication record signature verifies
2. artifact checksum matches
3. trust status is APPROVED_TRUSTED or APPROVED_LIMITED
4. artifact is not revoked
5. approved scopes are permitted by local Meshingress policy
6. executable checksums match manifest
7. tool module loads without duplicate IDs
8. MCP tool metadata matches publication metadata
```

## Phase 9: CLI Commands

Potential CLI commands:

```bash
meshingress repository upload ./generated-blender.jar --group dev.mrk.tools --artifact generated-blender --version 1.0.0
meshingress repository assess dev.mrk.tools:generated-blender:1.0.0
meshingress repository status dev.mrk.tools:generated-blender:1.0.0
meshingress repository approve dev.mrk.tools:generated-blender:1.0.0 --scope PROCESS_EXECUTE --scope FILES_READ
meshingress repository publish dev.mrk.tools:generated-blender:1.0.0
meshingress tools install dev.mrk.tools:generated-blender:1.0.0
```

## Phase 10: Tests

Test groups:

```txt
artifact-model unit tests
filesystem storage tests
upload/quarantine integration tests
scanner adapter contract tests
fake scanner pipeline tests
review state machine tests
publication record signing tests
server install rejection tests
server install success tests
```

Critical negative tests:

- artifact without checksum is rejected;
- artifact without publication record is rejected;
- artifact with invalid signature is rejected;
- artifact with revoked status is rejected;
- artifact requesting forbidden scope is rejected unless not approved;
- generated executable checksum mismatch is rejected;
- symlink escape in uploaded archive is rejected;
- scanner timeout fails closed.

# PRD: Meshingress Repository

## Product Summary

`meshingress-repository` is a private artifact repository for Meshingress-managed runtime assets. It behaves like a Maven-style repository from the user's perspective, but it stores additional security, scope, review, SBOM, provenance, and publication metadata needed by the Meshingress runtime.

## Users

- Meshingress developer uploading generated or first-party artifacts.
- Security reviewer approving/rejecting artifacts.
- Meshingress server fetching approved publication records.
- Future UI/catalog browser discovering available artifacts.
- Future automation agents generating and submitting tool modules.

## Primary Use Cases

### UC-1: Upload generated tool module

A user or agent uploads a generated CLI-wrapper tool module built from an external repo. The repository assigns coordinates, quarantines the payload, computes checksums, scans it, and marks it `REVIEW_PENDING`.

### UC-2: Review artifact scopes

A reviewer sees requested scopes, detected executable behavior, scan findings, and generated metadata. The reviewer approves only the permitted scopes.

### UC-3: Publish approved artifact

The repository creates a signed publication record containing artifact URI, checksums, trust status, approved scopes, SBOM reference, scan summary, and provenance.

### UC-4: Runtime installation

`meshingress-server` installs only from signed publication records. It verifies signature, checksum, trust status, revocation state, and scope compatibility before exposing the tool through MCP.

### UC-5: Upload availability annotation module

A developer uploads an artifact that contains an annotation and associated availability policy/condition. The repository scans and reviews it under an artifact type separate from tools.

## Artifact Coordinates

Use Maven-like coordinates:

```txt
groupId:artifactId:version[:classifier]@packaging
```

Examples:

```txt
dev.mrk.tools:generated-blender:1.0.0@jar
dev.mrk.availability:enable-on-days:1.0.0@jar
dev.mrk.policy:default-scope-policy:1.0.0@json
dev.mrk.schemas:tool-publication-record:1.0.0@json
```

## Artifact Types

```java
public enum MeshingressArtifactType {
    TOOL_MODULE,
    GENERATED_TOOL_MODULE,
    CLI_HARNESS,
    AVAILABILITY_ANNOTATION,
    AVAILABILITY_POLICY,
    SCOPE_POLICY,
    SECURITY_POLICY,
    JSON_SCHEMA,
    SBOM,
    ATTESTATION,
    TEMPLATE,
    RUNTIME_PLUGIN
}
```

## Trust Status Model

```java
public enum ToolTrustStatus {
    RECEIVED,
    QUARANTINED,
    SCANNING,
    REVIEW_PENDING,
    APPROVED_TRUSTED,
    APPROVED_LIMITED,
    REJECTED,
    BLOCKED_MALWARE,
    BLOCKED_POLICY,
    BLOCKED_VULNERABILITY,
    SUPERSEDED,
    REVOKED
}
```

Installable states:

```txt
APPROVED_TRUSTED
APPROVED_LIMITED
```

Non-installable states:

```txt
RECEIVED
QUARANTINED
SCANNING
REVIEW_PENDING
REJECTED
BLOCKED_MALWARE
BLOCKED_POLICY
BLOCKED_VULNERABILITY
SUPERSEDED
REVOKED
```

## Functional Requirements

### Artifact Upload

- Accept multipart upload or repository-local artifact path.
- Require coordinate metadata or infer it from `pom.xml`/manifest when present.
- Assign immutable content digest.
- Store raw upload without executing it.
- Move upload into quarantine state.

### Artifact Inspection

- Unpack archive/JAR in a non-executable quarantine directory.
- Reject symlinks escaping extraction root.
- Identify executable files.
- Identify scripts, launchers, native binaries, classpath JARs, manifests, schemas, SBOMs, signatures, and attestations.
- Compute SHA-256 for every file and a tree digest for the full artifact.

### Security Assessment

Run a pluggable assessment chain:

- SBOM generation.
- Vulnerability scan.
- Malware scan.
- YARA binary/text rule scan.
- Semgrep source/static rules.
- Secret scan.
- Suspicious script pattern detection.
- Executable behavior dry-run where safe.
- Scope inference and scope-policy comparison.

### Review

- Store automated scan results.
- Store manual reviewer decisions.
- Track requested scopes, inferred scopes, approved scopes, and denied scopes.
- Require review before publication unless explicit policy allows a trusted first-party bypass.

### Publication

- Generate signed publication record.
- Include artifact coordinate, artifact digest, file checksums, approved scopes, denied scopes, trust status, SBOM URI, assessment summary, provenance, and revocation state.
- Publish immutable version records.
- Support revocation without deleting old records.

### Runtime Consumption

- Runtime fetches publication record.
- Runtime verifies signature.
- Runtime verifies artifact checksum.
- Runtime verifies trust status is installable.
- Runtime verifies artifact is not revoked.
- Runtime verifies approved scopes comply with local runtime policy.
- Runtime installs artifact into a runtime-controlled cache.

## Non-Functional Requirements

- No execution during ingest before quarantine checks.
- Reproducible checksums.
- Immutable approved artifacts.
- Append-only review/audit history.
- Pluggable storage backend.
- Pluggable scanner integration.
- Scanner failures fail closed by default.
- Human-readable and machine-readable reports.
- Future-compatible with UI/catalog browsing.

## Acceptance Criteria

- Uploading an artifact creates a repository record in `RECEIVED` then `QUARANTINED`.
- SHA-256 checksums are computed for the artifact and extracted executable files.
- At least Syft, Grype or Trivy, ClamAV, YARA, Semgrep, and Cosign integration points exist behind interfaces.
- A scan result can transition an artifact to `REVIEW_PENDING`, `BLOCKED_MALWARE`, `BLOCKED_VULNERABILITY`, or `BLOCKED_POLICY`.
- A reviewer can approve an artifact as `APPROVED_TRUSTED` or `APPROVED_LIMITED`.
- A signed publication record can be generated for approved artifacts.
- `meshingress-server` refuses artifacts without a valid publication record.

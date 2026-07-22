# Artifact Review and Publication — `meshingress-artifact-model`

## Package Role

This package is the shared, persistence-neutral domain model for repository artifacts, trust assessment, scope declarations, provenance, and publication records.

## User-Visible Contribution

Artifact review and installation surfaces can consistently identify what was uploaded, how it was assessed, what scopes it requests, whether it is eligible, and whether it was signed or revoked.

## Position in the Feature Path

```text
artifact upload
  -> artifact model
  -> storage, scope/security assessment, and publication signing
  -> repository review response / install policy
```

## Entry Points

- `ArtifactCoordinate`, `ArtifactRecord`, and `ArtifactFileEntry`.
- `ArtifactAssessmentSummary`, `ArtifactScopeDeclaration`, and `ArtifactTrustStatus`.
- `ArtifactPublicationRecord`, provenance, eligibility, checksum, and artifact-type records/enums.

## Feature Contract

```yaml
artifactPublication:
  coordinate: ArtifactCoordinate
  type: MeshingressArtifactType
  trustStatus: ArtifactTrustStatus
  artifactUri: string
  artifactChecksum: ArtifactChecksum
  scopePolicy: ArtifactScopeDeclaration
  scanSummary: ArtifactAssessmentSummary
  provenance: ArtifactProvenance
  eligibilityDecision: PublicationEligibilityDecision
  signature: optional signing fields
```

## Dependencies

- Upstream: repository upload/review services.
- Downstream: storage, security, publication signing, and installation verification.

## Failure Behavior

Required publication fields, including coordinate, URI, and checksum, are validated by `ArtifactPublicationRecord`; invalid values fail before a valid record is created.

## Verification

Compile the artifact reactor and exercise repository review/publish tests after changing a model record.

## Evidence and Open Questions

Confirmed by the model records and enums under `artifact/model`. It owns no runtime configuration or storage implementation.

# Artifact Review and Publication — `meshingress-artifact-publication`

## Package Role

This package signs and verifies the signature shape of publication records using pluggable HMAC or Ed25519 implementations.

## User-Visible Contribution

Published artifact records can carry a key identifier, algorithm, and signature that downstream install/review policy can verify before trusting a module.

## Position in the Feature Path

```text
reviewed ArtifactPublicationRecord
  -> PublicationRecordSigner
  -> PublicationSignature
  -> persisted publication record
  -> installation verification
```

## Entry Points

- `PublicationRecordSigner`.
- `HmacPublicationRecordSigner` and `Ed25519PublicationRecordSigner`.
- `PublicationSignature` and `PublicationSigningException`.

## Feature Contract

```yaml
input: ArtifactPublicationRecord
output:
  keyId: string
  algorithm: string
  value: signature string
```

## Dependencies

- Upstream: `ArtifactService` and repository publication policy.
- Downstream: persisted `ArtifactPublicationRecord` and installation-side signature verification.

## Failure Behavior

Unsupported keys, algorithms, or signing operations produce `PublicationSigningException`; signature material is not stored as a package resource.

## Verification

Exercise the repository publication/verification flow after changing a signer implementation.

## Evidence and Open Questions

Confirmed by the signer interface, two implementations, and signature record. Key management and active signer selection are external configuration concerns.

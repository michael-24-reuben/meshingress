# Fixes

## Files Changed

- `lib/meshingress-artifact-model/src/main/java/dev/mrk/meshingress/artifact/model/PublicationEligibilityDecision.java`
- `lib/meshingress-artifact-model/src/main/java/dev/mrk/meshingress/artifact/model/ArtifactPublicationRecord.java`
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/config/MeshingressRepositoryProperties.java`
- `app/meshingress-repository/src/main/resources/application.properties`
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/store/ArtifactMetadataStore.java`
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/store/SqlArtifactMetadataStore.java`
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/ArtifactService.java`
- `app/meshingress-repository/src/test/java/dev/mrk/meshingress/repository/artifact/ArtifactRepositoryFlowTests.java`
- `app/meshingress-repository/src/test/java/dev/mrk/meshingress/repository/artifact/ArtifactRepositoryMissingScannerEligibilityTests.java`
- `app/meshingress-repository/src/test/java/dev/mrk/meshingress/repository/artifact/ArtifactRepositoryPublicationEligibilityTests.java`
- `app/meshingress-repository/src/test/java/dev/mrk/meshingress/repository/artifact/ArtifactRepositoryUntrustedBuilderEligibilityTests.java`
- `app/meshingress-repository/src/test/java/dev/mrk/meshingress/repository/artifact/ArtifactRepositorySigningKeyEligibilityTests.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/server/install/PublicationRecordVerifier.java`

## Behavior

- Added persisted `PublicationEligibilityDecision` evidence to publication records.
- Added repository publication eligibility configuration for enablement, reviewer approvals, required scanners, required provenance fields, trusted builders, trusted signing key ids, and revoked signing key ids.
- Publication now denies missing required scanner evidence, failed or blocked required scanner evidence, insufficient approvals, unreviewed inferred scopes, approved denied scopes, missing required provenance, untrusted builders, missing signing metadata, unknown signing keys, and revoked signing keys.
- Accepted eligibility decisions are included in the signed current publication payload.
- Runtime signature verification continues to support legacy blank-key HMAC publication records.

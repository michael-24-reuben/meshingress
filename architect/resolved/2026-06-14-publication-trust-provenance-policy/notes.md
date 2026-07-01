# Notes

## 2026-06-17 Activation

- Activated after `architect/resolved/2026-06-14-repository-review-security-audit` completed role-gated repository APIs and lifecycle audit metadata.
- The repository scanner sandbox pipeline remains deferred because the current sequencing policy says external scanner CLI integrations should wait until the embedded Java/library assessment path is complete.
- Safest next action is to inventory the existing HMAC publication signer/verifier and publication install tests, then define a narrow asymmetric/provenance first slice.

## 2026-06-18 First HMAC Key-Id / Provenance Slice

- Inventory result: repository publication creation used `PublicationRecordSigner` plus `HmacPublicationRecordSigner`; runtime install used `PublicationRecordVerifier` with shared-secret `HmacSHA256`; publication records already had `ArtifactProvenance` but repository publish wrote it as empty.
- Implemented a narrow compatibility slice, not full asymmetric signing: `PublicationSignature` now carries `keyId`, `PublicationRecordSigner` exposes `keyId()` and `algorithm()`, and `ArtifactPublicationRecord` carries `signatureKeyId`.
- Repository publish and revoke now sign an unsigned payload that includes `signatureKeyId`, `signatureAlgorithm`, and provenance. Repository-created publication records stamp configured provenance, defaulting `generatedBy` to `meshingress-repository`.
- Runtime verification rejects unknown nonblank signing key ids, rejects unsupported algorithms, rejects unsigned records, and retains a legacy HMAC payload path for older records whose `signatureKeyId` is blank.
- Focused verification passed:
  - `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=McpPublicationInstallTests" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> pass; 10 tests.
  - `.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=ArtifactRepositoryFlowTests" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> pass; 6 tests.
- Remaining work: asymmetric algorithm selection, signing key active/revoked/rotation model, provenance policy requirements beyond signature coverage, and publish eligibility policy rules.

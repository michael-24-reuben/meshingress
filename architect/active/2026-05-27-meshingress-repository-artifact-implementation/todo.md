# Todo

## Repository Naming and Module Structure

- [x] Add `app/meshingress-repository` module.
- [x] Add `lib/meshingress-artifact-model` module.
- [x] Add `lib/meshingress-artifact-storage` module.
- [x] Add `lib/meshingress-artifact-security` module.
- [x] Add `lib/meshingress-artifact-publication` module.
- [x] Update root Maven module list.
- [x] Add local development `repository/` root with ignored runtime contents.

## Artifact Model

- [x] Implement `ArtifactCoordinate`.
- [x] Implement `MeshingressArtifactType`.
- [x] Implement `ArtifactTrustStatus`.
- [x] Implement artifact checksum model.
- [x] Implement executable file entry model.
- [x] Implement requested/inferred/approved/denied scope model.
- [x] Implement scan result summary model.
- [ ] Implement review decision model.
- [x] Implement publication record model.
- [x] Implement provenance model.

## Storage

- [ ] Define `ArtifactBlobStore`.
- [ ] Define `ArtifactMetadataStore`.
- [ ] Define `ArtifactIndexStore`.
- [ ] Define `ArtifactQuarantineStore`.
- [x] Implement filesystem storage backend.
- [ ] Add content-addressed blob storage.
- [ ] Add append-only review event storage.
- [ ] Add immutable publication record storage.

## Upload and Quarantine

- [x] Implement upload endpoint.
- [x] Compute raw artifact SHA-256.
- [x] Store raw upload immutably.
- [x] Extract archives/JARs safely.
- [ ] Reject symlink escape.
- [x] Identify executable files.
- [x] Generate file tree manifest.
- [x] Transition artifact to `QUARANTINED`.

## Scanners and External Tools

- [x] Add scanner adapter interface.
- [ ] Add scanner process runner with timeout.
- [ ] Integrate Syft for SBOM generation.
- [ ] Integrate Grype for vulnerability scanning.
- [ ] Integrate Trivy for vulnerability/misconfiguration/secret scanning.
- [ ] Integrate ClamAV for malware scanning.
- [ ] Integrate YARA for suspicious binary/text patterns.
- [ ] Integrate Semgrep for source/static analysis.
- [ ] Integrate OpenSSF Scorecard for source repo hygiene.
- [ ] Integrate Cosign for signing and verification.
- [x] Normalize scanner results.
- [x] Store raw scanner reports.

## Assessment Pipeline

- [ ] Implement assessment job service.
- [x] Implement sequential assessment pipeline.
- [ ] Add scanner timeout and failure policy.
- [ ] Add vulnerability severity thresholds.
- [ ] Add malware blocking rule.
- [ ] Add suspicious pattern review rule.
- [ ] Add forbidden scope policy rule.
- [x] Add final trust status decision logic.

## Review

- [ ] Implement review endpoint.
- [x] Implement approve endpoint.
- [ ] Implement reject endpoint.
- [ ] Implement revoke endpoint.
- [ ] Store reviewer identity/context.
- [x] Store approved scopes.
- [x] Store denied scopes with reasons.
- [x] Require review before publication.

## Publication

- [x] Generate publication record JSON.
- [x] Sign publication record.
- [ ] Store signature bundle.
- [x] Expose publication endpoint.
- [ ] Add revocation overlay.
- [ ] Add publication record schema.

## Runtime Integration

- [ ] Add publication client to `meshingress-server`.
- [ ] Add publication record verifier.
- [ ] Add artifact checksum verifier.
- [ ] Add runtime install policy evaluator.
- [ ] Add runtime tool cache.
- [ ] Reject non-installable trust statuses.
- [ ] Reject unapproved scopes.
- [ ] Verify executable checksums before execution.

## Tests

- [ ] Unit test artifact coordinate parsing.
- [ ] Unit test trust status transitions.
- [ ] Unit test checksum generation.
- [x] Integration test upload and quarantine.
- [x] Integration test scanner adapter with fake scanner.
- [x] Integration test review and approval.
- [x] Integration test publication signing.
- [ ] Runtime test rejects unsigned publication.
- [ ] Runtime test rejects checksum mismatch.
- [ ] Runtime test rejects revoked artifact.
- [ ] Runtime test installs approved artifact.

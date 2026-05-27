# Todo

## Repository Naming and Module Structure

- [ ] Add `app/meshingress-repository` module.
- [ ] Add `lib/meshingress-artifact-model` module.
- [ ] Add `lib/meshingress-artifact-storage` module.
- [ ] Add `lib/meshingress-artifact-security` module.
- [ ] Add `lib/meshingress-artifact-publication` module.
- [ ] Update root Maven module list.
- [ ] Add local development `repository/` root with ignored runtime contents.

## Artifact Model

- [ ] Implement `ArtifactCoordinate`.
- [ ] Implement `MeshingressArtifactType`.
- [ ] Implement `ArtifactTrustStatus`.
- [ ] Implement artifact checksum model.
- [ ] Implement executable file entry model.
- [ ] Implement requested/inferred/approved/denied scope model.
- [ ] Implement scan result summary model.
- [ ] Implement review decision model.
- [ ] Implement publication record model.
- [ ] Implement provenance model.

## Storage

- [ ] Define `ArtifactBlobStore`.
- [ ] Define `ArtifactMetadataStore`.
- [ ] Define `ArtifactIndexStore`.
- [ ] Define `ArtifactQuarantineStore`.
- [ ] Implement filesystem storage backend.
- [ ] Add content-addressed blob storage.
- [ ] Add append-only review event storage.
- [ ] Add immutable publication record storage.

## Upload and Quarantine

- [ ] Implement upload endpoint.
- [ ] Compute raw artifact SHA-256.
- [ ] Store raw upload immutably.
- [ ] Extract archives/JARs safely.
- [ ] Reject symlink escape.
- [ ] Identify executable files.
- [ ] Generate file tree manifest.
- [ ] Transition artifact to `QUARANTINED`.

## Scanners and External Tools

- [ ] Add scanner adapter interface.
- [ ] Add scanner process runner with timeout.
- [ ] Integrate Syft for SBOM generation.
- [ ] Integrate Grype for vulnerability scanning.
- [ ] Integrate Trivy for vulnerability/misconfiguration/secret scanning.
- [ ] Integrate ClamAV for malware scanning.
- [ ] Integrate YARA for suspicious binary/text patterns.
- [ ] Integrate Semgrep for source/static analysis.
- [ ] Integrate OpenSSF Scorecard for source repo hygiene.
- [ ] Integrate Cosign for signing and verification.
- [ ] Normalize scanner results.
- [ ] Store raw scanner reports.

## Assessment Pipeline

- [ ] Implement assessment job service.
- [ ] Implement sequential assessment pipeline.
- [ ] Add scanner timeout and failure policy.
- [ ] Add vulnerability severity thresholds.
- [ ] Add malware blocking rule.
- [ ] Add suspicious pattern review rule.
- [ ] Add forbidden scope policy rule.
- [ ] Add final trust status decision logic.

## Review

- [ ] Implement review endpoint.
- [ ] Implement approve endpoint.
- [ ] Implement reject endpoint.
- [ ] Implement revoke endpoint.
- [ ] Store reviewer identity/context.
- [ ] Store approved scopes.
- [ ] Store denied scopes with reasons.
- [ ] Require review before publication.

## Publication

- [ ] Generate publication record JSON.
- [ ] Sign publication record.
- [ ] Store signature bundle.
- [ ] Expose publication endpoint.
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
- [ ] Integration test upload and quarantine.
- [ ] Integration test scanner adapter with fake scanner.
- [ ] Integration test review and approval.
- [ ] Integration test publication signing.
- [ ] Runtime test rejects unsigned publication.
- [ ] Runtime test rejects checksum mismatch.
- [ ] Runtime test rejects revoked artifact.
- [ ] Runtime test installs approved artifact.

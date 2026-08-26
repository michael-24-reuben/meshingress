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
- [x] Document `ArtifactScopeDeclaration(requestedScopes, inferredScopes, approvedScopes, deniedScopes)` parameter semantics for upload, assessment, review, and publication records.
- [x] Implement scan result summary model.
- [x] Implement review decision model.
- [x] Implement publication record model.
- [x] Implement provenance model.

## Storage

- [ ] Define `ArtifactBlobStore`.
- [x] Define `ArtifactMetadataStore`.
- [ ] Define `ArtifactIndexStore`.
- [ ] Define `ArtifactQuarantineStore`.
- [x] Implement filesystem storage backend.
- [ ] Add content-addressed blob storage.
- [x] Add append-only review event storage.
- [x] Add immutable publication record storage.

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
- [x] Add scanner process runner with timeout.
- [x] Add artifact behavior scanner for scope inference.
- [x] Define scope inference rule catalog schema.
- [x] Add SootUp-backed bytecode reachability analysis to distinguish dependency-only risky APIs from calls reachable from Meshingress tool entrypoints.
- [ ] Add SpotBugs embedded Java assessment adapter for general Java bug/risk patterns.
- [ ] Add FindSecBugs rules to the SpotBugs adapter for Java security findings.
- [x] Add CycloneDX SBOM generation for embedded artifact inventory metadata before external Syft integration.
- [ ] Generate CodeQL query packs from the repository scope rule catalog.
- [ ] Add CodeQL result importer/normalizer for inferred scope findings.
- [ ] Document CodeQL source/build requirements and JAR-only fallback behavior.
- [x] Add bytecode matcher support before regex-only matching.
- [ ] Add source-aware matcher support for artifacts with source.
- [ ] Add regex matcher support as low-confidence fallback only.
- [ ] Add bytecode/source heuristics for file, process, network, environment, and secrets API usage.
- [ ] Integrate Syft for SBOM generation.
- [x] Integrate Grype for vulnerability scanning.
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
- [x] Add scanner timeout and failure policy.
- [ ] Compare requested scopes with inferred scopes.
- [ ] Flag missing or under-declared scopes for reviewer action.
- [x] Ensure upload initializes `inferredScopes` as empty and assessment replaces it with scanner-derived scopes.
- [x] Ensure default approval never treats uploaded `requestedScopes` as trusted scope authority for JAR tools.
- [x] Add vulnerability severity thresholds.
- [ ] Add malware blocking rule.
- [ ] Add suspicious pattern review rule.
- [ ] Add forbidden scope policy rule.
- [x] Add final trust status decision logic.

## Review

- [x] Implement review endpoint.
- [x] Implement approve endpoint.
- [x] Implement reject endpoint.
- [x] Implement revoke endpoint.
- [x] Store reviewer identity/context.
- [x] Store approved scopes.
- [x] Store denied scopes with reasons.
- [x] Require review before publication.

## Publication

- [x] Generate publication record JSON.
- [x] Sign publication record.
- [x] Store signature bundle.
- [x] Expose publication endpoint.
- [ ] Add revocation overlay.
- [x] Add publication record schema.

## Runtime Integration

- [ ] Add publication client to `meshingress-server`.
- [x] Add publication record verifier.
- [x] Add artifact checksum verifier.
- [x] Add runtime install policy evaluator.
- [x] Add runtime tool cache.
- [x] Reject non-installable trust statuses.
- [x] Reject unapproved scopes.
- [ ] Verify executable checksums before execution.
- [ ] Add `frontend/` web interface for `app/meshingress-repository` to track artifacts and reduce manual API calls/uploads.

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
- [x] Runtime test installs approved artifact.

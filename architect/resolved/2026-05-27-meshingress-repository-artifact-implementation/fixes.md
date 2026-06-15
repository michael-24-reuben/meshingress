# Fixes

## Implemented Surfaces

- Added `app/meshingress-repository` as the standalone artifact intake, assessment, review, approval, and publication app.
- Added artifact model, storage, security, publication, and scope-scanner support libraries.
- Added upload, quarantine, assessment, approval, publication, and publication-read flow for repository artifacts.
- Added scanner-backed scope inference with reachable tool-entrypoint bytecode analysis.
- Added signed publication record verification in `app/meshingress-server`.
- Added runtime artifact cache copying and checksum verification.
- Added runtime install policy checks for trust status, revocation, locally disabled scopes, unknown scopes, and unapproved installed function scopes.
- Added publication provenance to `PUBLICATION_RECORD` registrations, including coordinate, artifact URI, artifact SHA-256, trust status, runtime cache path, signature algorithm, and approved scopes.

## Runtime API Boundary

`roles/tools/installPublication` remains the MVP runtime entry point. It consumes the signed publication JSON payload directly and does not yet fetch publication records through a repository client.

## Deferred Work

- Repository client flow from server to repository.
- CycloneDX SBOM generation and external scanner adapters.
- SpotBugs, FindSecBugs, CodeQL, Semgrep, Syft, Grype, Trivy, ClamAV, YARA, Cosign, Scorecard, SLSA, and in-toto integrations.
- Append-only review event storage and immutable publication storage with revocation overlays.
- Repository frontend/catalog UI.
- Per-executable checksum enforcement for non-JAR or extracted-executable install flows.

# Artifact Review and Publication — `meshingress-artifact-security`

## Package Role

This package models scanner requests, findings, pipeline stages, processes, results, statuses, and a Grype-backed scanner adapter for artifact review.

## User-Visible Contribution

Repository review can report security findings, scanner status, severity, and policy outcomes before an artifact becomes eligible for publication or installation.

## Position in the Feature Path

```text
stored artifact
  -> ScannerAdapter / scanner pipeline
  -> ScannerResult and findings
  -> ArtifactService assessment
  -> review or publication decision
```

## Entry Points

- `ScannerAdapter`, `ScannerRequest`, `ScannerResult`, and `Finding`.
- `ScannerPipelinePlan` and `ScannerPipelineStage`.
- `GrypeScannerAdapter`, options, process runner/request/result, and failure policy.

## Feature Contract

```yaml
input: ScannerRequest
output: ScannerResult
contains:
  - ScannerStatus
  - findings
  - severity and process evidence
policy: ScannerFailurePolicy
```

## Dependencies

- Upstream: repository configuration and `ArtifactService`.
- Downstream: scanner executable/process environment and artifact model assessment.

## Failure Behavior

Scanner process failures are represented by process/result status and interpreted according to `ScannerFailurePolicy`; callers decide whether a failed scan blocks publication.

## Verification

```powershell
.\mvnw.cmd -pl lib/meshingress-artifact-security -am test
```

## Evidence and Open Questions

Confirmed by the Grype adapter, process runner, and adapter tests. Scanner installation, executable path, and production policy values are supplied by the server environment.

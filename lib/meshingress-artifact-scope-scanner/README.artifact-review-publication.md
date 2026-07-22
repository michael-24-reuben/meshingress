# Artifact Review and Publication — `meshingress-artifact-scope-scanner`

## Package Role

This package infers requested scopes from an uploaded JAR's bytecode and produces a CycloneDX-oriented dependency inventory for repository review.

## User-Visible Contribution

An operator can review inferred capability requests and dependency evidence before deciding whether a tool artifact is acceptable for publication or installation.

## Position in the Feature Path

```text
stored JAR
  -> ScopeInferenceCatalog + BytecodeScopeScanner
  -> ScopeFinding / JarScopeScanResult
  -> ArtifactService assessment
  -> publication eligibility decision
```

## Entry Points

- `BytecodeScopeScanner` and `ScopeInferenceCatalogLoader`.
- `ScopeInferenceCatalog`, rules, matchers, `ScopeFinding`, and `JarScopeScanResult`.
- `EmbeddedCycloneDxJarSbomGenerator` and `CycloneDxSbomGenerator`.
- `SootUpReachabilityAnalyzer` for richer reachability analysis.

## Configuration and Resources

`src/main/resources/scope-rules/default-bytecode-scope-catalog.json` supplies the default inference catalog. The existing [general README](README.md) explains that the catalog is loaded once at startup, then passed into scans; this feature README does not replace it.

## Feature Contract

```yaml
input:
  jar: Path
  catalog: ScopeInferenceCatalog
output:
  scan: JarScopeScanResult
  findings: list of ScopeFinding
  sbom: CycloneDxSbom
```

## Dependencies

- Upstream: repository configuration loads the catalog; `ArtifactService` requests scans.
- Downstream: artifact model scope declaration and publication eligibility policy.

## Failure Behavior

Unreadable archives, catalog errors, or bytecode-analysis failures prevent a reliable scope result and must be treated by repository review policy.

## Verification

```powershell
.\mvnw.cmd -pl lib/meshingress-artifact-scope-scanner test
```

## Evidence and Open Questions

Confirmed by scanner/catalog/SBOM sources, the default JSON resource, existing general README, and scanner tests. The production catalog selection policy remains server-owned.

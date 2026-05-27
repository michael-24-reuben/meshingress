# Context

## Background

The project is exploring an integration model where agents can point to an external repository, clone it, run `cli-anything`, generate a usable CLI wrapper, then wrap the generated CLI and upstream executable inside a Java Meshingress tool module. That module can then be packaged as a JAR and installed into the Meshingress runtime.

The user identified a security concern with directly installing generated wrappers. The chosen architecture splits artifact handling into two systems:

```txt
Meshingress Repository
  - artifact intake
  - quarantine
  - scanning
  - review
  - approval
  - signing
  - publication records

Official MCP Runtime
  - consumes approved publication records
  - verifies checksums/signatures
  - enforces scopes
  - exposes tools through MCP
```

## Final Recommended Structure

Keep this structure:

```txt
meshingress/
├─ app/
│  ├─ meshingress-server/
│  └─ meshingress-repository/
│
├─ lib/
│  ├─ meshingress-tool-api/
│  ├─ meshingress-tool-annotations/
│  ├─ meshingress-tool-framework/
│  ├─ meshingress-artifact-model/
│  ├─ meshingress-artifact-storage/
│  ├─ meshingress-artifact-security/
│  └─ meshingress-artifact-publication/
│
├─ toolspace/
│  ├─ first-party/
│  └─ generated/
│
└─ repository/
   ├─ artifacts/
   ├─ metadata/
   ├─ indexes/
   ├─ quarantine/
   ├─ assessments/
   ├─ reviews/
   ├─ sbom/
   ├─ attestations/
   └─ signatures/
```

## Why `meshingress-repository`

`registry-storage/local-dev-only` was rejected because it sounds like an implementation detail and is too narrow. The user wants a concept similar to `mvnrepository.com/artifact`, which supports browsing and managing artifacts by recognizable coordinates.

`meshingress-repository` is broad enough for:

- generated tool modules;
- first-party tool modules;
- generated CLI harnesses;
- availability annotations;
- availability policy modules;
- scope policy bundles;
- security policy bundles;
- JSON schemas;
- templates;
- SBOMs;
- attestations;
- signatures;
- future runtime plugins.

## Existing Meshingress Alignment

Existing Meshingress tool modules are attachable Maven modules under `toolspace/`. They expose MCP tools through Spring Boot auto-configuration and the Meshingress tool API/annotations.

The tool API supports:

- `McpToolHandler` and dispatch handlers;
- `DispatchExecutionResult`;
- text and structured JSON result content;
- annotation-based `@McpTool`, `@McpFunction`, `@McpToolMapping`, `@McpToolScopes`, and `@McpConfigureMapping` declarations;
- scoped permissions through `McpToolScope`.

The planned configuration surface already includes registry behavior, dispatch limits, security posture, scope enforcement, audit behavior, and secret handling. This repository implementation should connect to those concepts rather than replace them.

## External Tools to Use

Use existing security/supply-chain tools instead of building custom engines.

### Syft

Purpose: SBOM generation from filesystems, images, and archives.

Official source: https://github.com/anchore/syft

### Grype

Purpose: vulnerability scanning from SBOMs, filesystems, and images.

Official source: https://github.com/anchore/grype

### Trivy

Purpose: broad scanning for vulnerabilities, misconfigurations, secrets, and repository/filesystem issues.

Official source: https://github.com/aquasecurity/trivy

### ClamAV

Purpose: malware/trojan/virus signature scanning.

Official source: https://www.clamav.net/

### YARA

Purpose: pattern-based malware and suspicious binary/text classification.

Official source: https://virustotal.github.io/yara/

### Semgrep

Purpose: static code analysis and security guardrails across source languages.

Official source: https://github.com/semgrep/semgrep

### Cosign / Sigstore

Purpose: signing and verifying artifacts, signatures, and attestations.

Official source: https://github.com/sigstore/cosign

### OpenSSF Scorecard

Purpose: automated security hygiene checks for upstream repositories.

Official source: https://github.com/ossf/scorecard

### CycloneDX

Purpose: SBOM format for supply-chain metadata.

Official source: https://cyclonedx.org/

### SLSA / in-toto

Purpose: provenance and supply-chain assurance model.

Official sources:

- https://slsa.dev/
- https://in-toto.io/

## Security Notes

### Do Not Trust Generated Artifacts

Generated wrappers are useful but should be treated as untrusted until the repository completes assessment and review.

### Do Not Let Requested Scopes Become Runtime Scopes

The artifact may request scopes. The repository must approve scopes. The runtime must enforce only approved scopes.

```txt
requestedScopes != approvedScopes
```

### Avoid Shell Concatenation

Any Java wrapper that invokes generated executables should use argv arrays and `ProcessBuilder`, not shell-concatenated commands.

### Fail Closed

Scanner failures, missing checksums, unsigned publication records, invalid signatures, and unknown trust states should block installation by default.

### Preserve Audit Trail

Review decisions and publication records should be append-only. Revocation should be represented as a new state/overlay, not destructive deletion.

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

### CodeQL

Purpose: source/build-aware semantic code analysis with custom query packs. Use it for scope inference when the repository has source or can create a valid Java/Kotlin CodeQL database from a build. It can express richer API/call/dataflow queries than raw regex and can return SARIF/CSV findings for review.

Official sources:

- https://github.com/github/codeql
- https://docs.github.com/code-security/codeql-cli/creating-codeql-databases
- https://codeql.github.com/docs/writing-codeql-queries/about-codeql-queries/

Important boundary: CodeQL should not be treated as the only JAR scanner. For arbitrary uploaded JARs without source or a reproducible build, bytecode-oriented analysis with ASM or SootUp remains the primary fallback.

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

### Infer Scopes From Artifact Behavior

Repository review must inspect the uploaded artifact for behavior that implies scopes even when the artifact does not request them. This is not expected to be perfect in the MVP, but it should catch common sneaky-tool patterns before publication.

Examples:

| Observed artifact behavior | Scope review implication |
|---|---|
| `new File(...)`, `Path.of(...)`, broad `java.io` usage | require filesystem scope review, then narrow to `FILES_READ`, `FILES_WRITE`, `FILES_DELETE`, etc. when the call pattern is known |
| `Files.read*`, `FileInputStream`, resource extraction from local paths | infer `FILES_READ` |
| `Files.write*`, `FileOutputStream`, archive extraction to disk | infer `FILES_WRITE` |
| `Files.delete*`, recursive delete helpers | infer `FILES_DELETE` |
| `ProcessBuilder`, `Runtime.getRuntime().exec(...)` | infer `SHELL_EXECUTE` |
| sockets, Java HTTP clients, third-party HTTP clients | infer `NETWORK_ACCESS` or narrower network scopes when available |
| environment variable reads, secret-store/client usage | infer environment/secrets scope review |

The repository should compare `requestedScopes` against inferred behavior. Missing, under-declared, or suspicious scope differences should keep the artifact in review or block publication depending on policy. Reviewers may approve only the final `approvedScopes`; inferred scopes are evidence, not automatic approval.

### Prefer Analyzer Rules Over Regex Text Matching

Scope inference should be rule-driven, but raw regex over JAR contents should be a last resort. The preferred model is a scope rule catalog that can be evaluated by stronger analyzers first:

1. Source/build-aware analysis when source is available, such as CodeQL custom queries and Semgrep rules.
2. Java bytecode analysis for JAR-only artifacts, using a library such as ASM or SootUp to inspect method calls, class references, constant pools, annotations, and bytecode instructions.
3. Security/static-analysis tools such as SpotBugs with FindSecBugs where their findings are useful evidence.
4. Regex or string-pattern matching against source, decompiled text, manifests, and resource files only when structured analysis is unavailable or as an additional low-confidence signal.

A rule catalog can still look like "patterns assigned to a scope", but each pattern should record the matcher type, confidence, and preferred execution backend:

```json
{
  "scope": "FILES_READ",
  "matchers": [
    {
      "type": "codeql-call",
      "owner": "java.nio.file.Files",
      "namePattern": "read%",
      "confidence": "high"
    },
    {
      "type": "bytecode-method",
      "owner": "java/nio/file/Files",
      "namePattern": "read.*",
      "confidence": "high"
    },
    {
      "type": "source-pattern",
      "pattern": "Files.read(...)",
      "confidence": "medium"
    },
    {
      "type": "regex",
      "pattern": "new\\s+File\\s*\\(",
      "confidence": "low",
      "reviewOnly": true
    }
  ]
}
```

Regex examples such as `new\\s+File\\(".*"\\)` are acceptable as low-confidence review hints, but should not be the primary mechanism for approving or blocking a JAR when bytecode/source-aware matching is possible.

### Scope Rule Catalog and CodeQL Query Generation

Do not make CodeQL the source of truth for the scope definitions. The repository should own a versioned scope rule catalog, for example `repository/policies/scope-rules/*.json` or a later policy artifact. CodeQL should consume that intent through generated query packs or generated predicates.

Recommended model:

```txt
scope rule catalog
  -> generated CodeQL query pack for source/build-aware analysis
  -> generated bytecode matcher table for ASM/SootUp JAR-only analysis
  -> optional regex fallback table for low-confidence review hints
```

This keeps the policy database stable even if CodeQL query syntax, model packs, or licensing/deployment constraints change.

### Avoid Shell Concatenation

Any Java wrapper that invokes generated executables should use argv arrays and `ProcessBuilder`, not shell-concatenated commands.

### Fail Closed

Scanner failures, missing checksums, unsigned publication records, invalid signatures, and unknown trust states should block installation by default.

### Preserve Audit Trail

Review decisions and publication records should be append-only. Revocation should be represented as a new state/overlay, not destructive deletion.

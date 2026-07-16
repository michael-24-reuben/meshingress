
# End-Phase Security Review: Scope Integrity Verification

## Purpose

Add a later-stage verification pass for runtime-installable Meshingress tool artifacts. The goal is to detect tools that declare too few scopes, falsify their manifest, or omit permissions required by observable behavior.

## Artifact Readability

A JAR can be inspected before loading. It should be treated as a readable artifact containing bytecode, resources, manifests, and metadata. Meshingress should scan the artifact while it is still staged, before it is moved into the installed runtime tool directory.

## Verification Inputs

- Tool manifest scopes
- `@McpToolScopes` annotations
- `@McpConfigureMapping` secrets/configuration
- Bytecode references
- Resource files
- Dependency metadata
- Checksums and signatures
- Runtime compatibility metadata

## Detection Examples

| Detected pattern | Scope concern |
|---|---|
| `System.getenv` | Missing `ENV_READ` |
| `java.nio.file.Files.read*` | Missing `FILES_READ` |
| `java.nio.file.Files.write*` | Missing `FILES_WRITE` |
| `java.nio.file.Files.delete*` | Missing `FILES_DELETE` |
| `java.lang.ProcessBuilder` | Missing `SHELL_EXECUTE` |
| `Runtime.getRuntime().exec` | Missing `SHELL_EXECUTE` |
| HTTP client or socket usage | Missing `NETWORK_ACCESS` |
| JDBC connection/query usage | Missing database scope |
| Config service access | Missing config scope |
| Native library loading | High risk / possible block |
| Dynamic class loading | High risk / possible block |

## Review Architecture

```txt
Staged JAR
  -> deterministic scanner
  -> scope inference policy
  -> AI-assisted review summary
  -> install gate
  -> persisted verification report
```

The deterministic scanner and policy engine should own enforcement. AI review can assist with summaries, suspicious patterns, and developer-facing explanations, but should not be the root authority.

## Verdicts

```txt
PASS
  Declared scopes match observed behavior.

WARN
  Suspicious behavior exists, but mapping is uncertain or admin review is recommended.

FAIL
  Observed behavior clearly exceeds declared scopes.

BLOCK
  Prohibited behavior appears, such as unsigned native loading, remote code loading, or severe manifest/signature violations.
```

## Limitations

Static scope verification is incomplete. It can be evaded or weakened by:

- reflection
- dynamic class loading
- obfuscation
- encoded strings
- native libraries
- generated code
- remote code downloads
- scripts launched indirectly
- behavior hidden in transitive dependencies

The system should present findings as risk analysis, not as proof of safety. High-risk evasive patterns should raise the verdict even when exact scope inference is uncertain.

## Development Placement

This is an end-phase/post-MVP feature. Build it after:

1. Runtime `tools/` installation layout.
2. Dynamic tool manifest schema.
3. Registry API and Maven-compatible artifact distribution.
4. Basic install/enable/disable/uninstall lifecycle.
5. Checksum/signature verification.
6. Manifest-declared scope validation.

Scope integrity verification should be added once the install path is stable enough to enforce policy consistently.

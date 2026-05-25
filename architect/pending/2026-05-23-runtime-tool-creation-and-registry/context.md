# Context

## Current Model

Meshingress tool modules currently live under `toolspace/` and are attached through Maven module/dependency wiring plus Spring auto-configuration. This works for first-party bundled tools.

## Proposed Model

Separate concerns:

```txt
toolspace/  -> source workspace
tools/      -> runtime-installed validated artifacts
registry/   -> web/API/Maven-compatible artifact distribution
```

## Runtime Environment Variables

Runtime-installed tools need configurable environment values such as endpoints, feature toggles, non-secret integration IDs, and process-level runtime values.

These should be declared in the tool manifest, supplied through runtime configuration, and exposed only to tools with appropriate scope. Secrets should continue to use the secret declaration path rather than generic environment access.

## Scope Note

Current scopes include config and external API access, but no dedicated environment-variable scope. `CONFIG_READ` could technically cover environment access, but it is too broad. Prefer adding `ENV_READ` for least-privilege control.

## Security Notes

Runtime install must validate:

- declared scopes
- declared secrets
- declared environment variables
- checksums
- signatures
- runtime compatibility
- duplicate tool IDs and invocation names
- dangerous scopes requiring explicit admin approval


## End-Phase Security Addition: Scope Integrity Verification

Runtime-installed JAR tools should eventually pass a scope integrity review before installation or activation.

A JAR is a readable ZIP-style artifact containing `.class` files, manifests, and resources. Meshingress can inspect it before loading it into the runtime. The verifier should compare declared scopes against observed code behavior.

Examples:

```txt
Declared scopes:
  EXTERNAL_API_READ

Observed behavior:
  System.getenv(...)
  java.nio.file.Files.delete(...)
  java.lang.ProcessBuilder

Potential missing scopes:
  ENV_READ
  FILES_DELETE
  SHELL_EXECUTE
```

The verifier should run before the artifact is moved into the installed runtime directory.

## Review Model

Use a layered review model:

```txt
Deterministic bytecode/resource scanner
  -> scope inference policy
  -> AI-assisted security review summary
  -> install gate decision
```

The deterministic scanner and policy engine should be the authority for blocking decisions. AI review is useful for explanation, pattern recognition, and human-readable risk summaries, but it should not be the sole enforcement mechanism.

## Known Limitations

Static scanning cannot fully prove runtime behavior. It can miss or only partially detect:

- reflection
- dynamic class loading
- obfuscated bytecode
- native library loading
- encoded strings
- generated code
- remote code loading
- script execution launched indirectly
- behavior hidden in dependencies

Because of this, scope integrity verification should return a verdict and risk level, not a mathematical guarantee.

Recommended verdicts:

```txt
PASS  - declared scopes match observed behavior
WARN  - suspicious or uncertain behavior exists
FAIL  - observed behavior clearly exceeds declared scopes
BLOCK - prohibited patterns or unsigned/untrusted artifact behavior detected
```

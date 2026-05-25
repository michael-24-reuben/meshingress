# PRD: Runtime Tool Creation and Registry

## Problem

Meshingress currently treats tool modules as repository-attached Maven modules. This is suitable for bundled tools, but it does not support third-party distribution, runtime installation, or no-restart attachment.

## Requirements

- Provide a scaffold workflow for new tools.
- Keep `toolspace/` as the source-development area.
- Introduce a runtime `tools/` directory for installed artifacts.
- Publish Meshingress tool SDK artifacts through a registry-style web/API surface.
- Support Maven-compatible artifact downloads for foreign tool authors.
- Support optional JSON API downloads and metadata lookup.
- Store tool manifests with ID, version, runtime type, scopes, secrets, compatibility, checksum, and signature metadata.
- Allow runtime-installed tools to declare custom environment variables.
- Require explicit permission for tools that read runtime environment variables.

## Proposed New Scope

Add:

```txt
ENV_READ
```

Optional later addition:

```txt
ENV_WRITE
```

`ENV_READ` should cover reading runtime-provided environment variables. Secret resolution should remain separate through the existing secret declaration path.

## Non-goals

- Do not load raw source directories directly at runtime.
- Do not require every tool to be dynamically installed.
- Do not replace the static bundled module path.
- Do not grant broad configuration access merely because a tool needs environment variables.


## End-Phase Requirement: Scope Integrity Verification

Add a later-stage security evaluator for runtime-installable tools.

The evaluator should inspect JAR contents before installation or activation and compare:

- manifest-declared scopes
- annotation-declared scopes
- observed bytecode/resource behavior
- dependency behavior where practical
- suspicious runtime capabilities

The evaluator should infer scope needs for behavior such as:

| Observed behavior | Likely required scope |
|---|---|
| `System.getenv` | `ENV_READ` |
| file read APIs | `FILES_READ` |
| file write APIs | `FILES_WRITE` |
| file delete APIs | `FILES_DELETE` |
| `ProcessBuilder` / `Runtime.exec` | `SHELL_EXECUTE` |
| sockets / HTTP clients | `NETWORK_ACCESS` |
| JDBC reads | `DATABASE_READ` |
| JDBC writes | `DATABASE_WRITE` |
| JDBC deletes | `DATABASE_DELETE` |
| config access | `CONFIG_READ` / `CONFIG_WRITE` |
| external API clients | `EXTERNAL_API_READ` / `EXTERNAL_API_WRITE` |

## Review Requirements

- Deterministic scanner must produce inspectable findings.
- Policy engine must map findings to required scopes.
- AI reviewer may summarize risk and suggest omitted scopes.
- Installer must block or warn according to policy.
- Admin override, if allowed, must be audited.
- Verdict must be stored with the installed tool record.

## Limitation Requirements

The system must explicitly document that static verification is incomplete. Reflection, obfuscation, native code, runtime downloads, and indirect script execution should elevate risk or trigger blocking rules.

## Development Phase

This should be treated as **end-phase development** after the basic registry, scaffold, runtime install lifecycle, and manifest validation are stable.

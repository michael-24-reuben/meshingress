# Context

## Repository

```txt
J:\Users\jbeas\Repositories\Dev.java-2026\artifacts\meshingress
```

Branch:

```txt
development
```

## Existing Routes

- `/mcp`
  - `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/McpController.java`
- `/artifact`
  - `app/meshingress-server/src/main/java/dev/mrk/meshingress/repository/artifact/ArtifactController.java`

The planned `/storage` route must remain a peer of these routes.

## Existing Cache Is Not Storage

`meshingress.cache` is a tool-result cache, not a physical-content serving layer.

Current known defaults:

- TTL: 5 minutes
- maximum TTL: 1 hour
- entry size: 1 MB
- aggregate size: 256 MB

Relevant locations:

- `app/meshingress-server/src/main/resources/application.properties`
- `lib/meshingress-config/src/main/java/dev/mrk/meshingress/config/MeshingressProperties.java`

Do not extend cache semantics to satisfy this capability.

## Storage Configuration Boundary

Use the server's existing `application.properties` for a `meshingress.storage.*` group, bound through a nested `MeshingressProperties.Storage` record in `lib/meshingress-config` when implementation is authorized. Do not add `storage.properties`: Spring Boot currently imports only the repository-specific `artifact.properties`, while the general Meshingress configuration model is centralized in `MeshingressProperties`.

## MCP Result Integration

Tool handlers return `DispatchExecutionResult`, which supports arbitrary JSON `structuredContent`. A `storage.lease/v1` node can therefore be represented without embedding physical bytes in the MCP response.

The storage lease is a domain-level structured node. It should not be confused with `ResultContent.type`, which classifies normal MCP content items as text, JSON, or MIME content.

## Separate Toonverse Objective

Active objective:

```txt
architect/active/2026-07-17-toonverse-source-tools
```

Current known state:

- `toonverse.fetch-chapters` accepts arbitrary inclusive ranges.
- Local `0..100` test passes.
- `.\mvnw.cmd -pl toolspace/x-open-ink-library -am test` passed with 19 tests.
- Focused server discovery verification is blocked by an unrelated compile failure:
  - `toolspace/helloworld`
  - `HelloWorldTool.java:45`
  - missing `HelloWorldGreetArgs#getName()`

Storage planning must not modify or absorb the Toonverse objective.

## Worktree Boundaries

Preserve unrelated modifications in:

- server audit files
- MCP files
- `packages/aegis`

Additional note:

- `toolspace/x-open-ink-library` is intentionally Git-ignored because of its `x-` prefix.
- Its changes do not appear in normal Git status.

## Planning Result

No storage implementation file was created or modified by this planning record.

The record is pending again because the high-level route plan still needs a code-level architecture. Implementation remains separately unauthorized, and the active Toonverse objective remains in progress.

## Expanded Design Files

The route and feature expansion is recorded in:

- `routes.md`
- `features.md`
- `v1-scope.md`

These files describe proposed implementation behavior but do not authorize code changes.

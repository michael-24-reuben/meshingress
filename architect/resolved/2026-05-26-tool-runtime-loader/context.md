# Context

## Existing Tool Module Model

Meshingress tool modules are attachable Maven modules that live under `toolspace/`. Each module ships with:

- a `pom.xml`
- Meshingress tool API dependency
- Meshingress tool annotations dependency
- Spring Boot auto-configuration dependency
- one or more tool classes
- a Spring auto-configuration class
- an `AutoConfiguration.imports` file

The current static workflow requires adding the module to the root Maven modules list and adding a dependency from `app/meshingress-server` to the tool module. This is documented in the tool module guide and examples.

The runtime loader should not discard this model. It should make activation more dynamic after a tool module has already been built.

## Relationship to Tool Authoring Workspace

The tool authoring workspace generator creates a ready-to-code tool module skeleton.

The runtime loader activates already-built tool artifacts.

These are separate concerns:

| Concern | Module |
|---|---|
| Generate module shell | `tool-authoring-workspace` |
| Build/package module | Maven |
| Resolve packaged artifact | `meshingress-tool-runtime-loader` |
| Load and activate artifact | `meshingress-tool-runtime-loader` |
| Execute tool calls | existing dispatch/tool framework |

## Option B as Primary

The preferred runtime loading path is Maven coordinate resolution.

This supports:

- locally built tools via `mvn install`
- downloaded tools from Maven repositories
- private/internal repositories
- dependency graph resolution
- versioned installs
- snapshot/dev workflows

The loader should accept coordinates and resolve the artifact plus runtime dependencies.

Example:

```json
{
  "groupId": "dev.mrk.toolspace",
  "artifactId": "weather-client",
  "version": "0.0.1-SNAPSHOT"
}
```

If the artifact was installed locally, resolution should find it in `~/.m2/repository`.

If the artifact is not local and remote repositories are allowed, resolution may download it from configured repositories.

## Option A as Secondary

Direct JAR loading remains useful as a fallback path.

Example:

```json
{
  "jarPath": "/opt/meshingress/plugins/weather-client.jar"
}
```

This is useful for:

* quick local tests
* manual installs
* debug workflows
* air-gapped deployments
* shaded all-in-one artifacts

However, direct JAR loading should not be the primary path because it does not naturally solve transitive dependency resolution.

## Runtime Reload Models

Two reload models are plausible.

### In-Process Loading

```txt
resolve artifact
-> create classloader
-> start child Spring context
-> register tools
```

Good for development and controlled runtime extension.

Risks:

* classloader leaks
* dependency conflicts
* incomplete unload
* live call coordination
* Spring context mutation complexity

### Blue/Green Replacement

```txt
instance A serves traffic
instance B starts with new tool set
traffic shifts to B
instance A drains
```

Good for production.

Risks:

* requires process supervision
* more deployment infrastructure
* not instant in-process activation

The initial runtime loader should support in-process loading while keeping strategy boundaries open for future blue/green reload.

## Why Not Main Classpath Mutation

Mutating the main JVM classpath at runtime is fragile.

Problems:

* Spring Boot has already performed classpath scanning and auto-configuration.
* Existing singleton beans may already be created.
* Dependency versions may conflict with loaded artifacts.
* Unloading classes is only possible when classloaders and all instances become unreachable.
* Threads, static fields, logging, caches, and global registries can retain references.

Controlled child contexts and isolated classloaders provide a better boundary.

## Tool Module Discovery

A runtime-loaded module may expose Spring auto-configuration through:

```txt
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

The loader needs to either:

1. read that resource and import the listed classes into a child context, or
2. require a tool descriptor that declares the auto-configuration class explicitly.

Option 1 is compatible with existing Spring Boot conventions.
Option 2 is more explicit and easier to validate.

Both may be supported.

## Expected Challenges

### Dependency Conflicts

A loaded tool may depend on a different version of a library already used by the server.

Mitigation:

* isolate tool-private dependencies
* keep Meshingress API contracts parent-owned
* reject conflicting core libraries
* consider dependency shading for complex tools

### Classloader Leaks

Classes unload only when the classloader is unreachable.

Leaks may come from:

* static fields
* non-daemon threads
* executor services
* thread locals
* logging frameworks
* global caches
* scheduled tasks
* parent registry references

Mitigation:

* require lifecycle cleanup
* close child Spring context
* close URLClassLoader
* deregister tools
* clear registry handles
* prohibit unmanaged background threads where possible

### Spring Context Boundaries

Loaded tools may need access to parent infrastructure.

Mitigation:

* provide a controlled parent context
* expose only approved beans
* avoid letting plugins mutate core server beans
* use bridge interfaces

### Security

Runtime loading arbitrary artifacts is dangerous.

Mitigation:

* repository allowlist
* checksum validation
* optional signature validation
* audit all lifecycle actions
* explicit approval for privileged scopes
* deny remote repositories by default in production
* reject unknown or duplicate module IDs

### Tool ID Conflicts

A new module may expose the same tool ID as an active module.

Mitigation options:

* reject duplicates
* allow explicit replacement only during reload
* namespace tools by module version
* keep old version active until new version validates

### Active Calls During Deactivation

A tool may be called while its module is being unloaded.

Mitigation:

* state transition to QUIESCING
* reject new calls
* wait for in-flight calls
* timeout and force stop if configured
* return clear error state if interrupted

### Maven Snapshot Behavior

SNAPSHOT artifacts are mutable.

Mitigation:

* allow snapshots in dev
* deny snapshots in production by default
* record resolved timestamp/checksum
* support explicit refresh policy

## Open Decisions

* Should artifact resolution be split into `lib/meshingress-tool-artifact-resolver` later?
* Should direct JAR loading require shaded/fat JARs?
* Should plugin metadata be embedded in the JAR?
* Should the loader use Spring's `AutoConfiguration.imports` directly or a Meshingress-specific descriptor?
* Should tool modules be allowed to provide arbitrary Spring beans?
* Which beans should child contexts inherit from the parent?
* What is the initial duplicate tool ID policy?
* Are remote repositories allowed in dev only or also production?
* Should high-risk scopes block activation until approved?
* Should module activation be exposed as an MCP/admin tool later?

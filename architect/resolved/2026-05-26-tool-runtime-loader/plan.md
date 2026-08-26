# Plan

## Proposed Module

```txt
lib/meshingress-tool-runtime-loader
```

Suggested package layout:

```txt
dev.mrk.meshingress.runtime.loader
dev.mrk.meshingress.runtime.artifacts
dev.mrk.meshingress.runtime.resolution
dev.mrk.meshingress.runtime.lifecycle
dev.mrk.meshingress.runtime.spring
dev.mrk.meshingress.runtime.registry
```

## Core Principle

Do not inject JARs into the main JVM classpath.

Instead:

```txt
coordinates or jar path
-> resolve artifact and dependencies
-> build isolated classloader
-> create child Spring context
-> discover tool beans
-> register tools
-> manage lifecycle handle
```

## Architecture

```txt
Meshingress Server
└─ ToolRuntimeLoader
   ├─ ToolArtifactResolver
   │  ├─ MavenCoordinateArtifactResolver
   │  ├─ LocalJarArtifactResolver
   │  └─ PluginDirectoryArtifactResolver
   │
   ├─ ToolModuleClassLoaderFactory
   │  └─ creates isolated classloaders from resolved artifacts
   │
   ├─ ToolModuleApplicationContextFactory
   │  └─ starts child Spring contexts for tool modules
   │
   ├─ ToolModuleRegistry
   │  └─ tracks installed/active/failed modules
   │
   ├─ ToolRegistrationBridge
   │  └─ registers loaded tool beans with the Meshingress tool registry
   │
   └─ ToolModuleLifecycleController
      └─ handles activate, quiesce, deactivate, reload
```

## Option B: Maven Runtime Resolver

This is the primary implementation path.

### Input

```json
{
  "groupId": "dev.mrk.toolspace",
  "artifactId": "weather-client",
  "version": "0.0.1-SNAPSHOT",
  "repositories": []
}
```

### Resolution Order

1. local Maven repository
2. configured internal/private repositories
3. configured remote repositories
4. optional plugin cache

### Expected Output

```txt
ResolvedToolArtifact
├─ main artifact jar
├─ dependency jars
├─ coordinates
├─ checksum metadata
├─ repository source metadata
└─ optional tool descriptor metadata
```

### Pros

* Supports downloaded JAR tools.
* Supports local `mvn install` workflow.
* Handles transitive dependencies.
* Supports versioned installs.
* Works with private Maven repositories.
* Cleaner long-term architecture.

### Cons

* Requires a dependency resolution library or embedded resolver integration.
* Needs repository configuration.
* Needs checksum/signature policy.
* May require conflict resolution rules.
* Can increase runtime complexity.

## Option A: Direct Artifact Loading

This is the secondary implementation path.

### Input

```json
{
  "jarPath": "/opt/meshingress/plugins/weather-client.jar"
}
```

### Pros

* Simple development workflow.
* Useful for debugging.
* Useful for manual installs.
* Useful in air-gapped environments.
* Does not require Maven repository access.

### Cons

* Weak transitive dependency handling unless the JAR is shaded or bundled.
* Harder to verify provenance.
* Version metadata may be incomplete.
* Can drift from Maven dependency graph behavior.

## Suggested Artifact Source Abstraction

```java
public sealed interface ToolArtifactSource
        permits MavenCoordinatesSource, LocalJarSource, PluginDirectorySource {
}
```

```java
public record MavenCoordinatesSource(
        String groupId,
        String artifactId,
        String version,
        List<URI> repositories
) implements ToolArtifactSource {
}
```

```java
public record LocalJarSource(
        Path jarPath
) implements ToolArtifactSource {
}
```

```java
public record PluginDirectorySource(
        Path directory
) implements ToolArtifactSource {
}
```

## Suggested Resolver API

```java
public interface ToolArtifactResolver {
    boolean supports(ToolArtifactSource source);

    ResolvedToolArtifact resolve(
            ToolArtifactSource source,
            ToolArtifactResolutionContext context
    );
}
```

```java
public record ResolvedToolArtifact(
        ToolModuleId moduleId,
        Path mainJar,
        List<Path> runtimeClasspath,
        ToolArtifactSource source,
        Map<String, String> checksums,
        ToolModuleDescriptor descriptor
) {
}
```

## Suggested Runtime Loader API

```java
public interface ToolRuntimeLoader {
    ToolModuleHandle activate(ToolArtifactSource source);

    ToolModuleHandle activate(ResolvedToolArtifact artifact);

    void deactivate(ToolModuleId moduleId);

    ToolModuleStatus status(ToolModuleId moduleId);

    List<ToolModuleStatus> list();
}
```

## Suggested Lifecycle States

```txt
DISCOVERED
RESOLVING
RESOLVED
LOADING
LOADED
STARTING
ACTIVE
QUIESCING
STOPPING
STOPPED
UNLOADED
FAILED
```

## Activation Flow

```txt
1. Accept ToolArtifactSource.
2. Select compatible ToolArtifactResolver.
3. Resolve artifact and dependency classpath.
4. Validate artifact metadata.
5. Create isolated classloader.
6. Create child Spring ApplicationContext.
7. Import tool module auto-configuration.
8. Discover tool beans.
9. Register tools into Meshingress registry.
10. Mark module ACTIVE.
```

## Deactivation Flow

```txt
1. Mark module QUIESCING.
2. Reject new calls for tools owned by the module.
3. Wait for active calls to complete or timeout.
4. Deregister tools from the registry.
5. Close child Spring ApplicationContext.
6. Close classloader if closeable.
7. Clear strong references.
8. Mark module STOPPED or UNLOADED.
```

## Reload Flow

```txt
1. Resolve replacement artifact.
2. Validate it can load.
3. Start replacement module in parallel if possible.
4. Quiesce old module.
5. Switch registry bindings to new module.
6. Stop old module.
7. Mark reload complete.
```

## Spring Integration

Each tool module already follows Spring Boot auto-configuration conventions.

The runtime loader should locate or receive the module auto-configuration class, then create a child context that imports it.

The child context should receive parent-provided infrastructure beans where safe, such as:

* ObjectMapper
* Meshingress configuration
* registry bridge
* logging infrastructure
* approved secret resolver
* tool context factories

The auto-configuration class pattern is already used by the current `helloworld` module, where an `@AutoConfiguration` class declares a tool bean. The module also relies on a Spring `AutoConfiguration.imports` resource to expose that configuration.

## Tool Registry Bridge

The loader should not make loaded modules mutate the registry directly.

Instead, use a bridge:

```txt
child context tool beans
-> ToolRegistrationBridge
-> server-owned registry
```

This keeps ownership clear:

* child context owns instantiated tool beans
* parent server owns global registry state
* loader owns lifecycle and deregistration handles

## Dependency Conflict Strategy

Use child-first or parent-first classloading intentionally.

Recommended initial policy:

```txt
Parent-first for Meshingress API contracts.
Child-first for tool-private dependencies.
```

Parent-owned APIs should include:

* Meshingress tool API
* Meshingress annotations
* dispatch/result model
* shared MCP contracts
* logging facade

Tool-private dependencies should stay isolated where possible.

## Security Strategy

Runtime loading is high-risk. The loader should enforce:

* artifact source allowlist
* checksum verification
* optional signature verification
* repository allowlist
* deny unknown repositories by default
* no inline arbitrary URL downloads without policy approval
* no loading duplicate module IDs unless reload policy allows it
* scan metadata before activation
* require explicit approval for privileged tool scopes
* audit every install, load, activate, deactivate, and failure event

## Configuration Surface

Future properties may include:

```properties
meshingress.runtime-loader.enabled=true
meshingress.runtime-loader.local-repository=${user.home}/.m2/repository
meshingress.runtime-loader.plugin-directory=./plugins
meshingress.runtime-loader.cache-directory=./data/tool-artifacts
meshingress.runtime-loader.allow-remote-repositories=false
meshingress.runtime-loader.allowed-repositories=
meshingress.runtime-loader.require-checksums=true
meshingress.runtime-loader.require-signatures=false
meshingress.runtime-loader.allow-snapshots=true
meshingress.runtime-loader.activation-timeout=30s
meshingress.runtime-loader.deactivation-timeout=30s
meshingress.runtime-loader.reload-strategy=in-process
```

## Implementation Phases

### Phase 1: Contracts and Registry

* Add module skeleton.
* Define artifact source contracts.
* Define resolved artifact model.
* Define lifecycle state model.
* Define runtime loader interface.
* Define module handle/status records.

### Phase 2: Local JAR Fallback

* Implement direct JAR resolver.
* Implement isolated classloader creation.
* Implement basic child context startup.
* Implement manual activation/deactivation.
* Use this to validate lifecycle mechanics before adding Maven resolution complexity.

### Phase 3: Maven Coordinate Resolver

* Add Maven coordinate resolver.
* Resolve from local Maven repository first.
* Resolve transitive runtime dependencies.
* Support repository configuration.
* Cache resolved artifacts.

### Phase 4: Registry Integration

* Discover tool beans from child context.
* Register discovered tools with Meshingress tool registry.
* Track ownership by module ID.
* Deregister tools during deactivate.

### Phase 5: Safety and Governance

* Add checksums.
* Add repository allowlist.
* Add audit events.
* Add approval checks.
* Add duplicate/reload policies.
* Add active-call drain/quiesce handling.

### Phase 6: Reload Strategies

* Add in-process reload.
* Add blue/green strategy abstraction.
* Keep blue/green as production-oriented future path.


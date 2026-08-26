# PRD: Tool Runtime Loader

## Problem

Meshingress currently treats tool modules as attachable Maven modules that are added to the root build and server dependency graph. This works for static development but limits runtime extensibility.

A developer or model-generated workspace may produce a valid tool module, but the server still needs a reliable way to discover, resolve, load, and activate the built artifact.

The system needs a runtime loader that can handle:

- local development artifacts installed into `~/.m2`
- downloaded tool modules from Maven-compatible repositories
- manually supplied JARs or plugin directories
- dependency resolution
- isolated activation
- clean lifecycle management
- future reload/deactivation behavior

## Goals

- Add `lib/meshingress-tool-runtime-loader`.
- Use Maven coordinate resolution as the primary artifact acquisition mechanism.
- Support direct JAR loading as a secondary fallback.
- Resolve tool artifacts and their runtime dependencies.
- Load tools without mutating the main application classpath.
- Activate tool modules through controlled lifecycle states.
- Support integration with the existing Meshingress tool registry.
- Preserve compatibility with existing tool module conventions.
- Define safe unload/deactivation behavior, even if full class unloading is initially best-effort.
- Prepare for future blue/green reload support.

## Non-Goals

- Do not generate tool business logic.
- Do not replace the tool authoring workspace generator.
- Do not require runtime editing of the root `pom.xml`.
- Do not require runtime editing of `app/meshingress-server/pom.xml`.
- Do not initially guarantee perfect JVM class unloading.
- Do not directly inject arbitrary JARs into the main server classpath.
- Do not make Maven `pom.xml` parsing the only runtime contract.

## Primary Use Case: Maven Coordinate Resolution

A tool module is built and installed or published:

```bash
cd toolspace/example-tool
mvn install
```

The runtime loader receives coordinates:

```json
{
  "groupId": "dev.mrk.toolspace",
  "artifactId": "example-tool",
  "version": "0.0.1-SNAPSHOT"
}
```

The resolver locates the artifact from:

1. local Maven repository
2. configured private repositories
3. configured remote repositories

It resolves the tool JAR and dependencies, builds an isolated runtime classpath, starts the tool module context, discovers tool beans, and registers them with the Meshingress tool registry.

## Secondary Use Case: Direct JAR Loading

The runtime loader receives a file path:

```json
{
  "jarPath": "/opt/meshingress/plugins/example-tool.jar"
}
```

The loader inspects the artifact, optionally reads embedded metadata, builds a classloader, starts a child context, and registers the discovered tools.

This path is secondary because it does not naturally solve transitive dependency resolution unless the artifact is shaded, bundled, or accompanied by a dependency manifest.

## Future Use Case: Blue/Green Runtime Replacement

For production deployments, in-process hot loading may be less safe than process replacement.

The loader design should allow a future strategy where:

1. instance A continues serving traffic
2. instance B starts with the new tool set
3. traffic is shifted to instance B
4. instance A drains and shuts down

This is safer for production than attempting to mutate a live Spring/JVM dependency graph.

## Acceptance Criteria

* A new `lib/meshingress-tool-runtime-loader` module is defined.
* The module exposes a clear artifact resolution API.
* The module exposes a clear runtime activation API.
* Maven coordinate resolution is the main flow.
* Direct JAR loading is available as a fallback flow.
* The design separates artifact resolution from runtime activation.
* Loaded modules use isolated classloaders or equivalent isolation.
* Loaded modules can expose Spring auto-configuration classes.
* Loaded modules can register tool beans into the existing Meshingress registry.
* The module defines lifecycle states for install, resolve, load, start, active, quiesce, stop, unload, and failed.
* Known risks around classloader leaks, dependency conflicts, unload semantics, and security are documented.


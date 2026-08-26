# Tool Runtime Loader

Design `lib/meshingress-tool-runtime-loader`, a Meshingress library responsible for resolving, loading, activating, deactivating, and potentially reloading packaged tool modules while the server is running.

The primary goal is to support runtime activation of already-built tool modules without requiring developers to manually wire every generated module into the main server build.

This module should support two artifact acquisition paths:

1. **Primary: Maven coordinate resolution**
   - Resolve a tool module using Maven coordinates such as `groupId:artifactId:version`.
   - Support local Maven repository resolution, private repositories, and remote repositories.
   - Resolve transitive dependencies needed by the tool module.

2. **Secondary: direct artifact loading**
   - Load a specific JAR or artifact bundle from disk.
   - Useful for development, manual installs, air-gapped environments, debugging, and fallback workflows.

The runtime loader must avoid treating runtime activation as uncontrolled "JAR injection" into the main server classpath. Instead, loaded tool modules should be activated through controlled lifecycle boundaries, preferably isolated classloaders and child Spring application contexts.

Meshingress tool modules already follow a Maven/Spring structure: each module lives under `toolspace/`, declares Meshingress tool dependencies, provides a Spring Boot auto-configuration entry, and exposes one or more tool beans. The runtime loader should preserve that model while making activation more dynamic after a tool module has already been built.


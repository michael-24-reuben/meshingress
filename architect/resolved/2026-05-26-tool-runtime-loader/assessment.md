# Assessment

The original gap was that Meshingress had a static tool-module model: tools could be added to the Maven reactor and server dependency graph, but there was no library-level runtime boundary for resolving an already-built artifact, loading it in an isolated context, discovering handlers, and registering those handlers into the live dispatch registry.

The resolved first implementation adds that boundary as `lib/meshingress-tool-runtime-loader`. It intentionally keeps artifact resolution, Spring activation, lifecycle state, and parent registry registration as separate contracts so later work can replace the local resolver with full Maven Resolver support without rewriting activation.

The delivered Maven coordinate flow is local-repository first. It resolves artifacts installed into the local Maven repository and recursively includes local runtime dependencies declared in POM files. Remote/private repository download, checksum enforcement, signature policy, and production-grade blue/green reload remain future hardening work.

Runtime activation uses a dedicated `URLClassLoader`, reads Spring Boot `AutoConfiguration.imports`, creates a child `AnnotationConfigApplicationContext`, discovers `McpToolHandler` beans or annotated tool beans, and registers the resulting handlers through the same effective registry used by dispatch.

Remaining risks:

- The classloader policy is currently parent-first because it uses `URLClassLoader`; stricter child-first handling for tool-private dependencies is still future work.
- The local Maven resolver handles concrete dependency versions in local POM files, not Maven property interpolation, dependency management, classifiers, or remote resolution.
- Quiescing and active-call draining are modeled in lifecycle states but not yet wired into `DefaultToolExecutor`.
- Activation is available as an internal Spring service but is not yet exposed through an admin MCP method.

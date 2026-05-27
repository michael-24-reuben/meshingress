# Blockers

## BLOCKED: Runtime registry integration details

The loader design depends on how the current Meshingress tool registry stores and exposes tool handlers.

Needed decision:

- Should runtime-loaded tools register through the same registry path as startup-discovered tools?
- Or should the runtime loader maintain a parallel registry layer?

Preferred direction: (Approved by User)

- Use the same effective registry exposed to dispatch.
- Track ownership metadata so runtime-loaded tools can be deregistered cleanly.

## BLOCKED: Artifact resolver dependency choice

The Maven coordinate resolver requires a resolver implementation.

Options:

1. Maven Resolver / Eclipse Aether
2. Maven Invoker
3. Gradual custom local-repository resolver first
4. External build/deploy process with only local cache loading

Preferred direction: (Approved by User)

- Start with local Maven repository resolution for `mvn install` artifacts.
- Then add full Maven Resolver support.

## BLOCKED: Plugin metadata format

The loader needs to identify module ID, version, and possibly auto-configuration classes.

Options:

1. Infer from Maven coordinates and Spring `AutoConfiguration.imports`.
2. Require a Meshingress descriptor file in the JAR.
3. Support both.

Preferred direction: (Approved by User)

- Support Spring `AutoConfiguration.imports` for compatibility.
- Add optional `META-INF/meshingress/tool-module.json` later for stronger validation.

## BLOCKED: Classloader policy

Need final policy for parent-first versus child-first classloading.

Preferred direction: (Approved by User)

- Parent-first for Meshingress API, annotations, dispatch contracts, and shared MCP contracts.
- Child-first or isolated for tool-private third-party dependencies.

## BLOCKED: Production reload strategy

In-process runtime loading is useful, but production may require blue/green replacement.

Needed decision:

- Is the first implementation dev-only?
- Or must it be production-safe from the first version?

Preferred direction: (Approved by User)

- Implement in-process loading first.
- Mark production hot reload as experimental until lifecycle and security controls are proven.


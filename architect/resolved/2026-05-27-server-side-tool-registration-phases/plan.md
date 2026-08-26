# Plan

## 1. Introduce phase and source-kind models

Add explicit enums or equivalent records:

```java
public enum ToolRegistrationPhase {
    EXPERIMENTAL,
    STAGING,
    BUNDLE,
    NATIVE
}

public enum ToolSourceKind {
    LOCAL_JAR,
    MAVEN_COORDINATES,
    CLASSPATH_BUNDLE,
    SERVER_NATIVE
}
```

Keep phases separate from source mechanics. `phase=staging` may initially use Maven coordinates, but the model should not require that every future staging source be Maven-backed.

## 2. Add registration request/response contracts

Suggested request shape:

```java
public record ToolRegistrationRequest(
        String toolId,
        ToolRegistrationPhase phase,
        LocalJarSpec localJar,
        MavenCoordinatesSpec maven,
        BundleSpec bundle,
        NativeSpec nativeTool,
        boolean replace
) {}
```

Suggested response shape:

```java
public record ToolRegistrationResult(
        String registrationId,
        String toolId,
        ToolRegistrationPhase phase,
        ToolSourceKind sourceKind,
        String status,
        Object source,
        Object provenance
) {}
```

## 3. Add `ToolRegistrationService`

Responsibilities:

- Select strategy by phase.
- Enforce phase-specific validation.
- Enforce security/scope policy.
- Acquire per-tool lock to avoid duplicate registration races.
- Coordinate dynamic activation/reconciliation.
- Persist/update registration state.
- Emit audit/provenance record.

## 4. Add phase-specific registration strategies

Create strategy interface:

```java
public interface ToolRegistrationStrategy {
    ToolRegistrationPhase phase();
    ToolRegistrationResult register(ToolRegistrationRequest request, ToolRegistrationContext context);
}
```

Implement:

- `ExperimentalToolRegistrationStrategy`
- `StagingToolRegistrationStrategy`
- `BundleToolRegistrationStrategy`
- `NativeToolRegistrationStrategy`

## 5. Implement `phase=experimental`

Required behavior:

1. Lock on canonical `toolId`.
2. Locate previous active experimental registration for `toolId`.
3. Deactivate previous runtime handle/classloader.
4. Mark previous registration as replaced.
5. Activate new `LocalJarSource`.
6. Register descriptors.
7. Persist active registration.
8. Write audit event.

Pseudo-flow:

```java
registry.findActiveByToolIdAndPhase(toolId, EXPERIMENTAL)
        .ifPresent(existing -> {
            runtimeLoader.deactivate(existing.runtimeHandle());
            registry.markReplaced(existing.id());
            audit.experimentalToolReplaced(existing, request);
        });

ToolRuntimeHandle handle = runtimeLoader.activate(
        new LocalJarSource(Path.of(request.localJar().jarPath()))
);

return registry.createActive(toolId, EXPERIMENTAL, LOCAL_JAR, handle, context.provenance());
```

## 6. Implement `phase=staging`

Required behavior:

1. Lock on canonical `toolId`.
2. Validate Maven coordinates.
3. Apply staging conflict policy.
4. Activate via `MavenCoordinatesSource`.
5. Register descriptors.
6. Persist active registration.
7. Write audit event.

Default conflict policy:

```txt
replace same tool ID in staging
```

## 7. Implement `phase=bundle`

Required behavior:

1. Do not call runtime loader with a new external source.
2. Use classpath/bundle registry data, likely through or adjacent to `ClasspathToolBundle`.
3. Verify requested tool exists in bundle/classpath descriptors.
4. Mark/reconcile registry state as `phase=bundle`.
5. Return `BUNDLE_TOOL_NOT_PRESENT` if not found.

## 8. Implement `phase=native`

Required behavior:

1. Restrict to reserved namespace.
2. Do not allow external override.
3. Reconcile server-native descriptor/handler.
4. Prefer internal bootstrap over externally callable HTTP registration unless explicitly required.

## 9. Wire endpoint in `RolesMcpController`

Endpoint:

```txt
POST /roles/tools/register?phase=experimental|staging|bundle|native
```

Controller responsibilities:

- Parse `phase`.
- Parse request body.
- Build `ToolRegistrationHttpContext`.
- Delegate to service.
- Return `ToolRegistrationResult`.

Avoid direct loader calls in the controller.

## 10. Add audit/provenance

Persist at least:

- registration ID
- tool ID
- phase
- source kind
- source metadata
- actor ID
- request ID
- remote address
- timestamp
- previous registration replaced, if any
- runtime handle ID, if dynamic

## 11. Add tests

Minimum tests:

- `experimental` replaces previous experimental registration.
- `experimental` cannot override native by default.
- `staging` resolves Maven coordinates and records provenance.
- `bundle` fails when tool is not on classpath.
- `bundle` reconciles when descriptor exists.
- `native` rejects non-reserved namespace.
- per-tool locking prevents duplicate active experimental registrations.

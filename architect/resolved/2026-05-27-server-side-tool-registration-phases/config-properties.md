# Config Properties

## Purpose

Add typed configuration under `meshingress.tools.registration` so phase routing, replacement behavior, and override rules are controlled by server policy rather than embedded directly in controller code.

The registration endpoint should use these properties to redirect computation to the correct phase strategy:

```txt
phase=experimental -> ExperimentalToolRegistrationStrategy -> LocalJarSource
phase=staging      -> StagingToolRegistrationStrategy      -> MavenCoordinatesSource
phase=bundle       -> BundleToolRegistrationStrategy       -> ClasspathToolBundle reconciliation
phase=native       -> NativeToolRegistrationStrategy       -> server-native registry reconciliation
```

## Proposed `MeshingressProperties.Tools` Extension

Add a nested `Registration` record under `MeshingressProperties.Tools`.

```java
public record Tools(
        @Valid @NotNull Registry registry,
        @Valid @NotNull Registration registration,
        List<String> allowList,
        List<String> denyList,
        @NotNull Duration defaultTimeout,
        boolean defaultAudit,
        boolean defaultDebugTrace
) {
    public record Registry(
            boolean enabled,
            boolean failOnDuplicateToolId,
            boolean failOnInvalidToolId,
            boolean includeDisabled,
            boolean scanOnStartup,
            boolean exposePrivateTools
    ) {}

    public record Registration(
            boolean enabled,
            boolean allowExperimental,
            boolean allowStaging,
            boolean allowBundle,
            boolean allowNativeHttp,
            boolean experimentalReplaceExisting,
            boolean allowExperimentalOverrideBundle,
            boolean allowExperimentalOverrideStaging,
            boolean allowStagingOverrideBundle,
            boolean allowOverrideNative,
            StagingConflictPolicy stagingConflictPolicy,
            @NotBlank String localJarRoot,
            boolean requireLocalJarChecksum,
            boolean requireMavenVersionPin,
            boolean requireApprovalForDynamicPhases
    ) {}
}
```

## New Enum

```java
public enum StagingConflictPolicy {
    REPLACE_EXISTING,
    REJECT_IF_EXISTS,
    ALLOW_MULTIPLE_IF_INVOCATION_NAMES_UNIQUE
}
```

## Recommended Defaults

```yaml
meshingress:
  tools:
    registration:
      enabled: true

      # Phase gates
      allow-experimental: true
      allow-staging: true
      allow-bundle: true
      allow-native-http: false

      # Experimental behavior
      experimental-replace-existing: true
      allow-experimental-override-bundle: false
      allow-experimental-override-staging: true

      # Staging behavior
      staging-conflict-policy: replace-existing
      allow-staging-override-bundle: false

      # Native protection
      allow-override-native: false

      # Dynamic source safety
      local-jar-root: tools/lib
      require-local-jar-checksum: true
      require-maven-version-pin: true
      require-approval-for-dynamic-phases: true
```

## Environment-Specific Override Example

For local development only:

```yaml
meshingress:
  security:
    mode: dev
  tools:
    registration:
      allow-experimental-override-bundle: true
      require-local-jar-checksum: false
```

Production should keep `allow-experimental-override-bundle=false`, `allow-override-native=false`, and `allow-native-http=false`.

## Computation Redirection Rules

The controller must not directly instantiate loader sources. It should redirect by phase into the service strategy map.

```java
ToolRegistrationPhase phase = ToolRegistrationPhase.fromHttpValue(request.getParameter("phase"));
ToolRegistrationStrategy strategy = strategies.get(phase);
return strategy.register(registrationRequest, registrationContext);
```

Validation order:

1. Check `meshingress.tools.registration.enabled`.
2. Check the per-phase gate, such as `allowExperimental` or `allowStaging`.
3. Validate request payload shape for the phase.
4. Check override/conflict policy.
5. Check authorization/scope requirements.
6. Call the selected strategy.

## Staging Conflict Policy

When `phase=staging` registers a tool ID that already exists in staging, use `stagingConflictPolicy`:

| Value | Behavior |
|---|---|
| `REPLACE_EXISTING` | Deactivate the active staging registration for the same canonical tool ID, then activate the new one. |
| `REJECT_IF_EXISTS` | Fail with a deterministic conflict error. |
| `ALLOW_MULTIPLE_IF_INVOCATION_NAMES_UNIQUE` | Allow side-by-side versions only if discovered invocation names do not collide. |

Recommended default: `REPLACE_EXISTING`.

## Override Policy

Default precedence remains:

```txt
native > bundle > staging > experimental
```

Override flags should be interpreted as explicit exceptions to the precedence rule.

| Existing active phase | Incoming phase | Default behavior | Config exception |
|---|---|---|---|
| `native` | any dynamic phase | reject | never recommended; controlled by `allowOverrideNative` |
| `bundle` | `experimental` | reject | allow if `allowExperimentalOverrideBundle=true` |
| `bundle` | `staging` | reject | allow if `allowStagingOverrideBundle=true` |
| `staging` | `experimental` | allow in dev-style workflow | controlled by `allowExperimentalOverrideStaging` |
| `experimental` | `experimental` | replace | controlled by `experimentalReplaceExisting` |

## Error Codes

Suggested deterministic errors:

```txt
TOOL_REGISTRATION_DISABLED
TOOL_REGISTRATION_PHASE_DISABLED
TOOL_REGISTRATION_INVALID_PHASE
TOOL_REGISTRATION_INVALID_PAYLOAD
TOOL_REGISTRATION_CONFLICT
TOOL_REGISTRATION_OVERRIDE_DENIED
TOOL_REGISTRATION_NATIVE_HTTP_DENIED
TOOL_REGISTRATION_BUNDLE_TOOL_NOT_PRESENT
TOOL_REGISTRATION_CHECKSUM_REQUIRED
TOOL_REGISTRATION_VERSION_PIN_REQUIRED
```

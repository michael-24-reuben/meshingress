# Context

## Prior Architecture Decision

Long-term tools should not crowd `app/meshingress-server/pom.xml`. Stable long-term tools should be composed through `app/meshingress-tool-bundle/pom.xml`, leaving the server module focused on runtime hosting, transport, dispatch, registry, security, audit, and configuration.

`MavenCoordinatesSource` should be treated as a temporary staging mechanism for tools that are versioned but not yet promoted into the bundle. It should not be the preferred permanent state for long-term tools because install-time resolution creates runtime speed drag and extra memory/classloader pressure compared with building the selected tools into the deployment.

## Phase Definitions

### `phase=experimental`

Backed by local runtime JAR loading. This is intended for local development, debugging, operator drop-ins, and ad hoc testing.

Required behavior:

1. Find any active previous experimental registration for the same canonical tool ID.
2. Deactivate/unregister that previous experimental attachment.
3. Close or detach associated runtime handles/classloaders where applicable.
4. Activate the new local JAR source.
5. Register discovered descriptors.
6. Persist provenance with `phase=experimental` and source kind `LOCAL_JAR`.

Important invariant:

```txt
There should be at most one active experimental registration per canonical tool ID.
```

### `phase=staging`

Backed by Maven-coordinate resolution. This is intended for versioned tool artifacts that are being tested or staged before promotion into `meshingress-tool-bundle`.

Required behavior:

1. Validate Maven coordinates.
2. Resolve the artifact from local/private repositories.
3. Activate via Maven-coordinate source.
4. Register discovered descriptors.
5. Persist provenance with `phase=staging` and source kind `MAVEN_COORDINATES`.

Recommended default conflict behavior:

```txt
Replace an existing staging registration for the same canonical tool ID unless the request explicitly asks for rejection or side-by-side versions.
```

### `phase=bundle`

Backed by classpath tools already included through `app/meshingress-tool-bundle`. This phase should not install or resolve external code at request time.

Required behavior:

1. Look up already discovered classpath/bundle tool descriptors.
2. Verify the requested tool exists on the application classpath.
3. Register/reconcile the registry record with `phase=bundle` and source kind `CLASSPATH_BUNDLE`.
4. Return a deterministic error if the requested tool is not present.

Suggested error:

```json
{
  "errorCode": "BUNDLE_TOOL_NOT_PRESENT",
  "message": "phase=bundle requires the tool to be present on the application classpath"
}
```

### `phase=native`

Backed by tools implemented directly by the Meshingress server/runtime. This should be restricted to server-owned namespaces and should usually be bootstrapped internally rather than exposed as a general external registration route.

Required behavior:

1. Verify the tool belongs to a reserved native namespace.
2. Verify the server-native handler/descriptor exists.
3. Register/reconcile with `phase=native` and source kind `SERVER_NATIVE`.
4. Reject attempts to override native tools from experimental or staging sources.

Suggested reserved namespaces:

```txt
meshingress.*
system.*
runtime.*
```

## Precedence and Override Policy

Default registry precedence:

```txt
native > bundle > staging > experimental
```

Development mode may allow experimental override of bundle/staging tools, but production should reject overrides of bundled or native tools unless explicitly configured.

Suggested config surface is detailed in `config-properties.md`. The essential policy flags are:

```yaml
meshingress:
  tools:
    registration:
      enabled: true
      experimental-replace-existing: true
      staging-conflict-policy: replace-existing
      allow-experimental-override-bundle: false
      allow-staging-override-bundle: false
      allow-override-native: false
      allow-native-http: false
```

## Authorization

Tool registration is a control-plane mutation, not an ordinary tool call. It should require explicit authorization.

Suggested required scopes:

| Phase | Required permissions |
|---|---|
| `experimental` | `PLUGINS_INSTALL` + `TOOLS_REGISTER` |
| `staging` | `PLUGINS_INSTALL` + `TOOLS_REGISTER` |
| `bundle` | `TOOLS_REGISTER` or `TOOLS_UPDATE` |
| `native` | internal-only or privileged server admin |

## HTTP Context

HTTP request context should be attached to the registration record, not to the low-level loader source.

Suggested stored fields:

- request ID
- actor/principal ID
- remote address
- user agent
- requested phase
- timestamp
- source metadata
- checksum or artifact coordinates where applicable

## Related Files

### `RolesMcpController.java`

Expected role:

- Add or route `roles/tools/register` endpoint.
- Parse `phase` from HTTP context/query parameter.
- Delegate to `ToolRegistrationService`.
- Avoid embedding loader-specific logic directly in controller methods.

### `ClasspathToolBundle.java`

Expected role:

- Support `phase=bundle` reconciliation.
- Represent or expose classpath/bundle tool descriptors already present in the runtime.
- Help distinguish classpath bundle tools from native or dynamically activated tools.

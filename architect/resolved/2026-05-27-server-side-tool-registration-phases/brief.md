# Server-side Tool Registration Phases

## Problem

Meshingress needs a server-side tool registration flow attached to the roles/MCP control surface. The main install endpoint should be:

```txt
roles/tools/register
```

Registration must be phase-aware so the server can route each request to the correct behavior:

| Internal source concept | HTTP phase | Intended behavior |
|---|---|---|
| `LOCAL_RUNTIME` | `phase=experimental` | Load a local JAR dynamically. Remove any previous experimental attachment for the same tool before activating the new one. |
| `RESOLVED_INSTALL` | `phase=staging` | Resolve and activate a Maven-coordinate tool as a temporary pre-bundle/staging phase. |
| `BUNDLED_BUILD` | `phase=bundle` | Reconcile or expose tools already present through `app/meshingress-tool-bundle`. Do not dynamically install external code. |
| `SERVER_CORE` | `phase=native` | Reconcile or expose server-native tools owned by the Meshingress runtime itself. |

## Design Intent

The server should treat `roles/tools/register` as a control-plane operation. The request `phase` should determine validation, source construction, cleanup behavior, activation/reconciliation behavior, provenance, and audit records.

The endpoint should not scatter direct calls to `toolRuntimeLoader.activate(...)` across controller code. Instead, the controller should delegate to a registration service that selects a phase-specific strategy.

## Related Files

- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/RolesMcpController.java`
- `lib/meshingress-tool-runtime-loader/src/main/java/dev/mrk/meshingress/runtime/bundle/ClasspathToolBundle.java`

## Additional Related Implementation Areas

- `app/meshingress-tool-bundle/pom.xml`
- `app/meshingress-server/pom.xml`
- Runtime loader source types for local JAR and Maven-coordinate loading
- Tool registry/service code that owns active descriptors and lifecycle state
- Security/scope enforcement for plugin installation and registry mutation
- Audit/provenance recording for registration events

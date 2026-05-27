# Blockers

No hard blocker yet.

## Decisions Needed

### Staging conflict policy

Choose one default behavior when `phase=staging` registers a tool ID that already exists in staging:

1. Replace existing staging registration.
2. Reject if active staging registration exists.
3. Allow multiple versions side-by-side only if invocation names are unique.

Recommended default: replace same tool ID. Encode as `meshingress.tools.registration.staging-conflict-policy=replace-existing`.

### Override policy

Choose whether development mode should allow experimental tools to override bundle tools.

Recommended default:

```txt
production: reject experimental override of bundle/native
local dev: allow experimental override of bundle if explicitly configured
```

Encode as `meshingress.tools.registration.allow-experimental-override-bundle=false` by default, with local-dev override allowed when explicitly set to `true`.

### Native registration exposure

Choose whether `phase=native` is externally callable over HTTP or only used by internal server bootstrap.

Recommended default: internal bootstrap only, unless a privileged admin route has a clear use case.

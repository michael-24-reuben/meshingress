# Notes

## Discussion Summary

The loading lifecycle was refined from purely source-based categories into server registration phases:

- `LOCAL_RUNTIME` maps to `phase=experimental`.
- `RESOLVED_INSTALL` maps to `phase=staging`.
- `BUNDLED_BUILD` maps to `phase=bundle`.
- `SERVER_CORE` maps to `phase=native`.

Long-term tools should be moved into `app/meshingress-tool-bundle/pom.xml` rather than staying permanently installed through Maven-coordinate runtime resolution. Maven-coordinate loading remains useful as a temporary staging phase, but should not be the preferred long-term deployment mode because of runtime resolution cost and memory/classloader pressure.

`roles/tools/register` should become the main server-side registration endpoint. The phase value should drive strategy selection, validation, cleanup, provenance, and activation/reconciliation behavior.

## Important Implementation Note

`experimental` must remove any previous experimental attachment for the same tool ID before proceeding. This should be done under a per-tool lock to avoid duplicate active experimental registrations.

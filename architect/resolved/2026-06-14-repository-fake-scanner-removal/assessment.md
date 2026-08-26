# Assessment

`fake-scanner` was an MVP placeholder left in the repository scanner list after real assessment stages became available. Keeping it enabled by default made repository assessment look more complete than it was and forced tests to depend on placeholder scanner ordering.

The real implemented stages are now:

- `cyclonedx-sbom`, which writes `cyclonedx-sbom.json` and contributes compact SBOM metadata to the assessment summary.
- `bytecode-scope-scanner`, which infers scopes from bytecode and tool entrypoints.

The scanner interface remains useful for future scanners, but the fake implementation no longer belongs in the production repository pipeline.


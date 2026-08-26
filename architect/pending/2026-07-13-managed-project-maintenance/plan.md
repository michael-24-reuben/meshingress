# Plan

1. Inventory current external, non-Maven tools/assets and classify them by the ownership model in `context.md`.
2. Define and validate the vendor-lock JSON schema, including exact version, source provenance, checksum, platform compatibility, license, and manifest-owned install path.
3. Replace the obsolete maintenance script with a read-only `-Check` baseline and explicit mode parsing; do not add repair behavior in the first slice.
4. Add Maven build/dependency validation that reads declared versions but never writes to `pom.xml` or changes the Maven wrapper.
5. Add code-inspection reporting as advisory findings with stable identifiers and links/paths to evidence.
6. Add `-RepairAssets` only after the manifest and a staging/checksum/atomic-replacement test suite exist.
7. Retire the obsolete Mockito scripts once their replacement is verified and documentation points to Maven as the agent provider.

## Implementation Boundary

Each stage must be independently reviewable. Asset repair must remain unavailable until a manifest record, checksum validation, ownership-path guard, and rollback-preserving behavior are all tested.

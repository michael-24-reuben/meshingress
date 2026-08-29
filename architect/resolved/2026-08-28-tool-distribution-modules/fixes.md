# Fixes

- Added `distribution/meshingress-tool-bom` for common internal tool artifact versions.
- Added `distribution/meshingress-tool-starter` for the common API, annotations, manifest, and Spring auto-configuration dependencies.
- Added `distribution/meshingress-tool-distribution` as the authoritative shipped-tool inventory.
- Migrated the six currently shipped tools to import the BOM and depend on the starter, while preserving their tool-specific dependencies.
- Updated the server to depend on the new distribution.
- Converted `app/meshingress-tool-bundle` into a compatibility bridge to the distribution, avoiding a second tool inventory.
- Added `distribution/README.md` documenting the allowed dependency direction.
- Changed registration configuration defaults to `distribution/meshingress-tool-distribution/pom.xml`.
- Changed default runtime and API bundle identifiers to `meshingress-tool-distribution`.
- Updated registration fixtures, OpenAPI source and checked-in API snapshot, repository README, and agent guidance to identify the distribution as canonical.
- Kept the legacy bundle module and root reactor entry unchanged and explicitly documented them as a compatibility bridge.

# Fixes

## Files Changed

- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/config/MeshingressRepositoryConfiguration.java`
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/config/MeshingressRepositoryProperties.java`
- `app/meshingress-repository/src/main/resources/application.properties`
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/ArtifactService.java`
- `app/meshingress-repository/src/test/java/dev/mrk/meshingress/repository/artifact/ArtifactRepositoryFlowTests.java`
- `app/meshingress-server/src/test/java/dev/mrk/meshingress/mcp/McpPublicationInstallTests.java`
- `lib/meshingress-artifact-security/src/main/java/dev/mrk/meshingress/artifact/security/FakeScanner.java`

## Behavioral Changes

- Removed the `FakeScanner` implementation and repository configuration wiring.
- Removed `meshingress.repository.fake-scanner-enabled` from default repository properties.
- Removed the `fakeScannerMvp` assessment summary flag.
- Updated repository flow coverage to assert `cyclonedx-sbom` and `bytecode-scope-scanner` without `fake-scanner`.
- Updated publication install test fixtures so publication summaries no longer refer to `fake-scanner`.


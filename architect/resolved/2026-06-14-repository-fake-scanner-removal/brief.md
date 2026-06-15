# Brief: Repository Fake Scanner Removal

## Goal

Remove the MVP `fake-scanner` from the repository assessment path now that the embedded CycloneDX SBOM generator and bytecode scope scanner are implemented and covered by focused tests.

## Scope

- Stop wiring `FakeScanner` into `meshingress-repository`.
- Remove the production `meshingress.repository.fake-scanner-enabled` property.
- Remove fake-scanner-specific assessment summary metadata.
- Update repository flow tests to assert real scanner output.
- Keep the scanner abstraction available for future real scanner plugins.


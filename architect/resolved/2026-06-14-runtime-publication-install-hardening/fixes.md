# Fixes

## Files Changed

- `app/meshingress-server/src/main/java/dev/mrk/meshingress/server/install/RepositoryArtifactFetcher.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/RoleToolService.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/params/RolesToolInstallPublicationParams.java`
- `app/meshingress-server/src/test/java/dev/mrk/meshingress/server/install/RuntimeToolCacheTests.java`
- `architect/active/2026-06-14-runtime-publication-install-hardening/todo.md`
- `architect/active/2026-06-14-runtime-publication-install-hardening/notes.md`
- `architect/active/2026-06-14-runtime-publication-install-hardening/meta.json`

## Behavioral Changes

- Added `RepositoryArtifactFetcher.fetchPublication(...)` for `GET /artifact/{groupId}/{artifactId}/{version}/publication`.
- Coordinate-based publication fetch uses `Accept: application/json`, `X-Repository-Role`, and `X-Repository-Actor: meshingress-server`.
- `roles/tools/installPublication` now accepts either `publication` or `coordinate`, rejecting missing or ambiguous input.
- Existing inline publication installs continue through the same installer path.
- Coordinate-fetched publication records are verified by the existing signature/trust/revocation/checksum/scope gates before activation.

## Compatibility

- Existing callers that send `params.publication` are unchanged.
- Coordinate-based fetch requires `meshingress.repository.api-base-url`; blank API configuration rejects the coordinate mode before install mutation starts.

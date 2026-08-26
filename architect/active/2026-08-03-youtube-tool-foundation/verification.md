# Verification

## Commands

```powershell
.\mvnw.cmd -pl toolspace\youtube -am test "-Dtest=YoutubeToolTests" "-Dsurefire.failIfNoSpecifiedTests=false"
.\mvnw.cmd -pl app\meshingress-server -am test "-Dtest=McpYoutubeToolMvcTests" "-Dsurefire.failIfNoSpecifiedTests=false"
.\mvnw.cmd -pl toolspace\youtube -am test "-Dtest=YoutubeToolTests,YoutubeManifestTests" "-Dsurefire.failIfNoSpecifiedTests=false"
.\mvnw.cmd -pl toolspace\youtube -am test
.\mvnw.cmd -pl app\meshingress-server -am test "-Dtest=McpYoutubeToolMvcTests" "-Dsurefire.failIfNoSpecifiedTests=false"
```

## Results

- Both commands completed with `BUILD SUCCESS`.
- Module test: 2 tests passed. It verified typed capability records and the transcript/local-media boundary.
- Server test: 1 test passed. It verified Spring discovery and that only `youtube.capabilities` plus `youtube.providers` appear in `tools/list`.
- Credential-manifest update: 3 module tests passed, including blank secret-reference declarations and registration into `McpToolMetadata`; the server discovery test also passed after manifest wiring.
- Public Data API update: the module reactor passed 7 tests, including a local HTTP-server adapter test for encoded query parameters and API-key inclusion plus a missing-key fail-closed test. The server reactor passed `McpYoutubeToolMvcTests`, confirming the catalog functions and four public `youtube.data.*` functions appear in `tools/list` while creator/OAuth/transcript functions remain absent.

## Remaining verification scope

No live YouTube endpoint was contacted because the production API-key property remains blank. The local adapter test uses a loopback HTTP server only. OAuth configuration, provider adapters, and account-authorized calls remain outside this slice.

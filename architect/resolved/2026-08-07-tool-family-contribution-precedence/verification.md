# Verification

Executed successfully on 2026-08-07:

```powershell
.\mvnw.cmd -pl app/meshingress-server -am test "-Dtest=McpToolManifestExecutorTests,McpToolMetadataTests,ToolModuleMetadataTests,McpToolContributionApiTests,McpDispatchDocumentationEndpointTests,ArtifactRepositoryNativeMetadataExportTests,RuntimeToolRegistryBridgeTests,ToolContributionResolverTests,FileToolRegistrationStoreTests" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Result: 11 focused tests passed across the full required Maven reactor.

Coverage includes manifest schema/migration, role-gated list/update routing, persisted precedence changes, dynamic primary/extension ordering, fallback promotion after primary removal, duplicate-conflict persistence, artifact icon export/serving, store restart persistence, and resolver namespace validation.

Additional successful validation on 2026-08-07:

```powershell
.\mvnw.cmd -pl app/meshingress-server -am test "-Dtest=McpControllerTests,McpPropertyPolicyTests,McpPowerShellPublicationInstallInstanceTests,McpYoutubeToolMvcTests" "-Dsurefire.failIfNoSpecifiedTests=false"
.\mvnw.cmd -pl toolspace/x-open-ink-library,toolspace/x-yt-dlp,toolspace/x-faster-whisper -am test
```

The server reactor verified public listing/call contracts, property policy, PowerShell manifest loading, and the YouTube catalog. The module reactor verified Open Ink Library, yt-dlp, Faster Whisper, annotation scanning, and manifest metadata migration.

# Verification

```powershell
.\mvnw.cmd -pl app/meshingress-server -am test "-Dtest=ToolModuleCatalogTests,ToolModuleCatalogControllerTests" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Passed across the 26-module Maven reactor. Focused tests verify a bundled PowerShell `cp-…` response, an active runtime module `rt-…` response, and public ID lookup for module documents and resources.

```powershell
npm run build
```

Passed in `app/meshingress-studio-web`; TypeScript and the Vite production build completed.

Live API inspection at `http://100.121.15.11:4737` returned the PowerShell catalog entry with `cp-CDPjsg3dF9NR8ox3esZIzy6YBZc0tdMwlfYLuuendSU`; its icon endpoint returned HTTP 200, `image/svg+xml`, and 1,942 bytes. The corrected client now prefixes that relative icon path with the configured API origin.

# Verification

Executed on Windows PowerShell with Java 25:

```powershell
.\mvnw.cmd -pl app\meshingress-server -am test "-Dtest=StorageLifecyclePolicyTests,ToolStorageRouterTests,AsyncExternalHandoffWorkerTests,StorageServiceTests" "-Dsurefire.failIfNoSpecifiedTests=false"
.\mvnw.cmd -pl toolspace\x-open-ink-library -am test "-Dtest=ToonverseToolTests" "-Dsurefire.failIfNoSpecifiedTests=false"
git diff --check
```

Results:

- Server reactor: 13 focused tests passed, including concurrent local/delegated router selection and rejection of delegated-plus-queued publication.
- Toonverse reactor: 10 tests passed, including delegated media URLs without Meshingress opening media streams.
- `git diff --check` passed.

No deployment or live Nextcloud call was performed in this slice.

## Reopened regression verification

```powershell
.\mvnw.cmd -pl app\meshingress-server -am test "-Dtest=DelegatedSourceTargetConfiguredConditionTests,StorageLifecyclePolicyTests,ToolStorageRouterTests,DelegatedViewerControllerTests" "-Dsurefire.failIfNoSpecifiedTests=false"
```

- 11 focused tests passed.
- The new condition test proves whitespace-only target values do not create the delegated viewer beans, while `nextcloud-primary` does.

## Reopened configuration-binding verification

```powershell
.\mvnw.cmd -pl app\meshingress-server -am test "-Dtest=DelegatedSourceTargetConfiguredConditionTests,StorageLifecyclePolicyTests,ToolStorageRouterTests,DelegatedViewerControllerTests" "-Dsurefire.failIfNoSpecifiedTests=false"
git diff --check
```

- 12 focused tests passed.
- The binding regression test proves the canonical immutable configuration retains `delegated-target=nextcloud-primary`; this is the value that `StorageLifecyclePolicy.delegatedTarget()` now consumes.
- `git diff --check` passed before commit.

## Reopened dispatch-boundary verification

```powershell
.\mvnw.cmd -pl app\meshingress-server -am test "-Dtest=StoragePublicationMcpControllerTests,McpAnnotationDispatchMvcTests" "-Dsurefire.failIfNoSpecifiedTests=false"
.\mvnw.cmd -pl toolspace\x-open-ink-library -am test "-Dtest=ToonverseToolTests" "-Dsurefire.failIfNoSpecifiedTests=false"
node --check toolspace\x-open-ink-library\src\main\resources\mcp-ws-download-book.js
git diff --check
```

- Server reactor: 6 focused tests passed. The direct controller tests cover successful status lookup, missing identifiers, and normalized storage failure; the MVC context test confirms `storage/publication-status` is registered.
- Toonverse reactor: 9 tests passed after removal of the tool-specific publication-status function.
- JavaScript syntax and whitespace checks passed.

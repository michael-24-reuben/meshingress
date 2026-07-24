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

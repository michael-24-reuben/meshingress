# Verification

## Automated verification

```txt
.\mvnw.cmd -pl app/meshingress-server -am '-Dtest=AsyncExternalHandoffWorkerTests,StorageServiceTests,WebDavHandoffPublisherTests,StorageLifecyclePolicyTests,WorkspaceMetadataStoreTests' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Result: success; 14 focused tests passed across the reactor.

The async-worker tests cover queued return before publication, manifest-last ordering, retryable 503 retention/requeue, terminal create-only collision retention, expired-lease recovery, status inspection, and explicit requeue. Existing WebDAV and local-storage tests remained green.

`git diff --check` reported no whitespace errors for the scoped changes.

## Operational note

The currently running server process must be restarted to load the new lifecycle and worker. No live external upload was initiated as part of this verification.

## Client-observability correction

```txt
node --check toolspace/x-open-ink-library/src/main/resources/mcp-ws-download-book.js
.\mvnw.cmd -pl app/meshingress-server -am '-Dtest=ToonverseToolTests,AsyncExternalHandoffWorkerTests,StorageServiceTests,WebDavHandoffPublisherTests,StorageLifecyclePolicyTests,WorkspaceMetadataStoreTests' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Result: both commands succeeded. `ToonverseToolTests` ran 9 tests with no failures; the selected server storage tests ran 14 tests with no failures.

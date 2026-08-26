# Verification

Executed:

```powershell
.\mvnw.cmd -pl app/meshingress-server -am test "-Dtest=StorageServiceTests,StorageControllerTests" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Result: passed. `StorageServiceTests` ran 3 tests and `StorageControllerTests` ran 2 tests, with zero failures or errors.

The tests cover workspace publication, named file retrieval, generated manifest retrieval, HEAD non-consumption, request-budget exhaustion, cleanup, and the explicit session/request/file route.

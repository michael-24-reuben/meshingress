# Verification

## Commands

```powershell
.\mvnw.cmd -pl app\meshingress-server -am '-Dtest=ProfileAdmissionServiceTest,ProfileLimitServiceTest,AegisProfileIdentityResolverTest,SecurityDispatchContractTest,NativeLoginValidationControllerTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
.\mvnw.cmd -f packages\aegis\pom.xml test
```

## Result

- Focused server verification passed: 10 tests.
- Aegis verification passed: 21 tests.
- The profile admission tests prove closed mode creates no profile and
  self-service creates one pending, unprivileged profile per verified identity.
- The limit tests prove the global switch and stable rate-limit result.
- The identity resolver test proves duplicate issuer/subject bindings are
  rejected.

## Known Limitations

- No live Google browser token was logged or used in tests.
- Storage-byte policy requires later profile attribution at the storage
  lifecycle boundary before it can be enforced.

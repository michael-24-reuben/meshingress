# Verification

## Commands

```powershell
.\mvnw.cmd -pl packages/aegis test
.\mvnw.cmd -pl packages/aegis package
jar tf packages\aegis\target\aegis-0.0.1-SNAPSHOT.jar
```

## Result

- `mvnw.cmd -pl packages/aegis package` passed on 2026-08-14.
- All 15 Aegis tests passed: existing profile/schema/grant tests plus bootstrap
  configuration and lifecycle tests.
- The packaged JAR contains `application.properties`,
  `BootstrapEnrollmentService.class`, and
  `InMemoryBootstrapEnrollmentStore.class`.
- Tests cover safe defaults, configurable parsing, invalid configuration,
  expiry, revocation, restricted action, bounded attempts, explicit rotation,
  single consumption, and concurrent contenders with exactly one winner.

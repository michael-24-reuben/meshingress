# Assessment

The reusable boundary is complete without making Aegis an authentication
server. Bootstrap evidence is verified by a host-provided
`BootstrapCredentialVerifier`; Aegis retains only an opaque issuance UUID and
safe lifecycle metadata. The host-owned `BootstrapEnrollmentStore.enroll`
contract requires the no-active-administrator check, issuance consumption, and
profile persistence to occur atomically.

`InMemoryBootstrapEnrollmentStore` provides a synchronized reference
implementation for isolated tests and local demonstrations. Production hosts
must replace it with a durable transaction-capable store. Aegis does not
generate a secret, persist a digest, resolve a secret reference, handle HTTP,
or issue a bootstrap credential automatically on startup.

The module now reads its own `application.properties` through
`BootstrapEnrollmentProperties`. Defaults are safe: enrollment is disabled;
the special action, finite TTL, bounded attempts, and administrator role are
all parsed and validated without Spring.

# Verification

## Focused Test

Command:

```powershell
$env:JAVA_HOME='C:\Users\jbeas\scoop\apps\temurin25-jdk\current'
.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=McpPublicationInstallTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result: `BUILD SUCCESS`; 12 tests run with 0 failures, 0 errors, and 0 skipped.

Coverage includes keyed HMAC compatibility, signed provenance tamper rejection, unknown key rejection, active Ed25519 acceptance, revoked Ed25519 rejection, unsigned record rejection, checksum enforcement, trust status, and scope policy gates.

`git diff --check` reported no whitespace errors for the implementation files.

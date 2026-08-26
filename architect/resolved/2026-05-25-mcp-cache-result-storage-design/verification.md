# Verification

## Commands

- `.\mvnw.cmd -pl app/meshingress-server -am test -DskipTests`
  - Result: build success after setting `JAVA_HOME` to the Java 25 install.
- `.\mvnw.cmd -pl app/meshingress-server -am test '-Dtest=dev.mrk.meshingress.mcp.tools.cache.McpCacheManagerTests' '-Dsurefire.failIfNoSpecifiedTests=false'`
  - Result: `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`.
- `.\mvnw.cmd -pl app/meshingress-server -am test`
  - Result: `Tests run: 37, Failures: 0, Errors: 0, Skipped: 0`.
- `.\mvnw.cmd clean package`
  - Result: `BUILD SUCCESS`; produced `app/meshingress-server/target/meshingress.jar`.

## Notes

- The first package attempt exposed Boot repackage drift in reactor libraries and one stale parent lookup in `toolspace/powershell-cli-tool`; those were fixed and verified by the final package run.
- Maven was run with `JAVA_HOME=C:\Users\jbeas\scoop\apps\temurin25-jdk\current` because the default shell JDK did not support release 25.

# Verification

- `.\mvnw.cmd -pl app/meshingress-server -am "-Dtest=McpControllerTests" "-Dsurefire.failIfNoSpecifiedTests=false" test` passed.
- `.\mvnw.cmd -pl app/meshingress-server -am "-DskipTests" package` passed and produced `app/meshingress-server/target/meshingress.jar`.

Full `.\mvnw.cmd -pl app/meshingress-server -am test` was also run. It compiled the new `voicebox` module and registered the Voicebox functions, but failed in `McpPublicationInstallTests.installRejectsUnapprovedFunctionScope` with `-32603` instead of the expected forbidden code. The affected files were already dirty before this conversion and are unrelated to the Voicebox module wiring.

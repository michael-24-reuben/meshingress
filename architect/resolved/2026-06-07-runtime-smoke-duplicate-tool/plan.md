# Plan

## Phase 1: Reproduce

Run the broad smoke command from PAS:

```bash
./mvnw.cmd -pl app/meshingress-server -am "-Dtest=*Smoke*" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Then run the focused runtime smoke command:

```bash
./mvnw.cmd -pl app/meshingress-server -am "-Dtest=ToolRuntimeLoaderSmokeTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

## Phase 2: Diagnose

Compare the runtime smoke fixtures and determine whether both tests load the same sample JAR into a shared registry context without cleanup.

If duplicate detection is doing the right thing in production code, keep it intact and fix only test isolation or fixture naming.

## Phase 3: Verify

The entry can be resolved only after focused and broad smoke commands pass, or after a documented diagnosis proves the failure is unrelated to current code and requires a separate blocker.

# Verification

Ran on Windows PowerShell from the repository root:

```powershell
.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=WorkflowRuntimeTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result: `BUILD SUCCESS` at `2026-07-26T18:47:55-04:00`.

The focused suite covers:

- string regular-expression case routing and named result binding;
- fan-out into a `join` that waits for every inbound branch;
- node-owned retry and error-port execution;
- rejection of object case routing without a JSON Pointer subject; and
- rejection of nodes that cannot be reached from the manual trigger.

The sample was then validated with:

```powershell
.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=WorkflowRuntimeTests,WorkflowSamplesTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result: `BUILD SUCCESS` at `2026-07-26T19:04:38-04:00`; 6 tests passed. The sample is only compiled and validated—the automated test never invokes the volume-changing PowerShell node.

The sample route was validated with a focused controller test and one live invocation on `2026-07-26T19:22:43-04:00`.

- `req-set-system-volume`: completed, exit code `0`, with `{"volumePercent":100,"endpoint":"default-render"}` on stdout.
- `req-greet-alphasunny`: completed with `Hello, Alphasunny!`.
- `req-search-solo-leveling`: completed; Toonverse returned 10 items from 27 matches, including `Solo Leveling`.

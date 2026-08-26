# Implemented Runtime Beta

- Added immutable workflow definition, node, endpoint, edge, input-binding, result-routing, and failure-policy model types under `dev.mrk.meshingress.workflow`.
- Added `WorkflowCompiler` validation for request IDs, result and input references, declared routing contracts, source and target ports, exactly one manual trigger, merge/join topology, acyclic graphs, and trigger reachability.
- Added `WorkflowRuntime` for deterministic in-process execution of manual trigger, tool call, compose, expression-true, merge, and join nodes.
- Routed each tool call through the existing `ToolExecutor` and derived an attempt-specific runtime request ID from the run, definition request ID, and attempt number.
- Implemented ordered string and number predicate cases, direct boolean ports, JSON Pointer bindings, default ports for non-boolean results, and node-local retry/error routing.
- Added `WorkflowSamples.desktopVolumeGreetingAndSoloLevelingSearch()` using `cli.powershell.execute`, `helloworld.greet`, and `toonverse.search` in sequence. Its PowerShell node sets the default Windows render endpoint to 100 percent.
- Added `POST /api/v1/workflows/samples/desktop-volume-greeting-solo-leveling/run` as an explicitly named beta sample runner. The Workflow Studio now invokes it and renders returned named results in the Variables drawer rather than simulating output.

The runtime has no HTTP or MCP workflow endpoint yet. It does not persist definitions or workflow runs.

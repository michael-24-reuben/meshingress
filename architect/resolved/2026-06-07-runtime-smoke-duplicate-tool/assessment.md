# Assessment

The broad smoke failure was caused by test fixture setup, not by runtime-loader unload logic.

`ToolRuntimeLoaderSmokeTestResults` uses the production `RuntimeToolRegistryBridge` and real Spring `ToolRegistry`. With default startup scanning enabled, bundled tool handlers can already be registered in that real registry. The smoke fixture then activates `sample-module-0.0.1-SNAPSHOT-all.jar`, whose sample function is also `helloworld.text`, and strict duplicate detection correctly throws:

```txt
IllegalStateException: Duplicate MCP function descriptor name: helloworld.text
```

The focused runtime smoke test passed because it uses an isolated `RecordingToolRegistrationBridge` rather than the Spring registry. Repository publication install tests already use the production registry with startup scanning disabled for the same reason: runtime-install tests need an empty registry baseline while preserving strict duplicate checks.

Risk level: low. The fix is test-scoped and keeps production duplicate detection unchanged.

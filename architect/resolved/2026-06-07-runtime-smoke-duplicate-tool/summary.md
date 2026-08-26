# Summary

Resolved the broad runtime-loader smoke failure by making `ToolRuntimeLoaderSmokeTestResults` start with an empty tool registry. The failure came from startup-scanned bundled tools colliding with the runtime sample function `helloworld.text`, while strict duplicate detection was behaving correctly. The fix is test-scoped, keeps production duplicate detection intact, and both focused and broad smoke commands now pass.

# Summary

The Windows control center now recognizes the healthy development server that was previously invisible because it was started by Maven rather than by the JAR launcher. Discovery is bounded to the configured listening Java process, the exact Meshingress runtime identity, and an actuator health response, so unrelated Java processes are not adopted.

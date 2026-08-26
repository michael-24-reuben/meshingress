# Summary

Resolved the module-catalog API and Studio icon integration. Active source modules and installed runtime modules now publish the same safe manifest-backed surface, without requiring an artifact repository JAR. The new catalog is intentionally separate from MCP `tools/list`; it provides compact module discovery plus bounded detail, README, and icon resources for UI consumers.

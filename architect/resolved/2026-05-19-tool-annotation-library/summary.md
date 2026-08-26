# Summary

Added `lib/meshingress-tool-annotations`, a shared tool-author library that exposes annotation-based tool metadata and a scanner modeled after the controller dispatch pattern. The scanner can inspect annotated tool classes, normalize function paths, build input schemas from annotated args, and produce `McpToolDescriptor` metadata. Runtime execution remains untouched; server registry/executor integration is left for a later step. Focused Maven verification passed with Java 22 compiler overrides.

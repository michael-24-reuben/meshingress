# Context

The previous resolved record `2026-05-19-tool-annotation-library` intentionally stopped at metadata scanning and did not attach annotations to server execution.

The controller dispatch annotation system under `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/dispatch` is for JSON-RPC method-family controllers, not tool authoring. This bridge should live in the server tool runtime layer and adapt annotated tool beans into the existing `ToolRegistry` and `ToolExecutor` flow.

The scanner must keep `McpToolScopes` as enum-backed scopes such as `McpToolScope.LOCAL_READ`; do not restore old hint fields like `readOnlyHint`.

Local Maven verification may need Java 22 overrides because the root POM currently targets Java 25 while this machine has historically used JDK 22.

# Assessment

The MVP server transport needed a Spring Boot host that delegated to the existing config, client, policy, runtime, and tool modules. The important behavioral constraint was matching the CLI: descriptor listing must not resolve app-password secrets or make HTTP calls, while account tests and tool calls must resolve the authenticated OCS user id before file tools construct WebDAV paths.

The implementation keeps capability logic out of controllers. Controllers only map HTTP and JSON-RPC requests to `NextcloudMcpRuntimeService`, which composes clients, registries, dispatchers, and runtime contexts.

Admin endpoints, WebSocket transport, OCC execution, and persistence-heavy audit history remain outside this slice.

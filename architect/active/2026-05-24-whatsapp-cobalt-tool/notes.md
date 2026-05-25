# Notes

- The tool should not expose `Store#chats()`, `Store#contacts()`, message listeners, or message-read/query helpers in this pass.
- A connected Cobalt client still has underlying access to WhatsApp session data inside the JVM. The boundary here is the public Meshingress MCP surface and default no-history pairing behavior.
- Implemented public functions: `status`, `start_pairing`, `connect_registered`, `send_text`, and `disconnect`.
- The Cobalt web session uses `WebHistorySetting.discard(false)` and `automaticMessageReceipts(false)`.
- Created and pushed backup branch `backup-2026-05-24-123317` at commit `01223a1a77b06352c729c4ee2e3177acbafb636a`.
- Verified compile and test-compile for the attached server reactor with Java 25 using `.\mvnw.cmd -pl app/meshingress-server -am test -DskipTests`.
- Full test execution is currently blocked before this module by `McpToolAnnotationScannerTests`, which fails with a null `McpConfigureMapping` in the pre-existing annotation scanner worktree changes.

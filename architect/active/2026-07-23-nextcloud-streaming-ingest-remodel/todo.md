# Todo

- [x] Generate test-only callers for MCP, reserved-link handoff, legacy prepared-link handoff, prepared direct WebDAV, direct source streaming, and chunked-upload v2.
- [x] Verify all routing callers with `node --check` without issuing a network request.
- [ ] Confirm the owner-approved target: standard WebDAV storage plus Meshingress reader/publisher.
- [ ] Design the streaming publisher contract and configuration migration.
- [ ] Implement direct `PUT` and chunked-upload v2 with durable retry state.
- [ ] Route Toonverse source streams through the publisher.
- [ ] Add storage protocol tests, retry tests, and a real reader render smoke test.
- [ ] Deploy only the replacement path, run a one-chapter live acceptance test, then decide the custom app retirement separately.

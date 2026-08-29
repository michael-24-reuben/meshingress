# Todo

- [x] Generate test-only callers for MCP, reserved-link handoff, legacy prepared-link handoff, prepared direct WebDAV, direct source streaming, and chunked-upload v2.
- [x] Verify all routing callers with `node --check` without issuing a network request.
- [x] ~~Confirm the owner-approved target: standard WebDAV storage plus Meshingress reader/publisher.~~ *(Discontinued: Owner approved and validated the dedicated Nextcloud delegated worker in `2026-07-23-nextcloud-delegated-worker`)*
- [ ] ~~Design the streaming publisher contract and configuration migration.~~ *(Cancelled)*
- [ ] ~~Implement direct `PUT` and chunked-upload v2 with durable retry state.~~ *(Cancelled)*
- [ ] ~~Route Toonverse source streams through the publisher.~~ *(Cancelled)*
- [ ] ~~Add storage protocol tests, retry tests, and a real reader render smoke test.~~ *(Cancelled)*
- [ ] ~~Deploy only the replacement path, run a one-chapter live acceptance test, then decide the custom app retirement separately.~~ *(Cancelled)*

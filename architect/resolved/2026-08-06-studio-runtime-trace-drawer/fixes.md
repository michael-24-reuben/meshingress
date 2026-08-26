# Fixes

- Added the `Runtime` Workflow Studio drawer tab.
- Captured live run, node-start, and node-complete timings in client state, including status, lane, attempts, output type, captured JSON size, and execution duration.
- Added a millisecond timeline, variable/event table, compact tool/search filtering, clear action, and a session-local settings popover.
- Expanded the drawer to at least 42 percent when opening Runtime so the dashboard is usable without manual resizing; a user-selected larger height remains intact.
- Kept Stop disabled with an explicit cancellation-unavailable tooltip because the beta workflow API has no cancel contract.
- Kept Current execution, Variables, and Definition intact.

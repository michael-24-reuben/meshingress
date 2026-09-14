# Plan

1. Import the JSON state catalog and validate every trigger value at the tracker boundary.
2. Normalize scene data and state into a renderer-safe snapshot with explicit `data-*` attributes.
3. Let the dashboard dispatch only tracker triggers and use the snapshot to render the honeycomb preview.
4. Add visual treatments later, after the state semantics are reviewed individually.

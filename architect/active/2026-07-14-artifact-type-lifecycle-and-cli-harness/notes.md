# Active Notes

## 2026-07-14 Assignment Activation

- The user made scope policy the current assignment.
- The MCP authentication and authorization architect remains pending and is last in the current todo order. The former root `ASSIGNMENT.md` and `HANDOFF.md` were copied into that entry as historical snapshots before the root cards changed.
- The active code slice removed unsupported artifact enum values and maps the legacy `GENERATED_TOOL_MODULE` value to `TOOL_MODULE` before enum-backed metadata is deserialized. A migrated publication is re-signed only when the configured signer matches its original key ID and algorithm; otherwise startup stops with a manual-migration error.
- The local file-backed H2 repository was started once with explicit absolute repository/JDBC paths. Its one artifact row and one revoked publication now report `TOOL_MODULE`; the publication remains `local-dev-hmac` / `HmacSHA256` with a 64-character signature.
- The next implementation slice is the scope-decision model: immutable declared/detected observations, revisioned enable/disable decisions, justified manual additions of known scopes, signed publication revisions, and runtime reload.

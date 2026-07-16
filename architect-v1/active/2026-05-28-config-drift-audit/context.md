# Context

## Source Report

- `architect/reports/redundant-code-inspection-2026-05-28-171500.md`

## Declared But Unused (initial inventory)

- `meshingress.tools.default-timeout`
- `meshingress.tools.default-audit`
- `meshingress.tools.default-debug-trace`
- `meshingress.tools.registration.require-approval-for-dynamic-phases`
- `meshingress.security.require-tool-approval`
- `meshingress.security.require-approval-for-privileged`
- `meshingress.security.require-approval-for-critical`
- `meshingress.security.default-deny`
- `meshingress.scopes.audit-required-for-high-risk`
- `meshingress.scopes.explicit-approval-for-critical`
- `meshingress.secrets.allow-env`
- `meshingress.secrets.allow-file`
- `meshingress.secrets.allow-inline`
- `meshingress.secrets.fail-on-missing`

## Relevant Files

- `lib/meshingress-config/src/main/java/dev/mrk/meshingress/config/MeshingressProperties.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/DefaultToolExecutor.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/McpDispatchExecutor.java`

## Notes

- Only `secrets.redactionPlaceholder` is currently used for audit redaction.
- Any enforcement changes should preserve existing default behavior unless explicitly approved.


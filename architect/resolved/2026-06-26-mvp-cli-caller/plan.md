# Plan

1. Choose CLI framework or minimal parser.
2. Wire config loading and account selection.
3. Wire tool registry/runtime.
4. Implement `tools list`.
5. Implement `call` with argument parsing.
6. Implement `accounts test`.
7. Implement `config check`.
8. Add CLI tests or command-level integration tests.

## Acceptance Criteria

- CLI can list tools from the same runtime used by the server.
- CLI can invoke an MVP tool with JSON arguments.
- Config validation failures are readable and secret-safe.
- Account tests do not perform destructive operations.

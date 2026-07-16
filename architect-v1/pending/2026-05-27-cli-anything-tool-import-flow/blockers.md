# Blockers

## BLOCKED: Repository Artifact Implementation Required

Implementation of this architect is blocked by the existing pending architect:

```txt
2026-05-27-meshingress-repository-artifact-implementation
```

The `cli-anything` generated-tool import flow must not be implemented until the todos in that architect are complete.

## Why This Blocks Implementation

This architect relies on repository behavior that should be designed and implemented first:

- artifact upload
- artifact quarantine
- artifact validation/scanning
- artifact review
- artifact trust status assignment
- checksum/signature generation
- immutable publication records
- approved artifact download path
- client/server bridge for installing tools from the repository

Without those primitives, generated tool import would either bypass trust review or require duplicating repository responsibilities inside `meshingress-server`.

## Required Unblock Condition

Unblock only when `2026-05-27-meshingress-repository-artifact-implementation` provides a working path for:

```txt
artifact upload → quarantine → validation/review → approved publication record → verified client/server download
```

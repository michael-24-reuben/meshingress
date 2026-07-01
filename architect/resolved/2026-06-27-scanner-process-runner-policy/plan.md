# Plan

## Phase 1: Inspect Existing Security/Assessment APIs

- Locate scanner result and pipeline classes in `lib/meshingress-artifact-security`.
- Locate repository assessment pipeline wiring in `app/meshingress-repository`.
- Confirm whether any untracked `ScannerFailurePolicy` or scanner pipeline classes already cover part of this contract.

## Phase 2: Add Runner Contract

- Add a command request model with argv, working directory, timeout, environment overrides if needed, and scanner id.
- Add a result model with status, exit code, duration, timeout flag, stdout/stderr, and failure reason.
- Add a runner service that uses `ProcessBuilder` with argv arrays.

## Phase 3: Add Failure Policy Mapping

- Represent scanner failure policy so required scanners can fail closed and optional scanners can produce review evidence without approving trust by accident.
- Keep policy metadata compatible with existing scanner pipeline plan/stage classes.

## Phase 4: Verify

- Add unit tests for success, non-zero exit, missing executable, and timeout.
- Run focused Maven verification for the changed module(s).
- Update PAS and ASSIGNMENT with exact results and remaining work.

## Non-Goals

- Do not wire real external scanner binaries.
- Do not install scanners on the host.
- Do not change runtime installation policy unless an existing test requires consuming the normalized failure result.

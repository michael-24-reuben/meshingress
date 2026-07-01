# Plan

## First Slice

1. Inventory the current `roles/tools/installPublication` cache-copy, activation, registration-save, and failure sequence.
2. Define transaction boundaries and compensating rollback for each state mutation.
3. Add focused failure-injection tests before changing repository transport.
4. Implement the smallest rollback slice and preserve existing signature/checksum/scope gates.

## Later Slices

- Repository API artifact fetch instead of shared filesystem resolution.
- Durable publication registration and restart reconciliation.
- Operational audit and metrics.

## Boundaries

- Do not collapse repository approval with runtime install.
- Do not weaken publication signature, checksum, trust, or scope verification.
- Do not mix scanner execution or publication eligibility policy into runtime transaction handling.

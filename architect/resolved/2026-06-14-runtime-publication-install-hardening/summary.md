# Summary

Runtime publication install hardening is resolved.

The install path now has tested rollback, runtime-cache cleanup, durable publication registration storage, startup reconciliation, repository API artifact-byte fetch, publication delete/deactivation cleanup, partial-activation rollback coverage, and repository publication-record fetch by coordinate. Inline signed publication payloads remain supported, while coordinate-based installs fetch the signed record from the repository API before the existing verifier and installer gates run.

Focused verification passed for the new fetch contract and for the existing MCP publication install suite. Scanner/sandbox policy, publication eligibility, and direct-registration cleanup remain separate follow-up work.

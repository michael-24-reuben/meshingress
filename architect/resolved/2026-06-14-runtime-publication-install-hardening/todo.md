# Todo

- [x] Define repository client contract for downloading approved artifact bytes.
- [x] Define repository client contract for downloading publication records.
- [x] Replace local repository-path resolution with API fetch where configured.
- [x] Persist runtime publication registration records in the durable tool store.
- [x] Add startup reconciliation for runtime cache, publication records, and active tool registrations.
- [x] Add rollback behavior for registration-save failure after runtime activation.
- [x] Add runtime cache cleanup for activation failure and post-activation install failures after cache copy.
- [x] Add cleanup-failure reporting coverage for cache cleanup and deactivation suppression edges.
- [x] Add rollback behavior for later durable persistence failures.
- [x] Add deactivation/revocation behavior for previously installed publications.
- [x] Add tests for restart survival and missing cache file recovery.
- [x] Add repository-fetch checksum mismatch coverage.
- [x] Add rollback-after-partial-activation tests.


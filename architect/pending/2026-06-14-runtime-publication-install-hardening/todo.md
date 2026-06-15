# Todo

- [ ] Define repository client contract for downloading approved artifact bytes and publication records.
- [ ] Replace local repository-path resolution with API fetch where configured.
- [ ] Persist runtime publication registration records in the durable tool store.
- [ ] Add startup reconciliation for runtime cache, publication records, and active tool registrations.
- [ ] Add rollback behavior for failed activation, failed scope validation, failed registry save, and cache copy failure.
- [ ] Add deactivation/revocation behavior for previously installed publications.
- [ ] Add tests for restart survival, missing cache file recovery, checksum mismatch after fetch, and rollback after partial activation.


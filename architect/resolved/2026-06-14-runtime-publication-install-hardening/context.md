# Context

The current runtime install MVP accepts a signed publication record through `roles/tools/installPublication`, verifies signature/checksum/trust/scope policy, copies the artifact to a runtime cache, activates the JAR, then records an active registration. Focused tests cover the gate behavior.

The remaining work is to make this production-shaped: fetch from repository API, persist runtime registrations, survive restart, and make install rollback transactional.


# Summary

Security-management dispatch routes are implemented and recorded as resolved.
They persist profiles and policies durably, require a verified non-development
OIDC principal with tenant-scoped security authority, audit mutations, and
enforce stored function policy consistently for listing and execution. The next
step is production OIDC configuration and a live HTTP/WebSocket JWT smoke test;
the old OIDC migration record remains the place for issuer-specific rollout
work.

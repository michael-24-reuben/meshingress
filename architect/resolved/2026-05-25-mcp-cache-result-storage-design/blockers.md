# Blockers

No hard blocker for the MVP design.

## Decisions Still Needed Before Implementation

- Final package/module location for cache framework classes.
- Whether `ttlMs = 0` means no expiration or use global default.
- Whether global max TTL can override annotation TTL.
- Whether cache-hit metadata should be visible in returned `_meta`.
- Whether cache files should be cleaned only opportunistically or also on startup.
- Whether filesystem cache should be enabled by default in dev only or all modes.

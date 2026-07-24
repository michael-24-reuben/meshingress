### Previous `storage.properties` reference

```properties
meshingress.storage.enabled=true
meshingress.storage.root=${meshingress.cache.location}/storage
meshingress.storage.max-entry-size=256MB
meshingress.storage.max-published-bytes=1GB
meshingress.storage.max-staging-bytes=256MB
meshingress.storage.max-entries=1024
meshingress.storage.max-concurrent-retrievals=32
meshingress.storage.default-ttl=30m
meshingress.storage.max-ttl=24h
meshingress.storage.default-max-requests=1
meshingress.storage.max-requests=50
meshingress.storage.staging-ttl=15m
meshingress.storage.cleanup-interval=5m
meshingress.storage.cleanup-batch-size=100
meshingress.storage.sql.schema=meshingress
meshingress.storage.sql.table-prefix=storage_
meshingress.storage.sql.table.entries=storage_entries
meshingress.storage.sql.table.usage=storage_usage
meshingress.storage.sql.table.events=storage_events
meshingress.storage.sql.initialize-schema=true
```
---

### Proposed `storage.properties` reference

```properties
# ============================================================
# Core storage policy
# ============================================================

meshingress.storage.enabled=true

# Allowed only for Meshingress-managed bytes:
# - local-local
# - local-external
#
# Legacy values delegated-external, local-async-external, and external-external
# are rejected at binding time. They combined separate concerns.
meshingress.storage.lifecycle=local-local

# Applies regardless of staging/publishing location.
meshingress.storage.max-entry-size=256MB


# ============================================================
# Local storage
# ============================================================

meshingress.storage.local.root=${meshingress.cache.location}/storage

# Total local entry count, including staging and locally published entries.
meshingress.storage.local.max-entries=1024


# ------------------------------------------------------------
# Local staging
# ------------------------------------------------------------

meshingress.storage.local.staging.max-bytes=256MB
meshingress.storage.local.staging.ttl=15m


# ------------------------------------------------------------
# Local publishing
# ------------------------------------------------------------

# Used only when lifecycle=local-local.
meshingress.storage.local.published.max-bytes=1GB

# Lease behavior for locally published files.
meshingress.storage.local.published.default-ttl=30m
meshingress.storage.local.published.max-ttl=24h

meshingress.storage.local.published.default-max-requests=1
meshingress.storage.local.published.max-requests=50

meshingress.storage.local.published.max-concurrent-retrievals=32


# ------------------------------------------------------------
# Local cleanup
# ------------------------------------------------------------

# Cleanup applies only to local staging and local publication data.
# External files are never deleted or cleared by Meshingress.
meshingress.storage.local.cleanup.interval=5m
meshingress.storage.local.cleanup.batch-size=100


# ============================================================
# External storage policy
# ============================================================

# Required only for lifecycle=local-external. This is the fixed target for
# LOCAL_BYTES workspaces; tools never choose a raw target or credentials.
meshingress.storage.external.default-target=webdav-primary

# Optional Nextcloud target for tool-requested DELEGATED_SOURCE_URLS workspaces.
# It can coexist with either local lifecycle because it does not use local-byte
# publication or the Meshingress handoff worker.
meshingress.storage.external.delegated-target=nextcloud-primary

# Fixed safety contract:
# - create new content
# - never overwrite existing content
# - never modify uploaded content
# - never delete uploaded content
# - never clear external storage
meshingress.storage.external.access-mode=write-only
meshingress.storage.external.mutation-policy=create-only
meshingress.storage.external.conflict-policy=fail
meshingress.storage.external.retention-policy=provider-managed

# External publication execution limits.
meshingress.storage.external.max-concurrent-uploads=8
meshingress.storage.external.upload-timeout=30m

# A LOCAL_BYTES tool requests QUEUED publication when it needs a durable
# handoff record before returning. A bounded worker claims records, recovers
# expired leases, retries transient failures, and writes manifest.json last.
meshingress.storage.external.async-handoff.worker-interval=5s
meshingress.storage.external.async-handoff.lease-duration=35m
meshingress.storage.external.async-handoff.max-attempts=8
meshingress.storage.external.async-handoff.initial-retry-delay=5s
meshingress.storage.external.async-handoff.max-retry-delay=5m

# Built-in provider coverage is intentionally explicit: this server currently
# ships the WebDAV direct-final adapter for LOCAL_BYTES local-external work and
# the Nextcloud reserved-workspace adapter for DELEGATED_SOURCE_URLS work.


# ============================================================
# External target: Nextcloud / WebDAV
# ============================================================

meshingress.storage.external.targets.nextcloud-primary.provider=webdav
meshingress.storage.external.targets.nextcloud-primary.enabled=true

meshingress.storage.external.targets.nextcloud-primary.endpoint=https://cloud.example.com
meshingress.storage.external.targets.nextcloud-primary.base-path=/remote.php/dav/files/example/Meshingress

# Current WebDAV adapter form: the environment variable contains the full
# HTTP Authorization value (for example, "Basic ..." or "Bearer ...").
# Other reference schemes are deliberately rejected until a secret resolver is wired.
meshingress.storage.external.targets.nextcloud-primary.credential-ref=env:MESHINGRESS_NEXTCLOUD_AUTHORIZATION

# A normal WebDAV PUT writes directly to the destination object.
# This target therefore supports final publication but does not
# necessarily support a safe external staging phase.
meshingress.storage.external.targets.nextcloud-primary.staging-mode=direct-final-only


# ============================================================
# External target: Google Drive
# ============================================================

meshingress.storage.external.targets.google-drive-primary.provider=google-drive
meshingress.storage.external.targets.google-drive-primary.enabled=false

meshingress.storage.external.targets.google-drive-primary.parent-folder-id=GOOGLE_DRIVE_FOLDER_ID
meshingress.storage.external.targets.google-drive-primary.credential-ref=oauth:google-drive-primary

# External staging may use a provider-managed resumable upload session.
meshingress.storage.external.targets.google-drive-primary.staging-mode=provider-session


# ============================================================
# External target: OneDrive
# ============================================================

meshingress.storage.external.targets.onedrive-primary.provider=onedrive
meshingress.storage.external.targets.onedrive-primary.enabled=false

# May alternatively be a concrete drive ID.
meshingress.storage.external.targets.onedrive-primary.drive-id=me
meshingress.storage.external.targets.onedrive-primary.base-path=/Meshingress

meshingress.storage.external.targets.onedrive-primary.credential-ref=oauth:onedrive-primary

# External staging may use a provider-managed upload session.
meshingress.storage.external.targets.onedrive-primary.staging-mode=provider-session


# ============================================================
# Metadata database
# ============================================================

# SQL contains lifecycle metadata, usage accounting, and events.
# It does not contain file bytes.
meshingress.storage.metadata.sql.schema=meshingress
meshingress.storage.metadata.sql.table-prefix=storage_

meshingress.storage.metadata.sql.table.entries=storage_entries
meshingress.storage.metadata.sql.table.usage=storage_usage
meshingress.storage.metadata.sql.table.events=storage_events

meshingress.storage.metadata.sql.initialize-schema=true
```

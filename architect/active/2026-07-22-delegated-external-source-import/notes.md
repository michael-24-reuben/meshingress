# Reconciliation note — 2026-07-23

The shared storage model is now implemented in `architect/resolved/2026-07-23-tool-owned-storage-transfer-model`.

This record's deployed Nextcloud protocol remains valid, but its old `delegated-external` lifecycle terminology is superseded: new callers select `DELEGATED_SOURCE_URLS` in their workspace request, and Nextcloud is configured through `meshingress.storage.external.delegated-target`. The Nextcloud import job remains destination-owned and is not a Meshingress queued handoff.

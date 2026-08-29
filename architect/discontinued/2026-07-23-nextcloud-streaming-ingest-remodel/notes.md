# Reconciliation note — 2026-07-23

`architect/resolved/2026-07-23-tool-owned-storage-transfer-model` removed delegated transfer from the global lifecycle. This record should evaluate streaming ingest as a `DELEGATED_SOURCE_URLS` destination concern; it must not introduce a Meshingress async publication mode for source URLs because Meshingress has no source bytes to hand over.

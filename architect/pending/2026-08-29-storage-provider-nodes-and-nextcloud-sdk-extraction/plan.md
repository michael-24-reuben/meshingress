# Design and Implementation Plan

This is a planning sequence only. Each implementation phase requires the entry to be activated after the open decisions are approved.

1. Finalize the `McpNode` taxonomy and Java annotation shape, including common metadata and kind-exclusive metadata.
2. Define provider discovery/listing names and compatibility behavior for existing `tools/list` clients.
3. Define provider declaration, compatible-contract inference, consumer eligibility, explicit `oneOf` selection, and failure reporting.
4. Define provider constructor/argument binding, resolver boundary, instance lifetime, and scope/blacklist enforcement. Preserve cache/replay boundaries so transport results never substitute for live provider identity.
5. Define the module migration map: Nextcloud behavior into its provider artifact, local storage into a provider module, and only cache plus provider-neutral contracts retained natively.
6. Reconcile the external Nextcloud SDK public API, coordinates, runtime assumptions, and documentation version; choose an adapter/dependency strategy that avoids two competing tool/runtime SPIs.
7. Create an implementation record with source-level sequencing, compatibility tests, migration steps, and rollback strategy. Activate that record before coding.

# Storage Lifecycle and Write-Only Handoff

Extend the existing ephemeral tool-workspace storage capability so it can either retain content locally or hand it off to a foreign storage provider without later read, mutation, deletion, cleanup, or ownership of that foreign content.

The new model must support exactly these lifecycle modes:

- `local-local`: stage and publish locally; retain the current temporary download and retrieval behavior.
- `local-external`: stage locally, then create the final foreign object once and retain only local metadata/audit evidence after a successful handoff.
- `external-external`: write through an external provider-managed staging/upload session and retain only local metadata/audit evidence after finalization.

`external-local` is invalid because Meshingress must not retrieve content from a foreign write-only destination.

The feature must define a complete file-operation API. Each backend declares which operations it allows; foreign write-only targets allow creation and upload-session writes only, while local storage can expose reads and cleanup. Creation must be create-only for every backend: never overwrite an existing completed object.

This is a planning record. It does not authorize implementation.

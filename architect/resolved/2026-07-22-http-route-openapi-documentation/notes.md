# Notes

No route behavior, authorization rules, or response status codes should change in this documentation-only slice.

## Outcome

`ArtifactController` now documents every public operation, its lifecycle intent, normal response, common repository failures, and the repository-header role contract. `StorageController` now documents file download and metadata retrieval, including `416` for a supplied `Range` header. The generated contract is verified through Springdoc rather than a separate export plugin.

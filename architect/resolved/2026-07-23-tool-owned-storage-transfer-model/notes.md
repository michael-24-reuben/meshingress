# Reopened regression — 2026-07-23

`@ConditionalOnProperty` considers a blank property present. A deployment that explicitly set `meshingress.storage.external.delegated-target=` therefore created `delegatedViewerService`, where the validated policy correctly had no target and the method threw `No delegated-source target is configured.`

Replace that condition on both the factory and viewer route with a nonblank-target condition. Keep the policy validation intact for nonblank target names.

## Reopened binding regression — 2026-07-23

The blank-property guard was correct but did not explain a named target still failing in `delegatedViewerService`. The condition reads the raw environment value, whereas `StorageLifecyclePolicy` reads `MeshingressProperties`. Adding a backwards-compatible overload to the immutable `Storage.External` record left binding ambiguous, so Spring constructed the object without the new `delegatedTarget` value. Mark the canonical record constructor with `@ConstructorBinding` and prove binding directly from the property key.

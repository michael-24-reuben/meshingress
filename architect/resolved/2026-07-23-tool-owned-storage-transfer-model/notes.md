# Reopened regression — 2026-07-23

`@ConditionalOnProperty` considers a blank property present. A deployment that explicitly set `meshingress.storage.external.delegated-target=` therefore created `delegatedViewerService`, where the validated policy correctly had no target and the method threw `No delegated-source target is configured.`

Replace that condition on both the factory and viewer route with a nonblank-target condition. Keep the policy validation intact for nonblank target names.

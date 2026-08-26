# Assessment

Nextcloud Flow was not the correct owner for arbitrary source URL imports. A dedicated `meshingress` custom app with authenticated OCS routes, durable queued jobs, Nextcloud's HTTP client, and the filesystem Node API provides the intended workspace operation.

The verified workspace is `Workspace/Meshingress/`. Downloads are owned by stable tool identity under `storage/<tool-id>/`; no session or request ID contributes to the user-visible path.

The remote target runs Nextcloud 34.0.0.12. The app intentionally declares an exact 34 support range until a later major version is tested.

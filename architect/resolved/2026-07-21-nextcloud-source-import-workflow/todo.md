# Todo

- [ ] Confirm the target Nextcloud user and `Workspace/` parent selected for the managed `Meshingress/` root.
- [ ] Confirm the displayed directory casing and the initial directory list: `storage`, `docs`, `generated`, `repository/artifacts`, `repository/quarantine`, `repository/vendor`, and `var/logs`.
- [ ] Confirm the initial accepted URL class: public HTTPS and short-lived signed HTTPS URLs only, or a broader credential model.
- [ ] Define collision/idempotency behavior for an existing destination path.
- [x] Build and install the `meshingress` Nextcloud workspace app, OCS routes, migration, and queued source-import job.
- [x] Add an SSH/Docker deployment helper that uploads a fresh app directory, validates it, enables it, and reports OCS routes without embedding credentials.
- [ ] Build and test the Meshingress workspace client, including per-tool identity propagation.
- [ ] Remove `EXTERNAL_EXTERNAL` from the lifecycle/configuration model without changing the existing local WebDAV handoff.
- [ ] Define and implement a new Meshingress storage lifecycle for link-producing tools. It must hand source links and stable tool identity to the Nextcloud app, then report terminal import metadata; it is not a replacement name for `EXTERNAL_EXTERNAL`.
- [ ] Perform a controlled end-to-end import and verify the returned metadata matches the visible Nextcloud file. Blocked by the selected workspace parent and valid user credential.

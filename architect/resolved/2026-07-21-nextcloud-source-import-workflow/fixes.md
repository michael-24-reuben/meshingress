# Implemented

- Added the `meshingress` Nextcloud custom app with OCS workspace initialization, source-import submission, and status routes.
- Added durable import records, queued background processing, retry handling, strict HTTPS/tool/path validation, and returned file metadata.
- Created the managed `Workspace/Meshingress/{storage,docs,generated,repository/{artifacts,quarantine,vendor},var/logs}` layout.
- Fixed Nextcloud 34 compatibility issues found by live validation: database cursor closing, null query binding, background-job argument decoding, and string HTTP response bodies.
- Added `install-nextcloud-app.js`, an SSH/Docker install helper that stages a fresh app, validates PHP, enables it through `occ`, reports OCS routes, and refuses overwrite.
- Preserved `EXTERNAL_EXTERNAL` removal and the later link-source lifecycle as separate Meshingress-server follow-up work.

## 2026-07-21 delegated-import repair

- Replaced the deployed Nextcloud 34-incompatible `ResultAdapter::free()` call with `closeCursor()`.
- Updated nullable job-field writes so `WorkspaceService::update()` emits SQL `NULL` rather than treating `NULL` as a quoted MySQL identifier.
- Added the installer’s explicit `--replace-existing` mode: it validates a staged replacement before swapping the installed app and keeps the existing directory until that validation succeeds.
- Added the registered `meshingress:source-import:work` command. It invokes the app's own `runNextQueuedImport()` path directly instead of depending on the Nextcloud background-job scheduler to select the app's `QueuedJob`.
- Added `--watch --interval=<seconds>` to that command. The live worker runs with `--interval=1`; it performs one lightweight queued-record lookup per idle second and processes a job immediately when one appears.
- Replaced the minute-based class-specific cron entry with one persistent boot-started worker. The account cannot enable systemd linger without administrator authentication, so the user crontab owns the portable `@reboot` process and restarts it after Docker/Nextcloud interruptions.
- Extended `install-nextcloud-app.js` to install or replace only its marked `@reboot` worker entry, restart the recorded worker after successful deployment, and verify the new worker and cron entry. Unrelated user cron entries and standard Nextcloud background-job cron remain untouched.

# Verification

- Remote server: Nextcloud 34.0.0.12, writable `custom_apps`, app enabled as `meshingress 0.1.0`.
- `occ router:list meshingress --ocs` listed all three OCS routes.
- PHP syntax checks passed for every app PHP file.
- Workspace initialization as `ops` returned OCS 200 and created the managed directory layout.
- A real HTTPS import completed as tool `meshingress.verify` at `storage/meshingress.verify/verification/example-4.html`.
- OCS status returned `COMPLETED`, file ID `1081`, 559 bytes, MIME type, ETag, and SHA-256 `ff67a9d764d6a2367a187734e697f6a53217db9a21c101d410a113ca871a299d`.
- Authenticated WebDAV GET returned 200, 559 bytes, and the same SHA-256.
- After `apache2ctl -k graceful` in the Nextcloud container, the live OCS status route returned the completed metadata.
- The installer passed Node syntax/help/missing-configuration checks and a protected remote dry run that rejected overwrite and preserved the live app.

## 2026-07-21 delegated-import repair

- Local `php -l` passed for every app PHP file; `node --check` and `--help` passed for the replacement-capable installer.
- Live guarded replacement passed PHP lint for the staged app, retained the OCS routes, and left `meshingress 0.2.0` enabled.
- Live source verification confirmed `closeCursor()` and the SQL-NULL update branch are installed; Apache received a graceful reload.
- `php -l integrations/nextcloud/meshingress/lib/Command/SourceImportWorkerCommand.php` passed locally; the guarded live replacement also linted the command successfully.
- `occ list --raw` exposes `meshingress:source-import:work`; its help lists `--watch` and `--interval`, and invalid `--interval=0` returns the expected validation error.
- The previously queued test jobs completed through the direct command. The live persistent process is running the command with `--watch --interval=1`, and the account crontab starts the same process at boot while retaining ordinary Nextcloud cron every five minutes.
- A live `install-nextcloud-app.js --replace-existing` run successfully reconfigured the worker. It left exactly one marked `@reboot` worker entry, preserved the five-minute ordinary Nextcloud cron entry, wrote the new worker PID, and verified one live one-second worker child process.

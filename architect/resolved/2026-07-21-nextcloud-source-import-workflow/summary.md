# Summary

Meshingress now has a deployed Nextcloud 34 workspace app for server-side source imports. It accepts a minimal URL/path request plus stable tool identity, writes inside the official `Workspace/Meshingress/storage/<tool-id>/` namespace, and reports final downloaded-file metadata.

The app is installed on the intended remote Nextcloud instance and has completed an end-to-end import with matching OCS and WebDAV metadata. The deployment helper supports future fresh installs without embedding host credentials.

Future work is intentionally separate: add the Meshingress-server lifecycle for link-producing tools, submit source-import jobs to this app, report terminal metadata, and remove `EXTERNAL_EXTERNAL` without changing the existing local byte-owner WebDAV handoff.

The delegated-import worker repair is now deployed. Imports no longer remain queued because of the missing scheduler, stale cursor API, invalid nullable update, or generic-queue starvation; the app exposes a dedicated worker command that stays alive and checks for a queued import once per second. The host runs ordinary Nextcloud cron every five minutes and starts that dedicated worker at boot. The guarded replacement installer now creates or updates that worker itself, so future installations require no separate manual cron setup.

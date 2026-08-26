# Meshingress Workspace App and Source Import

Create a general-purpose Nextcloud app named `meshingress`, rather than a download-only `meshingress_import` app. Its first operation replaces the local download-then-WebDAV-upload path for link-producing Meshingress tools with a Nextcloud-owned asynchronous import.

The app manages one selected Nextcloud workspace: `Workspace/Meshingress/`. It owns the directory layout inside that root; tools identify themselves when requesting work, and source downloads are stored under the stable official `storage/<tool-id>/` namespace—not under ephemeral session or request IDs. The app queues the request, fetches through Nextcloud's supported HTTP client, creates required managed directories, writes through the Nextcloud Files API, and exposes final metadata. The submit request must not wait for the download.

This is a planning record. It does not authorize implementation in Meshingress or the Nextcloud instance.

## In Scope

- Selecting the correct Nextcloud extension mechanism.
- The managed `Workspace/Meshingress/` namespace and initial directory contract.
- The minimal submit and status API contracts, including source-tool identity.
- Job ownership, file-write path, durable status, and result metadata.
- The Meshingress workspace-operation boundary needed to consume the workflow.
- Security and failure boundaries for user-provided URLs and paths.

## Out of Scope

- Installing or enabling a Nextcloud app.
- Changing the live Nextcloud configuration, credentials, or external mounts.
- Implementing the Meshingress adapter or changing existing local/WebDAV handoffs.
- Generic support for every external storage provider.

# Summary

Ephemeral storage is now a tool-output workspace, not a generic token blob. Tools use the `meshingress-tool-api` workspace service to stage named output files and publish them as `/storage/{sessionId}/{requestId}/files/`. The server keeps the authoritative metadata, generates `files/manifest.json`, applies workspace-level expiry/request/quota cleanup, and passed focused verification.

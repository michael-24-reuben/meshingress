# Delegated External Source-Import PRD

## Goal

Allow a link-producing Meshingress tool to publish a mixed workspace to Nextcloud without downloading delegated media into Meshingress local storage.

## Contract

The feature is enabled only by:

```properties
meshingress.storage.lifecycle=delegated-external
```

An eligible tool follows this sequence:

1. Open a storage workspace; Meshingress reserves an OPEN Nextcloud workspace.
2. Write tool-generated native files into that reservation.
3. Register each HTTPS source URL and validated relative output path.
4. Publish once; Meshingress seals the reservation and Nextcloud queues one import job.
5. Read publication status until the remote job is terminal.

## State model

```text
OPEN -> SEALED -> QUEUED -> RUNNING -> COMPLETED
                     \-> FAILED
```

OPEN accepts native files and source batches. SEALED is immutable and may create only one job. COMPLETED returns the Nextcloud manifest and files URI. FAILED preserves a safe failure detail and does not expose a partial success manifest.

## Non-goals

- Do not make `local-async-external` download or delegate URLs differently.
- Do not create one remote job per source.
- Do not allow caller-selected users, arbitrary workspace roots, or direct mutation outside the tool-owned Nextcloud namespace.
- Do not make Meshingress retrieve delegated media after publication.

## Acceptance criteria

- Toonverse registers image URLs without opening local media streams under `delegated-external`.
- Native `book.json` and chapter descriptors are present in the same final Nextcloud manifest.
- A second seal, path collision, non-owner request, or post-seal mutation is rejected.
- Local storage bytes remain bounded to native files; delegated image bytes are owned by Nextcloud.

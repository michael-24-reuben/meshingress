# Product Requirements

## Objective

Allow one Meshingress server to support tools that transfer owned local bytes and tools that delegate source URLs, without changing a global lifecycle property between calls.

## Required behavior

1. The property `meshingress.storage.lifecycle` accepts `local-local` or `local-external` only and describes Meshingress-managed bytes.
2. A tool method explicitly requests either local-byte transfer or delegated-source transfer.
3. Local-byte transfer follows the configured lifecycle. Under `local-external`, a tool may use inline or durable queued handoff only when that destination supports it.
4. Delegated-source transfer opens and seals a destination-owned workspace. The destination's status is exposed as delegated-import status; Meshingress does not enqueue a local upload for delegated source bytes.
5. A qBittorrent-style tool can retain control of its download job, then collect completed local files through the local-byte path.
6. Existing stable workspace identity, tool identity, ownership, relative-path validation, quota, manifest, and retry controls remain enforced.

## Non-goals

- Do not expose destination credentials or arbitrary target selection to tool callers.
- Do not allow `DELEGATED_SOURCE_URLS` to use Meshingress queued publication.
- Do not redefine a destination-owned import job as a Meshingress handoff job.
- Do not deploy or alter the current Nextcloud application as part of this planning record.

## Acceptance criteria

- One test context can open a local-byte workspace and a delegated-source workspace without changing `meshingress.storage.lifecycle`.
- Local-byte queued handoff creates and processes a Meshingress-owned durable handoff job.
- Delegated-source seal returns destination-owned status and creates no Meshingress local-byte handoff job.
- Toonverse and a qBittorrent fixture demonstrate their respective transfer paths.
- Legacy lifecycle values have explicit migration or rejection behavior and no longer silently select a conflicting global storage service.

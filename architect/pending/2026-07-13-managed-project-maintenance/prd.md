# Product Requirements: Managed Project Maintenance

## Command Modes

| Mode | Default mutability | Responsibility |
|---|---|---|
| `-Check` | Read-only | Inspect environment, code/build health, dependency resolution, configured managed assets, and stored-tool availability. This is the default. |
| `-Build` | Build output only | Run the declared Maven build and report the result. Maven resolves declared dependencies and prepares test-only agents. |
| `-ReportCode` | Read-only | Produce code-quality candidates such as redundancy, suspicious error handling, and debug leftovers. Findings are advisory, never automatic edits. |
| `-RepairAssets` | Manifest-owned paths only | Restore only missing or checksum-mismatched wrapped vendors recorded in the lock manifest. |
| `-UpgradePlan` | Read-only | Report possible vendor or dependency upgrades; it must not download, install, or modify version declarations. |

## Wrapped Vendor Lock Manifest

Create a committed manifest in a later implementation slice, proposed path: `maintenance/vendor-lock.json`.

Each record must include:

```json
{
  "id": "vendor-tool-runtime",
  "wrapper": "toolspace/example-tool",
  "version": "1.4.2",
  "source": {
    "uri": "https://approved.example/vendor-tool-runtime-1.4.2.zip",
    "publisher": "example",
    "repository": "https://github.com/example/vendor-tool-runtime",
    "revision": "v1.4.2"
  },
  "artifact": {
    "fileName": "vendor-tool-runtime-1.4.2.zip",
    "sha256": "required lowercase hexadecimal digest",
    "sizeBytes": 0,
    "format": "zip"
  },
  "compatibility": {
    "os": ["windows"],
    "architecture": ["x64"],
    "runtime": "java-25"
  },
  "license": "SPDX identifier or declared license reference",
  "installPath": "tools/.managed/vendor-tool-runtime/1.4.2",
  "ownership": "maintenance-managed"
}
```

Rules:

1. A record uses one immutable version, source URI, filename, and SHA-256 digest. Version ranges and `latest` are forbidden.
2. `installPath` must be project-relative and under a dedicated manifest-owned directory such as `tools/.managed/`; it cannot target user-managed tool storage, `repository/`, or runtime-data paths.
3. `-RepairAssets` downloads to a staging directory, verifies filename, size when provided, and SHA-256 before an atomic replacement of the manifest-owned path.
4. A vendor update is a reviewed Git change to the lock manifest and any applicable wrapper compatibility tests. Maintenance never edits the lock manifest.
5. A failed validation preserves the prior installed asset and reports the exact vendor ID, expected version, source, checksum, and failure reason.
6. Maven artifacts are not duplicated in this manifest unless they are intentionally wrapped as non-Maven runtime assets.

## Outputs

- Human-readable maintenance report.
- Machine-readable summary suitable for future automation.
- Non-zero exit status when a required check, build, or explicit repair fails.
- No secrets, authorization headers, or private URLs in reports.

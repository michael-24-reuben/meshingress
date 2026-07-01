# Context

`fake-scanner` has been removed from the repository assessment path. Current assessment relies on `cyclonedx-sbom` and `bytecode-scope-scanner`. Those are useful, but they do not detect malware behavior, malicious payloads, suspicious native files, or dependency-level vulnerability information.

This entry groups scanner plugin orchestration, sandbox analysis, and dependency-aware SBOM enrichment because they are all assessment pipeline concerns and should feed the same assessment summary, raw-report storage, trust status, and approval policy.


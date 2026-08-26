# Todo

- [x] Define scanner pipeline interfaces and configuration model.
- [x] Decide required vs optional scanners for the first production-like repository flow.
- [x] Add configured scanner timeout and failure-policy metadata.
- [x] Add raw-report retention policy for scanner and sandbox outputs beyond the current persisted assessment files.
- [x] Select a first malware/sandbox strategy for JAR artifacts.
- [x] Ensure sandbox execution cannot access host secrets, unrestricted network, or repository internals.
- [x] Extend SBOM output from JAR-entry inventory toward dependency-aware metadata.
- [x] Store raw sandbox and scanner reports under repository assessment storage.
- [x] Add tests proving blocked scanner results prevent approval or publication.


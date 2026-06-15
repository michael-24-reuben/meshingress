# Todo

- [ ] Define scanner pipeline interfaces and configuration model.
- [ ] Decide required vs optional scanners for the first production-like repository flow.
- [ ] Add scanner timeout, failure, and raw-report retention policy.
- [ ] Select a first malware/sandbox strategy for JAR artifacts.
- [ ] Ensure sandbox execution cannot access host secrets, unrestricted network, or repository internals.
- [ ] Extend SBOM output from JAR-entry inventory toward dependency-aware metadata.
- [ ] Store raw sandbox and scanner reports under repository assessment storage.
- [ ] Add tests proving blocked scanner results prevent approval or publication.


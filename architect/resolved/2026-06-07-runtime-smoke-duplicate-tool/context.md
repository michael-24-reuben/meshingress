# Context

This entry exists because chapter 2 needs a trustworthy verification baseline before embedded assessment enrichment continues.

`architect/PAS.md` currently records:

- scanner tests passed;
- repository flow tests passed;
- focused runtime smoke passed;
- broad server smoke failed with duplicate MCP function descriptor `helloworld.text`.

The active chapter 2 entry, `2026-06-05-embedded-assessment-enrichment`, should remain focused on embedded SBOM and scanner enrichment. This smoke-baseline entry isolates the test/runtime failure so the SBOM work does not inherit ambiguous smoke results.

Related records:

- `architect/active/2026-06-05-embedded-assessment-enrichment`
- `architect/resolved/2026-05-26-tool-runtime-loader`

# Summary

Direct experimental local-JAR registration now verifies supplied SHA-256 checksums before runtime activation. The bundle registration path already performed checksum verification, so the implementation brought the experimental path into line with that existing behavior. Focused MCP sample coverage now confirms checksum mismatch rejection through `roles/tools/register`. Direct registration remains a development/smoke-test surface; repository-backed publication installation remains the production provenance path.

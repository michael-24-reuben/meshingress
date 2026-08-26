# Plan

1. Generate a deterministic opaque public ID for each catalog entry and resolve public requests through that ID.
2. Replace public catalog response and resource URL use of raw IDs with `toolId`.
3. Fetch the module catalog only after successful MCP `tools/list`, and pass catalog summaries directly into the Explorer tree.
4. Verify server response behavior, runtime/classpath prefixes, and the Studio build.

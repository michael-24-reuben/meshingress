# Assessment

The public catalog exposed raw implementation identities such as `classpath:<manifest-class>`. Those identifiers leaked classpath structure and forced Studio icon selection to be copied prematurely onto MCP function records.

A full SHA-256 digest of the internal module key gives each active entry a stable opaque identity. The source prefix makes the catalog family visible without exposing the key: `cp-` for classpath modules and `rt-` for runtime modules. Lookup is catalog-bound, so an ID only resolves while its entry is active.

# Context

The first catalog version exposed `classpath:<manifest-class>` as its public `id` and the Studio fetched catalog data concurrently with `tools/list`, copying icons onto function records by namespace.

The new public identifier must not reveal the raw classpath or runtime module key. It must still resolve deterministically while the server has catalog entries. Classpath entries use `cp-`; runtime entries use `rt-`. The digest is one-way rather than encryption, and the catalog resolves a public ID by matching it to its active server-owned entry.

`WorkflowNode` currently already has a `toolId` field for its existing workflow identity. Deciding how catalog `toolId` participates in persisted workflow nodes is explicitly deferred to the next conversation.

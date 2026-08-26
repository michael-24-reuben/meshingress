# Tool Family Contributions and Persisted Function Precedence

Replace the current loose manifest `toolId` with `ToolModuleMetadata`, whose canonical `namespace` is exactly one delimiter-free name, such as `youtube`.

Multiple artifacts may contribute functions to the same tool family. A contribution must never replace a higher-precedence contribution's function. The server must resolve ownership from persisted registry precedence rather than classpath or discovery order, log skipped duplicate functions, and retain each non-conflicting function from lower-precedence contributions.

This entry is planning only. It does not change the manifest API, registration store, runtime loading, Studio UI, or existing YouTube behavior.

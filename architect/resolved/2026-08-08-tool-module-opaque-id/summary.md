# Summary

Resolved the public opaque module-ID and Explorer data-flow follow-on. Tool modules now publish `toolId` rather than their raw server module key, and Studio obtains module data only after MCP discovery has completed. Catalog resource links are resolved against the configured API origin, so a separately hosted Studio displays manifest SVG icons correctly. The next slice may define how this catalog identity should be used when constructing or persisting workflow nodes.

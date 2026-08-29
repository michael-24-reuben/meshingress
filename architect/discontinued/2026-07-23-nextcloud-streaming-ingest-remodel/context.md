# Context

The current path succeeds at creating durable Nextcloud files and inline descriptor viewing, but it has not proven that a completed Open Ink book renders. It splits downloading, retries, queue state, and rendering access across Meshingress and a custom Nextcloud app.

The replacement should use standard Nextcloud WebDAV as the durable storage protocol. It must retain one clear owner for each concern: Meshingress downloads and retries; Nextcloud stores and serves files; the reader renders Open Ink content.


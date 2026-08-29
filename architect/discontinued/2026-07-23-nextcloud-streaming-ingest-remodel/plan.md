# Routing Caller Slice

1. Keep the existing production routes visible as test callers: MCP download, current reserved workspace plus HTTPS links, and legacy DAV preparation plus legacy link handoff.
2. Add the two candidate replacement routes without wiring runtime production behavior: direct source-stream `PUT` and Nextcloud chunk-upload v2.
3. Require explicit environment values and unique roots; do not embed credentials or execute a live call during syntax verification.
4. Use the callers to select the replacement transport before Java lifecycle/configuration changes begin.


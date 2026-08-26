# Plan

1. Attach the `toolspace/youtube` module and auto-configuration to the existing server bundle.
2. Expose a truthful catalog of available foundation functions and planned function IDs.
3. Preserve provider boundaries for official Data, Analytics, Reporting, transcript, AI, and local media responsibilities.
4. Add adapter contracts without selecting a client library, credential store, OAuth flow, or upstream transcript provider.
5. Verify module unit tests and server MCP discovery.

## Deferred implementation order

1. Define typed read-only Data API request/response contracts, quota behavior, and API-key versus OAuth policy.
2. Add OAuth credential ownership, scopes, refresh-token storage, consent callbacks, and audit rules before any authorized feature.
3. Implement creator writes, uploads, moderation, captions, and live features only after destructive-action policy and confirmation requirements exist.
4. Select and review a transcript provider independently of the official Data API.
5. Add analytics and reporting request models, then AI composition only over acquired content.

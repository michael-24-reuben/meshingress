# Brief

Prepare the missing durable and server-side components required to expose
privileged security-management MCP dispatch methods safely. This includes
verified principal integration, Aegis profile persistence, a stored tool-policy
domain, authorization, revision-safe route DTOs, audit events, and tests.

The existing Aegis profile lifecycle is reusable core only. This record does
not authorize route implementation, Spring wiring, raw credential handling, or
production identity-provider configuration.

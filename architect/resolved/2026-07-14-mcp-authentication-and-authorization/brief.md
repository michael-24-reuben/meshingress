# MCP Authentication and Authorization Architecture

## Goal

Design a secure, low-friction authentication and authorization architecture for Meshingress MCP clients, tool calls, administration, and tool-to-upstream credentials.

## Desired Outcome

Meshingress has a reviewed design that separates caller identity from backend credentials, applies authorization consistently to both discovery and invocation, and supports common credential patterns without repeatedly prompting users or exposing secrets to MCP clients.

## Non-Goals

- Do not implement OAuth, OIDC, token storage, token exchange, or new policy code in this entry.
- Do not remove the current development-only headers or admin-token behavior until a migration and compatibility decision is approved.
- Do not persist client access tokens, upstream refresh tokens, API keys, or other secrets in `architect/` files, logs, or test fixtures.
- Do not adopt ToolHive code or assume its Kubernetes/container runtime model applies to Meshingress.

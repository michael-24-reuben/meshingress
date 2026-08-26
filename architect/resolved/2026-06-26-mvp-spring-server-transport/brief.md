# Brief

## Objective

Expose the MVP MCP runtime through a Spring Boot server without moving capability logic into controllers.

## Scope

- Spring application module.
- Properties binding.
- Client/runtime/tool wiring.
- Health endpoint.
- Tools list endpoint.
- Tool call endpoint.
- Optional MCP JSON-RPC shape.
- Audit service wiring if MVP audit requires it.

## Non-Goals

- WebSocket transport unless explicitly pulled into MVP.
- Admin controller.
- Persistence-heavy audit history unless a future slice requires it.

## Resolution Position

Resolve after CLI and tool runtime are stable, then hand off to final MVP verification.

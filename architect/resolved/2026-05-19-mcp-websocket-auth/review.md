# Issue Review

## Summary

The review concluded that the MCP WebSocket authentication design is implementable and should fit the current architecture if it remains a thin transport/authentication layer.

The core recommendation is to authenticate during the WebSocket handshake, then delegate all JSON-RPC payloads to the existing MCP dispatcher using `McpCallContext`.

## Architectural Decision

The WebSocket endpoint must preserve the existing HTTP MCP flow.

```txt
WebSocket text frame
  -> JsonNode
  -> McpCallContext
  -> McpDispatcher#dispatch(JsonNode request, McpCallContext context)
  -> optional JsonNode response
  -> WebSocket text frame
```

## Required Reuse

- Existing `McpDispatcher`
- Existing `McpCallContext`
- Existing MCP method controllers
- Existing role/permission behavior
- Existing HTTP MCP controller behavior as semantic reference

## Transport Additions

- `/mcp/ws`
- handshake authentication interceptor
- credential validator
- WebSocket handler
- WebSocket configuration
- `SecurityConfig` permit rule for upgrade route

## Test Focus

- handshake success/failure
- missing/invalid/expired/revoked credentials
- single request response
- notification no-reply behavior
- batch response behavior
- context propagation
- role/admin parity with HTTP MCP route

## Explicit Non-Implementation

The earlier sample `McpService` remains excluded. It must not be introduced into the codebase.

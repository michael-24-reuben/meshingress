# Plan

1. Define framework-free tool-API value types: `McpPrincipal`, request IDs,
   declared client metadata, call lineage, and execution control. Decide and
   document the source/binary compatibility migration for the existing public
   `McpCallContext` record before changing its components.
2. Introduce a server-only transport request/evidence type and a single context
   factory. Migrate HTTP MCP, MCP WebSocket handshake/message handling,
   workflow HTTP, and workflow WebSocket away from direct public-context
   construction.
3. Refactor access policy into evidence verification plus principal-based
   authorization. Keep a narrowly configured, observable legacy development
   adapter only if migration compatibility requires one; it must create a
   principal rather than leak a role header.
4. Add `ToolAccessService` and a typed decision/result model. Route public
   `tools/list`, `tools/call`, and the execution path through it; retain
   role-admin inventory as a separate inspection surface.
5. Separate availability into scan metadata, list-time policy input, and
   call-time policy input. Make dynamic argument policies fail closed before
   handler invocation.
6. Make `McpDispatchExecutor` calculate and attach execution control before
   invoking a handler. Preserve WebSocket progress support through that control
   rather than identity context mutation.
7. Add parent-to-child workflow context derivation and audit fields based on
   principal subject plus typed lineage, never authorization evidence.
8. Migrate tests and external tool-module examples, remove deprecated raw
   context accessors only after the compatibility window, and run focused plus
   reactor verification.

## Implementation Order

The verified-principal/context-factory slice comes first. Tool eligibility and
availability must follow before any actual OIDC/Spring/Aegis adapter is wired;
otherwise a verified identity would still have inconsistent list/call effects.

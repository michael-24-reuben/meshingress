# Design and Implementation Plan

## Phase 0: Confirm Delivery Decisions

- [ ] Select the first Coolify deployment type and host environment.
- [ ] Define the execution image contents and the approved child-process
  families that are meant to contribute to container telemetry.
- [ ] Set initial CPU, memory, storage, process-count, and network limits.
- [ ] Define retention, sample interval, and the local-only/privacy policy.

## Phase 1: Container Delivery Contract

- [ ] Add an approved Meshingress image build and deployment configuration in
  the project-approved infrastructure location.
- [ ] Configure an application health endpoint suitable for Coolify proxy
  routing.
- [ ] Prove image startup, health transition, resource limits, rollback, and
  non-exposure of private internal service ports.
- [ ] Record what Coolify operational metrics are available for the selected
  deployment type and where their limits differ from Studio telemetry.

## Phase 2: Aggregate Telemetry Contract

- [ ] Define the stable sample JSON schema, units, time semantics, missing
  sample behavior, and versioning policy.
- [ ] Implement one central, read-only sampler for aggregate container and
  JVM values only.
- [ ] Attach run lifecycle only at a central workflow lifecycle seam; do not
  scatter tracking logic throughout tools, controllers, or request payloads.
- [ ] Define lifecycle cleanup, backpressure, capped buffers, authorization,
  and collector failure behavior.

## Phase 3: Native MCP Resource Delivery

- [ ] Advertise `resources` capability, resource template, read, subscribe,
  and unsubscribe support through the MCP dispatcher.
- [ ] Use a run-scoped URI and resource-updated notification flow rather than
  a tool call or client polling loop.
- [ ] Verify transport authentication, reconnect behavior, and that resource
  responses contain only the approved aggregate schema.

## Phase 4: Studio Runtime Overlay

- [ ] Align workflow events and telemetry to a server-owned run-relative
  timeline.
- [ ] Add series colors, legend, settings filters, shared cursor tooltip,
  loading/empty/gap states, and the execution-container scope label.
- [ ] Keep Runtime traces session-local unless a later retention feature is
  expressly approved.

## Phase 5: Platform Portability and Replacement Follow-Up

- [ ] Define an adapter boundary so Cloudflare Containers or another platform
  can feed the same aggregate sample schema later.
- [ ] Reopen or create a focused successor for strict replacement only when
  blue/green, drain, durable state, and WebSocket recovery implementation is
  approved.

## Source Areas to Assess Before Editing

| Area | Likely role |
|---|---|
| `app/meshingress-server/.../workflow/WorkflowRuntime.java` | Central run lifecycle seam. |
| `app/meshingress-server/.../workflow/WorkflowRunListener.java` | Existing start/node/end listener contract. |
| `app/meshingress-server/.../controller/internal/InternalMcpController.java` | MCP capability declaration. |
| `app/meshingress-server/.../controller/McpDispatcher.java` | Native resource-method dispatch. |
| `app/meshingress-server/.../mcp/McpWebSocketHandler.java` | Subscription transport and reconnect behavior. |
| `app/meshingress-studio-web/.../WorkflowDrawer.tsx` | Runtime settings and overlay view. |
| `app/meshingress-studio-web/.../WorkflowStudioPage.tsx` | Runtime trace ownership and run association. |
| future deployment directory | Approved Docker/Coolify delivery artifacts; none exists yet. |

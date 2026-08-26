# PRD: Coolify Container Runtime Telemetry and Delivery

## Problem

The Studio Runtime Gantt currently identifies workflow node timing but does
not show whether Meshingress and its in-container child processes were under
resource pressure. PID-only collection excludes relevant child processes,
while whole-desktop collection is too broad and does not respect the intended
privacy boundary.

Meshingress also has no established container deployment contract. The prior
Kubernetes-first hot-replacement design is valuable long-term but is too broad
for the near-term delivery and telemetry objective.

## Product Requirement

Meshingress must have a Coolify-compatible single execution-container delivery
model. The container is the aggregate resource boundary for Meshingress and
only the processes it launches within that container.

During an active workflow run, Studio can subscribe to a read-only,
run-associated native MCP Resource. Meshingress supplies bounded aggregate
samples; Studio owns series visibility, colors, graph rendering, filters,
tooltips, and session-local history.

## Functional Requirements

1. The deployment contract packages Meshingress as one image suitable for a
   Coolify application deployment, with an explicit health endpoint and
   CPU/memory limits.
2. The metric boundary includes the JVM and its child processes inside the
   execution container; it excludes host applications, external services,
   separate containers, and unrelated desktop processes.
3. The MCP server advertises the standard Resources capability and supports a
   run-scoped telemetry resource template, read, and subscription flow.
4. Resource samples contain only aggregate, documented metrics such as
   container CPU, memory, I/O, network counters, JVM heap, and thread count.
5. A resource update tells Studio to obtain a new sample; Studio does not
   treat missing data as zero.
6. Every sample has a server-owned, run-relative time coordinate so it can
   align with the Gantt without browser clock drift.
7. Studio exposes a stable-colored series legend and runtime settings to show
   or hide individual metrics. The presentation clearly says
   "Execution container load".
8. Metric buffers are bounded and session/run scoped. They expire after a
   documented retention period and are never a general host-surveillance log.
9. A sampler, container-runtime failure, or unavailable resource leaves the
   normal Gantt usable and visibly marks telemetry unavailable or gapped.
10. The design keeps collection and resource serialization independent of
    Coolify-specific APIs, so a later Cloudflare Containers adapter may supply
    the same schema without changing Studio semantics.

## Coolify Operational Requirement

Coolify may provide image deployment, health checking, proxy routing,
container resource limits, and operational container state. It must not be
treated as the authoritative source for Studio's run/node identity or
per-workflow telemetry. Its deployment type and metrics capabilities must be
verified at implementation time, especially if a Docker Compose stack is
selected.

## Retained Replacement Constraints

This record does not deliver hot replacement. If a later delivery replaces a
serving runtime, it must retain the discontinued record's invariant that a
failed candidate B never drains serving A. Readiness, explicit drain,
bounded execution completion, WebSocket reconnect, and durable-state decisions
remain future required work.

## Non-Goals

- One container per tool call, node, retry, or workflow run.
- Exact per-tool resource attribution from one shared container.
- Host-wide or desktop-wide surveillance.
- Tool argument/result capture or process-command inspection.
- A Coolify dashboard embedded inside Studio.
- Immediate Kubernetes, Cloudflare, or Coolify deployment implementation.

## Acceptance Criteria for a Later Implementation

- A test workflow that launches an in-container child process produces a
  bounded resource series that excludes a known unrelated host process.
- Studio displays timed samples against the same run-relative axis as the
  workflow Gantt, filters individual series, and shows sample gaps honestly.
- A caller can read and subscribe to the advertised MCP resource without
  calling a tool.
- Coolify refuses proxy routing until the declared health endpoint succeeds.
- A deployment or telemetry failure leaves existing workflow execution and the
  Gantt available, with no broad host telemetry exposed.

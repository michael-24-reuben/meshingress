# Context

## Origin and Supersession

The owner chose a Coolify-first container delivery path after discussing a
Studio Runtime Gantt overlay for resource telemetry. The preceding
`2026-08-09-meshingress-hot-runtime-replacement` record was moved to
`architect/discontinued/` by owner direction. This entry is its successor for
near-term container delivery and telemetry only.

The discontinued record remains relevant: a future replacement must preserve
the invariant that a failed candidate runtime does not disrupt the serving
runtime, and it must explicitly design readiness, drain, reconnect, and
durable state. These requirements are intentionally not claimed as supplied by
Coolify or this future telemetry feature.

## Current Runtime Baseline

- Studio Runtime traces are session-local and browser-timed.
- `WorkflowRuntime` has `WorkflowRunListener` callbacks for run start, node
  start/completion, and run completion.
- MCP currently advertises tools capability, not Resources capability.
- Meshingress has HTTP `/mcp` and MCP WebSocket `/mcp/ws`; Studio currently
  uses a workflow WebSocket for live execution events.
- No Kubernetes manifest, container image, Compose stack, or Coolify contract
  is present in this checkout as of this record's creation.

## Telemetry Boundary

The initial boundary is one Meshingress execution container/cgroup. Metrics
are aggregate for all eligible processes inside it, including the JVM and
child processes launched by tools. A single graph can correlate a node span
with a load change, but cannot attribute a share of that load to one node.

Excluded by design:

- unrelated host/desktop processes;
- separate containers and sidecars unless an explicit roll-up is added;
- remote API/service resource use;
- process command lines, payloads, credentials, file paths, and outputs.

## Deployment and Provider Notes

Coolify is the immediate target because it deploys Docker applications and
supports health checks, proxy routing, and configured resource limits. Its
operational dashboard is not the source of Studio run/node identity; the
runtime's native MCP Resource remains responsible for the chart data.

Cloudflare Containers is a later portability target. It can provide aggregate
container metrics, but a Worker/container delivery model and session transport
would be a separate architecture decision. This entry therefore defines a
provider-neutral telemetry schema rather than a Cloudflare implementation.

## Verification Boundaries

Any future implementation should prove all of the following before completion:

1. the deployed image becomes healthy before proxy traffic;
2. a known in-container child process affects aggregate telemetry;
3. a known unrelated host process does not;
4. telemetry timestamps align with the workflow Gantt;
5. the MCP resource omits prohibited/sensitive data; and
6. unavailable telemetry leaves normal workflow execution and Gantt behavior
   intact.

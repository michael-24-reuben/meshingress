# Coolify Container Runtime Telemetry and Delivery

## Goal

Define the near-term Meshingress delivery direction: run Meshingress in one
Coolify-managed execution container, expose aggregate container telemetry to
the Studio Runtime Gantt through a native MCP Resource, and retain a
platform-neutral path for future Cloudflare support.

## Scope

- Establish one container/cgroup as the telemetry boundary for the Meshingress
  JVM and child processes launched inside it.
- Define the read-only MCP telemetry resource and the Studio-owned graph,
  colors, filters, and availability states.
- Define the Coolify deployment prerequisites: image, health check, proxy
  routing, resource limits, and operational metrics boundaries.
- Preserve the strict replacement, readiness, draining, WebSocket reconnect,
  and durable-state constraints from the discontinued
  `2026-08-09-meshingress-hot-runtime-replacement` record.
- Keep the delivery model portable enough that a future Cloudflare Containers
  adapter can supply equivalent aggregate container metrics.

## Boundaries

- Design/record work only. Do not add Java source, Dockerfiles, Compose files,
  Coolify configuration, MCP methods, or Studio code without separate
  authorization.
- This is one shared execution container, not one container per tool call or
  workflow node.
- Telemetry is aggregate container state; it must not claim exact per-tool
  CPU, memory, disk, or network attribution.
- Do not collect tool arguments/results, headers/tokens, command lines,
  files, network destinations, process lists, or arbitrary desktop activity.
- Coolify is the initial target, not evidence that Kubernetes replacement or
  Cloudflare delivery is already implemented.

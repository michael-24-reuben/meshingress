# Todo

## Decisions Required

- [ ] Approve one shared execution container as the initial telemetry scope.
- [ ] Choose the initial metric series, units, sampling interval, and cap.
- [ ] Choose run-buffer retention and subscription authorization policy.
- [ ] Choose the first Coolify host and deployment type.
- [ ] Confirm the initial image/tool dependency set and resource limits.

## Later Implementation

- [ ] Create the Coolify-compatible image and health-check contract.
- [ ] Add the central aggregate container/JVM sampler.
- [ ] Add standard MCP Resources capability and a run-scoped telemetry URI.
- [ ] Add Studio Runtime graph overlay, color legend, and metric filters.
- [ ] Verify aggregate child-process inclusion and unrelated-host exclusion.
- [ ] Document operational rollback and telemetry-failure behavior.

## Deferred Follow-Ups

- [ ] Evaluate a Cloudflare Containers adapter after the local Coolify contract
  and telemetry schema are proven.
- [ ] Revisit per-tool or per-run containers only if aggregate correlation is
  insufficient.
- [ ] Reopen strict hot replacement as a separate implementation decision.

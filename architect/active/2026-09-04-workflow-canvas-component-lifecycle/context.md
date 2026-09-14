# Context

## Related scene-builder decision

The new scene-builder dashboard established `WorkflowCanvasSceneEventTracker`
as the state medium between event producers and canvas consumers:

```txt
dashboard / canvas interaction
  -> typed scene trigger
  -> tracker validates and normalizes scene state
  -> subscription receives immutable snapshot
  -> canvas, slot, node, and path render attributes from that snapshot
```

The tracker is deliberately a mediator with `dispatch()` and `subscribe()`, not
a native DOM `EventTarget`. React pointer, focus, and click handlers remain at
the component edge and submit typed triggers to it.

## Boundaries

- `states/enums.json` remains the runtime state catalog.
- The honeycomb inspiration remains a consumer and must not independently
  infer official scene state.
- Do not make the abstract base class a second state store; it observes the
  tracker and owns only its component-local lifecycle.
- Root `architect/ASSIGNMENT.md` and `architect/HANDOFF.md` are dirty for an
  unrelated active effort and must not be overwritten by this design record.

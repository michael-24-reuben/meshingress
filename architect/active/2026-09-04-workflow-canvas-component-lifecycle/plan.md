# Lifecycle Design Plan

## Candidate base contract

Use a generic abstract `WorkflowCanvasComponent<TInput, TModel, THandle>`.

```ts
abstract class WorkflowCanvasComponent<TInput, TModel, THandle> {
  protected constructor(identity: ComponentIdentity, tracker: WorkflowCanvasSceneEventTracker)
  init(input: TInput): void
  start(): THandle
  stop(): void
  dispose(): void

  protected abstract parse(input: TInput): TModel
  protected abstract mount(model: TModel): THandle
  protected abstract update(scene: WorkflowCanvasScene): void
}
```

This is a design sketch, not an approved API.

## Lifecycle responsibilities

| Phase | Responsibility | Must not do |
|---|---|---|
| constructor | Receive stable identity and shared scene/tracker dependencies. | Parse workflow payloads, subscribe, or render. |
| `init(input)` | Validate and parse component-specific input into a local model; establish invariant state. | Attach DOM listeners or begin rendering. |
| `start()` | Subscribe to normalized scene updates, mount/render from the already parsed model, and expose a runtime handle. | Reparse incoming workflow data or choose global state transitions. |
| `stop()` | Reversibly detach subscriptions, cancel local transient work, and suspend rendering participation. | Destroy parsed input or make the component unusable. |
| `dispose()` | Terminal cleanup after it can never restart. | Act as a second ambiguous synonym for `stop()`. |

Prefer `dispose()` over `exit()`: `stop()` has a clear reversible meaning,
while `dispose()` communicates terminal ownership cleanup. Add `exit()` only if
the host platform later has an explicit navigation/teardown event requiring it.

## Start-phase ordering

```txt
1. assert init completed
2. read one normalized scene snapshot from the tracker
3. subscribe before exposing the mounted handle
4. mount/render the parsed local model against that snapshot
5. route later updates through update(scene)
```

The component does not parse raw events in `start()`. Event translation belongs
to the scene tracker; raw workflow input parsing belongs in `init()`.

## Open design question

The current renderer is React. Before implementing an imperative `mount()` API,
decide whether the handle represents a React-facing view-model/subscription
rather than direct DOM ownership. Direct DOM mounting risks creating a parallel
render tree beside React.

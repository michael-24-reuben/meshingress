# Assessment

`onPointerMove` queued a functional `setTransform` update that read
`pan.current` when React later executed the updater. `endPan` and
`onLostPointerCapture` correctly clear that ref when a pointer finishes or
loses capture. Therefore, a queued move update could execute after cleanup and
attempt to access `pan.current.originX` while `pan.current` was null.

The uncaught render error caused React to unmount the `WorkflowCanvas` tree,
which presented as a blank page. This was not a `#root` clearing issue.

The existing pending `2026-08-03-studio-root-and-site-error-pages` entry has a
separate scope: production root routing and an intentional browser error
surface. It does not own this pan-state race and was left unchanged.

# Fixes

Updated `WorkflowCanvas.tsx` so `onPointerMove` copies the active pan
gesture's origins, start coordinates, and current pointer coordinates into
local values before it queues `setTransform`.

The queued updater now depends only on those immutable values and the prior
transform state. Pointer completion and lost-capture cleanup can continue to
clear `pan.current` immediately without invalidating an already received move
event.

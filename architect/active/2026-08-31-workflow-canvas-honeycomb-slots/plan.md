# Plan

1. Capture and refine the honeycomb state vocabulary. **In progress.**
2. Agree the visible grammar for empty, occupied, reserved, valid-drop, and
   invalid-drop slots. The passive outer frame owns the separated axial
   placement; the interactive slot is a regular hex calculated from the frame
   side and its own inset, with independent state overlays.
3. Build a separate HTML/TypeScript inspiration prototype, isolated from the
   production Studio surface.
4. Keep the prototype prop-compatible with `WorkflowCanvas` so its only future
   production integration seam is the canvas element in `Workbench.tsx`.
5. Exercise pointer, keyboard, and reduced-motion behavior in the prototype.
6. Only after user approval, design an implementation migration for the
   production `WorkflowCanvas` and its persisted model boundaries.

## Implementation boundary

Do not modify `WorkflowCanvas.tsx`, workflow persistence, or production CSS as
part of the prototype-design slice unless the user explicitly requests it.

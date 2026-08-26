# Fixes

## Added package

Added `app/meshingress-studio-web/src/features/workflow-studio/components/argument-inputs/`.

- `ArgumentInput.tsx` dispatches from JSON Schema type/format and falls back to
  text when a current value cannot safely render in the specialized control.
- `contracts.ts` normalizes property schema metadata and shares the field
  contract.
- Separate modules provide text, integer, number, boolean, enum, date,
  date-time, time, URL, email, and password controls.

## Integration

`InspectorPanel` now resolves each property schema once, retains the existing
type icon and parameter label, shows schema descriptions and required markers,
and sends each child control's exact string output into `WorkflowNode.arguments`.
The existing workflow-definition serializer continues to submit that map with
no backend defaults or server-side argument replacement.

## Styling

Added compact counter controls, visible hover/focus states, a labelled boolean
control, descriptions, and required indicators to the existing Studio stylesheet.

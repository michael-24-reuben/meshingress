# Context

## Source anchors

- `app/meshingress-studio-web/src/features/workflow-studio/components/InspectorPanel.tsx`
  currently resolves a parameter schema for icons and renders the argument value.
- `app/meshingress-studio-web/src/features/workflow-studio/api/mcp.ts` exposes
  `McpToolFunction.inputSchema` returned by MCP `tools/list`.
- `app/meshingress-studio-web/src/features/workflow-studio/model/types.ts` keeps
  `WorkflowNode.arguments` as `Record<string, string>`.
- `app/meshingress-studio-web/src/features/workflow-studio/model/definition.ts`
  serializes those exact strings into the submitted workflow definition.
- `app/meshingress-studio-web/src/features/workflow-studio/components/icons/node-icons.tsx`
  already maps schema types/formats to object-type representation icons.

## Component contract

`contracts.ts` will define a small normalized field descriptor derived from a
JSON Schema property: `type`, `format`, `enum`, `minimum`, `maximum`,
`multipleOf`, `required`, `description`, and multiline intent where present.
The contract will expose `value: string`, `onChange(value: string)`, a stable
input id, and the accessible name/description ids supplied by the parent.

The dispatcher must prefer the live `inputSchema` received from MCP and retain
the existing local catalogue only as a metadata fallback. It must have a safe
text fallback when a tool has no schema or declares an unsupported schema.

## Payload invariants

The visible control is authoritative: its emitted value updates
`WorkflowNode.arguments`, and that same object is serialized by the existing
definition builder. Numeric, boolean, date, and enum controls may validate or
constrain user entry in the browser, but they must not silently create values
that are absent from the UI.

## Accessibility and interaction requirements

- Each field remains linked to the existing parameter label and description.
- Numeric controls work with keyboard entry and native increment/decrement
  controls; do not rely on pointer-only counter buttons.
- Enum selection, checkbox/switch state, and calendar inputs have visible
  focus treatment and keyboard operation.
- Preserve a readable validation message for invalid browser input rather than
  substituting an arbitrary payload value.

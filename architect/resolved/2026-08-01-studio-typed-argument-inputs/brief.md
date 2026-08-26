# Workflow Studio typed argument input components

## Objective

Replace the Inspector's one-size-fits-all argument textarea with a modular UI
that selects a field control from the tool parameter's JSON Schema type and
format. Each supported argument type will have its own component module in a
dedicated package.

## Current state

`InspectorPanel` renders every selected argument as a textarea. The Studio
already receives each tool's `inputSchema` from MCP (with the local tool
catalogue as a fallback), and workflow definitions submit node arguments as
`Record<string, string>` to `/api/v1/workflows/run` or the workflow WebSocket.

## Design decision

Create this package under the Workflow Studio feature:

```text
components/argument-inputs/
  ArgumentInput.tsx              # schema-to-component dispatcher
  contracts.ts                   # shared props and normalized type contract
  TextArgumentInput.tsx
  IntegerArgumentInput.tsx
  NumberArgumentInput.tsx
  BooleanArgumentInput.tsx
  EnumArgumentInput.tsx
  DateArgumentInput.tsx
  DateTimeArgumentInput.tsx
  TimeArgumentInput.tsx
  UrlArgumentInput.tsx
  EmailArgumentInput.tsx
  PasswordArgumentInput.tsx
```

`InspectorPanel` owns the argument name, required marker, description, and
schema lookup. `ArgumentInput` owns control selection only. Every child field
receives a string value and emits a string, so the current submitted workflow
definition remains the source of the runtime payload without backend defaults,
sample arguments, or server-side type-specific values.

## Initial implementation scope

Create the following component modules:

- Text: ordinary strings and the unknown-schema fallback; preserve multiline
  text capability when the schema or tool metadata calls for it.
- Integer: a keyboard-accessible numeric counter using a number control and
  stepper semantics; emits a canonical integer string.
- Number: a numeric counter using number control and stepper semantics; honors
  schema `minimum`, `maximum`, and `multipleOf` when supplied; emits a canonical
  decimal string.
- Boolean: a labelled checkbox or switch; emits `"true"` or `"false"`.
- Enum: a labelled select populated from schema `enum` values.
- Date: a native `input[type=date]` calendar control.
- Date-time: a native `input[type=datetime-local]` calendar/time control.
- Time: a native `input[type=time]` control.
- URL: a native URL input with browser validation.
- Email: a native email input with browser validation.
- Password: a masked password input. It remains a string in the submitted
  definition; no secret storage or backend secret resolution is introduced in
  this slice.

## Explicitly deferred

Do not create components yet for arrays, objects/JSON editors, null, files,
binary/base64, images, code editors, duration, interval, color, location, or
any custom/unknown media type beyond the text fallback. These require distinct
editing, serialization, validation, upload, or security decisions and will be
tracked as later architecture work.

## Non-goals

- No backend endpoint, workflow runtime, or tool-execution changes.
- No hard-coded tool argument values.
- No third-party calendar, form, code-editor, or JSON-editor dependency in the
  first slice.
- No change from the current string argument payload contract.

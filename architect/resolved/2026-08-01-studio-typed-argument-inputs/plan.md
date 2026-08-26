# Implementation plan

1. Add `components/argument-inputs/contracts.ts` and the dispatcher. Normalize
   JSON Schema metadata without introducing a second source of parameter data.
2. Implement the initial simple field modules independently, using native HTML
   controls and shared CSS classes only where presentation is common.
3. Replace the textarea branch in `InspectorPanel` with `ArgumentInput`, while
   retaining labels, descriptions, required indicators, and current update flow.
4. Add focused component/Inspector behavior coverage for dispatch and string
   emission. Verify a workflow definition contains precisely the values shown
   in the Inspector.
5. Run Studio lint and production build, then manually validate one tool for
   text, number, boolean, enum, and date/date-time values through the existing
   Run action.

## Acceptance criteria

- The listed initial controls are rendered from live tool schemas and show the
  existing type icon before the parameter name.
- A number parameter renders a usable counter/number field, and a date or
  date-time parameter renders a native calendar-capable field.
- Every supported control updates and submits the existing string argument map.
- Unsupported or complex schemas use the text fallback and are not misrepresented
  as a specialized editor.
- No Meshingress server changes are needed for this UI slice.

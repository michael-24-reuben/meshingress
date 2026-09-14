# Notes

- The initial dashboard is intentionally declarative: it proves the event/data boundary without pretending all future state designs already have approved visual grammar.
- The tracker owns both dashboard-dispatched state changes and preview hover/focus/selection overlay triggers. Node overlays are token sets, so hover and focus do not erase selection.
- Verification: `npm run build` passed. The local `?view=scene-builder` preview mounted the event controls, all nine JSON state groups (58 values), and the scene-fed honeycomb canvas.

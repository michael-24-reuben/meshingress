# Workflow Canvas Honeycomb Handoff

## Workspace

- repository: `C:\Users\jbeas\Repositories\Dev.java-2026\artifacts\meshingress`
- branch: `development` — do not switch branches.
- active record: `architect/active/2026-08-31-workflow-canvas-honeycomb-slots`
- scope: isolated Studio inspiration prototype only. Do not change the
  production `WorkflowCanvas`, workflow persistence, or the `Workbench.tsx`
  integration seam without explicit user approval.

## Goal

Continue evolving a flat-topped honeycomb WorkflowCanvas prototype. The user
wants a parent frame around each separated cell and an interactive hexagonal
slot inside it, while retaining the existing node prototype, real sample
workflow inputs, and CSS-owned geometry.

## Current Frame Geometry

- `--hwp-frame-side` is the nominal frame/port-ring side. It drives axial
  placement and the six side-port coordinates, not the displayed frame size.
- `--hwp-grid-gap` now shrinks centered visual-frame dimensions. The visual
  frame, inner slot, and node therefore may expose a void without moving ports.
- `--hwp-slot-side` derives from the visual frame and `--hwp-slot-inset` using
  the regular-hex relation `2 / sqrt(3)`; it is not an authored root dimension.
- `span.hwp-slot-frame` is the positioned, passive parent. Its SVG boundary
  is transparent, muted, solid, 1px, mitered, and `pointer-events: none`.
- `button.hwp-slot` is centered within the frame, uses the calculated slot
  dimensions, and remains the sole interactive/accessibility surface.
- Hover, focus, and selection use the button's clipped pseudo-element. The
  passive frame must not react to input.
- The node derives from the child slot, preserves a 4px node inset and 4px
  node perimeter, and remains hex-clipped.

## Newly Agreed Stacked-Port Model (initial marker slice implemented)

- Each slot owns six `SlotPort`s, one per nominal hex vertex. The ports are
  dedicated to the slot and are calculated from `--hwp-frame-side` geometry.
- `--hwp-grid-gap` shrinks only the rendered frame, slot, and node, leaving the
  port ring fixed in the resulting void. This geometry split is implemented.
- Neighboring slots' corresponding ports deliberately stack at the same
  coordinate but remain separate objects; they must retain their slot-qualified
  identities rather than being de-duplicated as shared anchors.
- Close-up view may fan the displayed endpoints, reveal path data, and use
  curved directional tangents. These are presentation offsets only: compact and
  close-up views retain the same logical port endpoints and edges.

## Relevant Files

- `app/meshingress-studio-web/src/features/workflow-studio/canvas/honeycomb-prototype/HoneycombWorkflowCanvasInspiration.tsx`
  - Renders 35 axial cells, each frame + button pair. Uses real `nodes` and
    `edges`; `r-005` is test-page-only at `q:1,r:0` between `r-001` and
    `r-002`.
- `app/meshingress-studio-web/src/features/workflow-studio/canvas/honeycomb-prototype/honeycomb-workflow-canvas.css`
  - Authoritative frame/slot/node geometry and all `hwp-` scoped styling.
- `app/meshingress-studio-web/src/features/workflow-studio/canvas/honeycomb-prototype/honeycomb-grid.ts`
  - Pure axial geometry; TypeScript receives resolved CSS dimensions via the
    hidden probe.
- `app/meshingress-studio-web/src/features/workflow-studio/HoneycombWorkflowCanvasTestPage.tsx`
  - Query-selectable prototype harness (`?view=honeycomb`).
- `docs/workflow-canvas-state-model.md`
  - State vocabulary and JSON examples; CSS owns local ephemeral presentation,
    TypeScript owns cross-element semantic state.

## Important Recent Decisions

- Earlier dashed and gapless-frame experiments were explicitly rejected.
  Do not restore their geometry or markup.
- The current outer frame is solid, not dashed.
- The frame is the parent geometry root; the slot is calculated from it.
- Current user edits to node metadata (`hwp-node-kind`, header, and footer)
  are live source and must be preserved.

## Verification

```powershell
cd C:\Users\jbeas\Repositories\Dev.java-2026\artifacts\meshingress\app\meshingress-studio-web
npm run build
```

This passed after the frame-root geometry change. Vite emitted only its
existing main-bundle-size advisory.

## Known Gap

The desktop browser preview is currently available and baseline port QA passed.
Do not treat the port-design refinement as complete until the live
`?view=honeycomb` surface continues to confirm:

1. parent frames visibly retain the original separated lattice;
2. the child slot is centered within each frame;
3. the outer solid line stays passive while button selection works; and
4. node and frame borders do not overlap.
5. each slot displays six passive port markers at nominal vertex coordinates,
   with co-located three-way markers remaining visually stable.

The current `--hwp-grid-gap` is `0.2px`, so the new visual void is subtle.
Marker size, contrast, layering, and desired void size are still user-led
refinement choices.

## Current Worktree Safety

- This is a large, dirty shared worktree with unrelated server, storage,
  generated, asset, and Architect changes. Preserve all of it.
- The root `architect/ASSIGNMENT.md` and `architect/HANDOFF.md` belong to the
  separate active storage-provider objective. Do not overwrite them for this
  honeycomb work.
- The honeycomb prototype and its Architect record are currently untracked;
  inspect live files rather than relying only on `git diff`.
- Never use reset, checkout, or branch-changing commands.

## Immediate Next Step

Run the local Studio preview at `http://127.0.0.1:5173/?view=honeycomb`,
perform the five visual checks above, then refine the passive port markers with
the user before adding port interaction or restoring any path rendering.

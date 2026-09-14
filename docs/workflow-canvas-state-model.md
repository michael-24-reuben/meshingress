# Workflow Canvas State Model

Status: design reference for the exploratory `WorkflowCanvas` honeycomb-slot
prototype. It is not yet a persisted workflow format or a public API contract.

## Purpose

The current workflow canvas uses freely positioned, grid-snapped rectangular
nodes. The proposed canvas uses discrete hexagonal slots. A slot owns placement;
a node occupies a slot; paths visualize flow between compatible nodes.

The model keeps mutually exclusive states in a single field and uses additive
flags for interaction and presentation. For example, a selected node may also
be bookmarked and in an error state. Those are overlays, not competing node
states.

## State rules

- A slot has exactly one occupancy state.
- A node has exactly one execution state. It may have zero or more overlays.
- A node's input/output capability is separate from whether a path is attached.
- A dragged node keeps its origin `reserved` until a valid drop commits. It does
  not make the origin empty during the drag.
- A path has exactly one lifecycle state plus zero or more overlays.
- Visual priority resolves conflicts; a blocking/error condition always wins
  over a passive state such as hover or selection.

## Reference JSON

```json
{
  "workflowCanvas": {
    "layout": {
      "kind": "hex-slot",
      "coordinates": "axial",
      "slotId": "slot/q3-r-1"
    },
    "canvas": {
      "interaction": "idle",
      "workflowState": "incomplete",
      "overlays": ["none"]
    },
    "slot": {
      "id": "slot/q3-r-1",
      "occupancy": "empty",
      "dropState": "none",
      "capabilities": {
        "acceptsNode": true,
        "acceptsInput": true,
        "acceptsOutput": true
      },
      "overlays": ["hovered"]
    },
    "node": {
      "id": "r-001",
      "slotId": "slot/q3-r-1",
      "executionState": "idle",
      "capabilities": {
        "acceptsInput": true,
        "acceptsOutput": true
      },
      "connectionState": "connected",
      "overlays": ["selected", "bookmarked"],
      "diagnostics": []
    },
    "drag": {
      "state": "dragging",
      "nodeId": "r-001",
      "originSlotId": "slot/q3-r-1",
      "originSlotOccupancy": "reserved",
      "hoveredSlotId": "slot/q4-r-1",
      "hoveredSlotState": "valid-drop-target"
    },
    "path": {
      "id": "r-001->r-002",
      "sourceNodeId": "r-001",
      "targetNodeId": "r-002",
      "sourcePortId": "port/junction/u4-v3/slot/q0-r1",
      "targetPortId": "port/junction/u6-v3/slot/q2-r0",
      "traversalPortIds": [
        "port/junction/u4-v3/slot/q0-r1",
        "port/junction/u6-v3/slot/q2-r0"
      ],
      "turnAllocations": [],
      "lifecycleState": "connected",
      "flowState": "idle",
      "overlays": []
    }
  },
  "enums": {
    "canvasInteraction": [
      "idle",
      "panning",
      "zooming",
      "selecting",
      "dragging-node",
      "drawing-path",
      "reconnecting-path",
      "layout-revealing"
    ],
    "workflowState": [
      "valid",
      "incomplete",
      "disconnected",
      "unreachable",
      "cyclic",
      "blocked",
      "executing",
      "completed",
      "failed"
    ],
    "slotOccupancy": [
      "empty",
      "occupied",
      "reserved",
      "blocked",
      "out-of-bounds"
    ],
    "slotDropState": [
      "none",
      "valid-drop-target",
      "invalid-drop-target"
    ],
    "nodeExecutionState": [
      "idle",
      "running",
      "success",
      "warning",
      "error",
      "disabled",
      "unavailable"
    ],
    "nodeConnectionState": [
      "isolated",
      "connected",
      "missing-required-input",
      "blocked-by-upstream-error",
      "stale-output"
    ],
    "nodeOverlay": [
      "selected",
      "hovered",
      "focused",
      "bookmarked",
      "dragging",
      "lifted-from-origin",
      "has-unsaved-changes",
      "unreachable",
      "invalid"
    ],
    "pathLifecycleState": [
      "unconnected",
      "drafting",
      "connected",
      "pending-validation",
      "invalid",
      "blocked"
    ],
    "pathFlowState": [
      "idle",
      "active",
      "success",
      "warning",
      "error",
      "muted"
    ]
  }
}
```

## Prototype identity grammar

The isolated honeycomb prototype uses readable, deterministic topology keys.
They are implementation-facing identifiers, not CSS selectors or persisted
workflow IDs yet.

```txt
slot/q<q>-r<r>
junction/u<u>-v<v>
port/junction/u<u>-v<v>/slot/q<q>-r<r>

slot/q1-r0
junction/u4-v1
port/junction/u4-v1/slot/q1-r0
```

A junction identifies the shared three-slot vertex. A port identifies one
concrete endpoint owned by one slot at that junction, so stacked ports retain
independent direction, availability, capacity, and path history.

The prototype uses a concrete port graph to find a legal junction sequence. It
then renders the logical route spine: the source anchor, one selected
slot-owned anchor at every non-straight middle junction, and the target anchor.
`data-route-port-ids` and `data-route-junction-ids` therefore expose only those
intentional anchors, not every physical stacked-port handoff considered during
the graph search.

Every concrete port marker exposes `data-port-connection-count`. It is the
number of distinct logical route spines using that exact `slotPortId`; it
starts at `0`. It is never a count of the shared `junctionId`, so stacked ports
at the same coordinate retain independent counts.

The current prototype treats every slot as a traversable corridor. Its cost is
physical perimeter distance with a small direct-trajectory bias and a stable
lexical tie-breaker. Availability, input/output compatibility, capacity,
occupied-slot policy, and rerouting after a port closes remain future work.

When traffic crosses a junction, the planner retains the concrete output port
that made the crossing for the following in-slot hop. It scores the two
adjacent exits by collinearity with that port's geometric axis, making a
straight-ahead continuation cheaper than the sideward sibling without making
the latter illegal. For example, `port/junction/u7-v4/slot/q2-r0` continues
through `slot/q1-r1` to `port/junction/u6-v3/slot/q1-r1`; its `u6-v5` sibling
remains a fallback.

## Directional port conditions

Availability and traffic role are independent. Every port may begin
`availability: "open"`; that says it is eligible for routing, not that it is
bidirectional. In this prototype, a route makes exactly two traffic-role
claims: its source anchor is `output` and its target anchor is `input`.
Pass-through anchors do not receive a traffic role.

```json
{
  "slotPortId": "port/junction/u4-v3/slot/q0-r1",
  "availability": "open",
  "trafficRole": "input",
  "acceptsInboundTraffic": true,
  "acceptsOutboundTraffic": false
}
```

| Role | May receive traffic | May emit traffic | Invariant |
| --- | --- | --- | --- |
| `input` | yes | no | Cannot be used to start or continue an outbound hop. |
| `output` | no | yes | Cannot be used to end or continue an inbound hop. |

The prototype begins with every port `open` and unclaimed. The graph uses
temporary directional states while it searches, but only its two endpoint
states are persisted as route traffic claims or emitted as
`data-traffic-role`. It renders outputs in cyan, inputs in purple, and every
unclaimed or pass-through port in the muted default colour.

A directed traversal begins at an output endpoint and ends at an input
endpoint. Its middle anchors are geometric route attachments, selected from the
inside of each 120-degree turn; they are not node traffic bindings. No concrete
endpoint port may carry both roles at once. Formal user-controlled role
assignment, capacity, and closure are still future work.

## Inner-turn port allocation

A path turn has a third concern besides topology and traffic direction: which
of the three concrete ports stacked at its middle junction belongs to the slot
on the **inside** of the angle. For every non-straight junction sequence
`previous → current → next`, the prototype allocates the current-junction port
owned by the only slot incident to all three junctions.

```txt
junction/u9-v4 -> junction/u10-v5 -> junction/u9-v6
                            │
                  slot/q2-r1, right port
                  port/junction/u10-v5/slot/q2-r1
```

```json
{
  "previousJunctionId": "junction/u9-v4",
  "junctionId": "junction/u10-v5",
  "nextJunctionId": "junction/u9-v6",
  "slotId": "slot/q2-r1",
  "slotPortId": "port/junction/u10-v5/slot/q2-r1"
}
```

This allocation is intentionally separate from `input` / `output`: it is the
route's interior-side owner, not a second traffic role. Route SVGs expose
`data-route-turn-port-ids` and `data-route-turn-slot-ids`; the corresponding
port marker receives `data-route-turn-allocation="true"` without an additional
allocation ring.
It deliberately omits `data-traffic-role`: endpoint bindings alone own traffic
roles. The turn's slot ownership is selected from the inward 120-degree side,
not from source/destination membership.

## Slot conditions

| Condition | Meaning | Visual intent |
| --- | --- | --- |
| `empty` | Contains no node or future slot content. | Quiet hex outline or subtle fill. |
| `occupied` | Contains one attached node or other slot content. | Node owns the central hex. |
| `reserved` | Origin of a node currently being dragged. | Ghost node/placeholder; cannot accept another node. |
| `valid-drop-target` | Empty, accessible slot under the dragged node that may accept it. | Strong positive highlight. |
| `invalid-drop-target` | Hovered slot cannot accept the dragged node. It may be occupied, blocked, incompatible, or out of bounds. | Clear reject treatment; never imply a successful drop. |
| `blocked` | Permanently unavailable due to layout, policy, grouping, or future content. | Muted, locked treatment. |
| `out-of-bounds` | Outside the editable honeycomb space. | Normally not rendered; reject if reached. |
| `hovered` | Pointer is over the slot. This is an additive overlay. | Gentle highlight, beneath drop-state feedback. |

`occupied` and `empty` are occupancy states. `valid-drop-target` and
`invalid-drop-target` are drag-time states layered on top of occupancy, rather
than alternatives that erase it. An occupied slot hovered during a drag is
therefore represented as `occupancy: "occupied"` plus
`dropState: "invalid-drop-target"`.

## Node conditions

### Capabilities

Every node declares these independently. A trigger may only offer output; a
terminal/output node may only accept input; a transform usually does both.

| Capability | Meaning |
| --- | --- |
| `acceptsInput: true` | The node exposes one or more inbound connection points. |
| `acceptsOutput: true` | The node exposes one or more outbound connection points. |
| `acceptsInput: false` | No inbound path may be attached. |
| `acceptsOutput: false` | No outbound path may originate here. |

The canvas should not infer either capability from the node type or from
whether it currently has a connected path.

### Execution and availability

| Condition | Meaning |
| --- | --- |
| `idle` | Ready but not currently executing. |
| `running` | Currently executing. |
| `success` | Most recent execution completed successfully. |
| `warning` | Completed or validated with a non-fatal concern. |
| `error` | Failed execution or validation. |
| `disabled` | Intentionally excluded from execution. |
| `unavailable` | Required tool/function is not available in the current runtime. |

### Connectivity and diagnostics

| Condition | Meaning |
| --- | --- |
| `isolated` | Occupies a slot but has no attached path. |
| `connected` | Has a structurally valid attached path. |
| `missing-required-input` | Needs an input path that is absent. |
| `blocked-by-upstream-error` | Cannot run because a required upstream node failed. |
| `stale-output` | Inputs/configuration changed since the node last produced output. |
| `unreachable` | Cannot be reached from a trigger/start node. |
| `invalid` | Fails a structural or semantic workflow rule. |
| `has-unsaved-changes` | Presentation/configuration differs from persisted state. |

### Interaction overlays

| Condition | Meaning |
| --- | --- |
| `hovered` | Pointer is over the node. |
| `selected` | Current inspector/keyboard selection. |
| `focused` | Keyboard focus target. |
| `bookmarked` | User saved the node/tool for quick access. |
| `dragging` | Node is actively being dragged. |
| `lifted-from-origin` | Dragging presentation specifically detached from its reserved origin slot. |

## Drag conditions and transitions

```json
{
  "dragLifecycle": [
    {
      "event": "grab-node",
      "from": { "originSlot": "occupied", "node": "idle" },
      "to": { "originSlot": "reserved", "nodeOverlays": ["dragging", "lifted-from-origin"] }
    },
    {
      "event": "hover-empty-compatible-slot",
      "to": { "hoveredSlot": "empty", "dropState": "valid-drop-target" }
    },
    {
      "event": "hover-occupied-or-incompatible-slot",
      "to": { "hoveredSlot": "occupied", "dropState": "invalid-drop-target" }
    },
    {
      "event": "release-on-valid-slot",
      "to": { "originSlot": "empty", "targetSlot": "occupied", "nodeOverlays": [] }
    },
    {
      "event": "release-on-invalid-slot-or-cancel",
      "to": { "originSlot": "occupied", "nodeOverlays": [] }
    }
  ]
}
```

## Path conditions

| Condition | Meaning |
| --- | --- |
| `unconnected` | A possible port with no path. |
| `drafting` | User is drawing a new path or reconnecting an existing one. |
| `connected` | Structurally attached source and target. |
| `pending-validation` | Attached, but compatibility/schema validation has not completed. |
| `invalid` | Source/output or target/input is incompatible. |
| `blocked` | Cannot carry flow because of an upstream error, disabled node, or workflow policy. |
| `active` | Currently carrying execution. |
| `success` | Most recent traversal completed successfully. |
| `warning` | Traversal completed with a non-fatal concern. |
| `error` | Traversal failed. |
| `selected` / `hovered` | Interaction overlays for inspection and editing. |
| `muted` | Intentionally de-emphasized without removing the path. |

## Visual conflict priority

When several conditions are true, render the highest applicable category first:

1. `blocked`, `out-of-bounds`, `unavailable`, `error`, `invalid`
2. `invalid-drop-target`, `missing-required-input`, `blocked-by-upstream-error`
3. `valid-drop-target`, `running`, `active`, `pending-validation`
4. `reserved`, `dragging`, `selected`, `focused`
5. `warning`, `success`, `bookmarked`, `hovered`
6. `occupied`, `connected`, `idle`, `empty`

Selection, focus, bookmark, and hover must remain visible as secondary accents;
they must not hide a failure, unavailable node, or rejected drop target.

## Current implementation mapping

The present Studio canvas already has free-position grid snapping, node
selection/favoriting, node run states (`idle`, `running`, `success`, `error`),
and path reconnection. The reference above deliberately expands that vocabulary
for a future hex-slot implementation and does not change the current persisted
workflow node or edge model.

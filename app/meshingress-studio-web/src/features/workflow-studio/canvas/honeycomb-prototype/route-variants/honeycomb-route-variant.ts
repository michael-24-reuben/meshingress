import type { WorkflowEdge } from '../../../types'
import type { AxialSlot, FlatTopHexPortCorner, HoneycombGeometry, HoneycombJunctionCoordinate, HoneycombPoint } from '../honeycomb-grid'

export type PortTrafficRole = 'input' | 'output'

/** A concrete slot-owned port selected as a route hook. */
export interface HoneycombRoutePortHook {
  corner: FlatTopHexPortCorner
  id: string
  junction: HoneycombJunctionCoordinate
  junctionId: string
  point: HoneycombPoint
  slotId: string
}

export interface HoneycombRoutePortState {
  portId: string
  role: PortTrafficRole
}

/**
 * Inputs supplied by the canvas. Variants must treat them as immutable and
 * return a plan rather than rendering or mutating canvas state.
 */
export interface HoneycombRouteRequest {
  assignedRoles: ReadonlyMap<string, PortTrafficRole>
  edge: WorkflowEdge
  geometry: HoneycombGeometry
  slots: readonly AxialSlot[]
  sourceSlot: AxialSlot
  targetSlot: AxialSlot
}

/**
 * A route's ordered hook sequence. `ports[0]` is its origin/output hook and
 * `ports.at(-1)` is its destination/input hook. Variants may expose further
 * decisions through the optional metadata fields without changing this core
 * rendering contract.
 */
export interface HoneycombRoutePlan {
  id: string
  ports: HoneycombRoutePortHook[]
  portStates: HoneycombRoutePortState[]
  turnPortIds: string[]
  turnSlotIds: string[]
}

/** An interchangeable, pure routing policy for the honeycomb prototype. */
export interface HoneycombRouteVariant {
  readonly id: string
  plan(request: HoneycombRouteRequest): HoneycombRoutePlan | null
}

import { useLayoutEffect, useMemo, useState } from 'react'
import type { WorkflowEdge, WorkflowNode } from '../../types'
import {
  flatTopHexPortCorners,
  flatTopHexPosition,
  flatTopHexFrameSideMidpoint,
  flatTopHexSharedFrameSides,
  flatTopHexSlotId,
  flatTopHexSlotPortIdentity,
  type AxialSlot,
  type FlatTopHexFrameSide,
  type HoneycombGeometry,
} from './honeycomb-grid'
import { graphRouteVariant } from './route-variants/graph-route-variant'
import type { PortTrafficRole } from './route-variants/honeycomb-route-variant'
import type { WorkflowCanvasScene } from '../../states/workflow-canvas-scene'
import './honeycomb-workflow-canvas.css'
import './honeycomb-state-presentation.css'
import type { CanvasModelSnapshot, WorkflowModelSnapshot } from '../../states/WorkflowStudioState'
import {
  HoneycombNodeSnapshot,
  HoneycombNodeModelSnapshot,
  type HoneycombNodeSnapshotProps,
} from './HoneycombNodeSnapshot'
import {
  HoneycombSlotFrameSnapshot,
  HoneycombSlotModelSnapshot,
  HoneycombSlotSnapshot,
  flatTopHexFrameSidePath,
  hexCornerPortPolygonPoints,
  type HoneycombSlotFrameSnapshotProps,
} from './HoneycombSlotFrameSnapshot'
import {
  HoneycombCanvasModelSnapshot,
  HoneycombCanvasSnapshot,
  type HoneycombCanvasModelSnapshotProps,
} from './HoneycombCanvasModelSnapshot'
import { HoneycombWorkflowModelAdapter } from './adapters/HoneycombWorkflowModelAdapter'
import {
  readableSlotLabel,
  type HoneycombNodePlacement,
  type HoneycombSlot,
  type HoveredNodeSlot,
  type ImmediateRouteSeam,
} from './honeycomb-types'

export type { HoneycombSlot, HoneycombNodePlacement, HoveredNodeSlot, ImmediateRouteSeam }
export { readableSlotLabel }
export type { HoneycombNodeSnapshotProps, HoneycombSlotFrameSnapshotProps, HoneycombCanvasModelSnapshotProps }
export {
  HoneycombNodeSnapshot,
  HoneycombNodeModelSnapshot,
  HoneycombSlotFrameSnapshot,
  HoneycombSlotModelSnapshot,
  HoneycombSlotSnapshot,
  HoneycombCanvasModelSnapshot,
  HoneycombCanvasSnapshot,
  flatTopHexFrameSidePath,
  hexCornerPortPolygonPoints,
}

export interface HoneycombWorkflowCanvasInspirationProps {
  edges: WorkflowEdge[]
  nodes: WorkflowNode[]
  /** Notifies an embedding Studio surface when an occupied node slot is selected. */
  onNodeSelect?: (nodeId: string) => void
  /** Parsed scene state from the scene builder; absent for the standalone prototype. */
  scene?: WorkflowCanvasScene
  /** Stable state projection used by an integrated renderer. */
  model?: WorkflowModelSnapshot
  /** Allows a scene host to map real workflow data to the temporary honeycomb lattice. */
  placements?: HoneycombNodePlacement[]
}

const workflowSlots: HoneycombSlot[] = Array.from({ length: 7 }, (_, q) =>
  Array.from({ length: 5 }, (_, r) => {
    const slot = { q: q - 1, r: r - 2 }
    return { ...slot, id: flatTopHexSlotId(slot), kind: 'workflow' as const }
  }),
).flat()

const axialNeighborOffsets: readonly AxialSlot[] = [
  { q: 1, r: 0 },
  { q: 1, r: -1 },
  { q: 0, r: -1 },
  { q: -1, r: 0 },
  { q: -1, r: 1 },
  { q: 0, r: 1 },
]

/**
 * A one-slot boundary outside the workflow lattice. These visual-only slots
 * cannot host nodes or receive events; only their junctions touching the
 * workflow lattice remain available to make the canvas edge explicit.
 */
const canvasEndSlots: HoneycombSlot[] = Array.from(new Map(
  workflowSlots.flatMap((slot) => axialNeighborOffsets.map((offset) => {
    const endSlot = { q: slot.q + offset.q, r: slot.r + offset.r }
    return [flatTopHexSlotId(endSlot), { ...endSlot, id: flatTopHexSlotId(endSlot), kind: 'canvas-end' as const }]
  })),
).values()).filter((slot) => !workflowSlots.some((workflowSlot) => workflowSlot.id === slot.id))

const prototypeSlots = [...workflowSlots, ...canvasEndSlots]
const workflowJunctionIds = new Set(workflowSlots.flatMap((slot) => flatTopHexPortCorners.map((corner) => flatTopHexSlotPortIdentity(slot, corner).junctionId)))

/**
 * Temporary placement bridge for the inspiration environment. It intentionally
 * consumes real Studio nodes while keeping the production x/y positioning and
 * persistence model untouched.
 */
export const defaultHoneycombPrototypePlacements: HoneycombNodePlacement[] = [
  { nodeId: 'r-001', q: 0, r: 2 },
  { nodeId: 'r-005', q: 0, r: 0 },
  { nodeId: 'r-002', q: 1, r: 2 },
  { nodeId: 'r-004', q: 4, r: 2 },
]

function readDimension(style: CSSStyleDeclaration, propertyName: string): number {
  const value = Number.parseFloat(style.getPropertyValue(propertyName))
  if (Number.isNaN(value)) throw new Error(`Honeycomb geometry probe property ${propertyName} must resolve to a pixel value.`)
  return value
}

function useHoneycombGeometry(geometryProbe: HTMLElement | null): HoneycombGeometry | null {
  const [geometry, setGeometry] = useState<HoneycombGeometry | null>(null)

  useLayoutEffect(() => {
    if (!geometryProbe) return
    const style = getComputedStyle(geometryProbe)
    setGeometry({
      slotHeight: readDimension(style, 'height'),
      slotSide: readDimension(style, 'padding-left'),
      slotWidth: readDimension(style, 'width'),
    })
  }, [geometryProbe])

  return geometry
}

function flatTopHexBoundaryPoints({ slotHeight, slotWidth }: HoneycombGeometry): string {
  return [
    `${slotWidth / 4},0`,
    `${slotWidth * 3 / 4},0`,
    `${slotWidth},${slotHeight / 2}`,
    `${slotWidth * 3 / 4},${slotHeight}`,
    `${slotWidth / 4},${slotHeight}`,
    `0,${slotHeight / 2}`,
  ].join(' ')
}

function stageMetrics(slots: HoneycombSlot[], geometry: HoneycombGeometry) {
  const positions = slots.map((slot) => flatTopHexPosition(slot, geometry))
  const left = Math.min(...positions.map((point) => point.x))
  const top = Math.min(...positions.map((point) => point.y))
  const right = Math.max(...positions.map((point) => point.x + geometry.slotWidth))
  const bottom = Math.max(...positions.map((point) => point.y + geometry.slotHeight))
  const gutter = 32
  return {
    left,
    top,
    height: bottom - top + gutter * 2,
    width: right - left + gutter * 2,
    gutter,
  }
}

function useHoneycombWorkflowCanvasInspiration({ edges, nodes, onNodeSelect, placements = defaultHoneycombPrototypePlacements, scene, model }: HoneycombWorkflowCanvasInspirationProps): CanvasModelSnapshot {
  const [selectedSlotId, setSelectedSlotId] = useState<string | null>(null)
  const [hoveredNodeSlot, setHoveredNodeSlot] = useState<HoveredNodeSlot | null>(null)
  const [geometryProbe, setGeometryProbe] = useState<HTMLElement | null>(null)
  const interactionAdapter = useMemo(() => model ? new HoneycombWorkflowModelAdapter(model) : undefined, [model])
  const nodesById = useMemo(() => new Map(nodes.map((node) => [node.id, node])), [nodes])
  const placementBySlotId = useMemo(() => new Map(placements.map((placement) => [flatTopHexSlotId(placement), placement])), [placements])
  const placementByNodeId = useMemo(() => new Map(placements.map((placement) => [placement.nodeId, placement])), [placements])
  const geometry = useHoneycombGeometry(geometryProbe)
  const metrics = useMemo(() => geometry ? stageMetrics(prototypeSlots, geometry) : null, [geometry])
  const positionedSlots = useMemo(() => geometry ? prototypeSlots.map((slot) => ({ ...slot, point: flatTopHexPosition(slot, geometry) })) : [], [geometry])
  const boundaryPoints = useMemo(() => geometry ? flatTopHexBoundaryPoints(geometry) : '', [geometry])
  const immediateRouteSeams = useMemo(() => {
    if (!geometry) return { controlsBySourceSlotId: new Map<string, ImmediateRouteSeam[]>(), sidesBySlotId: new Map<string, Set<FlatTopHexFrameSide>>() }

    const sidesBySlotId = new Map<string, Set<FlatTopHexFrameSide>>()
    const controlsBySourceSlotId = new Map<string, ImmediateRouteSeam[]>()
    const addSide = (slotId: string, side: FlatTopHexFrameSide) => {
      const sides = sidesBySlotId.get(slotId) ?? new Set<FlatTopHexFrameSide>()
      sides.add(side)
      sidesBySlotId.set(slotId, sides)
    }

    edges.forEach((edge) => {
      const sourceSlot = placementByNodeId.get(edge.source)
      const targetSlot = placementByNodeId.get(edge.target)
      if (!sourceSlot || !targetSlot) return
      const sharedSides = flatTopHexSharedFrameSides(sourceSlot, targetSlot)
      if (!sharedSides) return

      const sourceSlotId = flatTopHexSlotId(sourceSlot)
      const targetSlotId = flatTopHexSlotId(targetSlot)
      addSide(sourceSlotId, sharedSides.first)
      addSide(targetSlotId, sharedSides.second)

      const midpoint = flatTopHexFrameSideMidpoint(sharedSides.first, geometry)
      const sourcePosition = flatTopHexPosition(sourceSlot, geometry)
      const targetPosition = flatTopHexPosition(targetSlot, geometry)
      const control: ImmediateRouteSeam = {
        pathId: `${edge.source}->${edge.target}`,
        destinationAngle: Math.atan2(targetPosition.y - sourcePosition.y, targetPosition.x - sourcePosition.x) * 180 / Math.PI,
        id: `route-side-control/${edge.source}->${edge.target}`,
        point: midpoint,
        sourceNodeId: edge.source,
        sourceSlotId,
        targetSlotId,
      }
      const controls = controlsBySourceSlotId.get(sourceSlotId) ?? []
      if (!controls.some(({ id }) => id === control.id)) controls.push(control)
      controlsBySourceSlotId.set(sourceSlotId, controls)
    })

    return { controlsBySourceSlotId, sidesBySlotId }
  }, [edges, geometry, placementByNodeId])
  const routeModel = useMemo(() => {
    if (!geometry || !metrics) return {
      assignedRoles: new Map<string, PortTrafficRole>(),
      connectionCounts: new Map<string, number>(),
      routes: [],
      turnPortIds: new Set<string>(),
    }
    const assignedRoles = new Map<string, PortTrafficRole>()
    const connectionCounts = new Map<string, number>()
    const turnPortIds = new Set<string>()
    const routes = edges.flatMap((edge) => {
      const sourceSlot = placementByNodeId.get(edge.source)
      const targetSlot = placementByNodeId.get(edge.target)
      if (!sourceSlot || !targetSlot) return []
      const route = graphRouteVariant.plan({
        assignedRoles,
        edge,
        geometry,
        slots: workflowSlots,
        sourceSlot,
        targetSlot,
      })
      if (!route) return []
      route.portStates.forEach(({ portId, role }) => assignedRoles.set(portId, role))
      new Set(route.ports.map((port) => port.id)).forEach((portId) => connectionCounts.set(portId, (connectionCounts.get(portId) ?? 0) + 1))
      route.turnPortIds.forEach((portId) => turnPortIds.add(portId))
      return [{
        ...route,
        points: route.ports.map((port) => ({ x: port.point.x - metrics.left + metrics.gutter, y: port.point.y - metrics.top + metrics.gutter })),
      }]
    })
    return { assignedRoles, connectionCounts, routes, turnPortIds }
  }, [edges, geometry, metrics, placementByNodeId])

  return new HoneycombCanvasModelSnapshot({
    boundaryPoints,
    geometry,
    hoveredNodeSlot,
    immediateRouteSeams,
    interactionAdapter,
    metrics,
    nodesById,
    onNodeSelect,
    placementBySlotId,
    positionedSlots,
    routeModel,
    scene,
    model,
    selectedSlotId,
    setGeometryProbe,
    setHoveredNodeSlot,
    setSelectedSlotId,
    workflowJunctionIds,
  })
}

/**
 * Snapshot-producing compatibility entry point retained for the Scene Builder
 * and standalone consumers. The optional legacy scene fallback remains part of
 * this public contract.
 */
export function HoneycombWorkflowCanvasInspiration(props: HoneycombWorkflowCanvasInspirationProps): CanvasModelSnapshot {
  return useHoneycombWorkflowCanvasInspiration(props)
}

/** A React surface for hosts that render Honeycomb directly. */
export function HoneycombWorkflowCanvas(props: HoneycombWorkflowCanvasInspirationProps) {
  return useHoneycombWorkflowCanvasInspiration(props).getCanvas()
}

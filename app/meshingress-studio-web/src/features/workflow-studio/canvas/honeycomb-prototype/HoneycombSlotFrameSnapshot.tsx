import { Fragment, type JSX } from 'react'
import type { WorkflowNode } from '../../types'
import {
  flatTopHexCornerInwardAngle,
  flatTopHexCornerPortPoint,
  flatTopHexFrameSides,
  flatTopHexPortCorners,
  flatTopHexSlotPortIdentity,
  type FlatTopHexFrameSide,
  type HoneycombGeometry,
  type HoneycombPoint,
} from './honeycomb-grid'
import type {
  HoneycombRoutePortHook,
  HoneycombRoutePortState,
  PortTrafficRole,
} from './route-variants/honeycomb-route-variant'
import type { WorkflowCanvasScene } from '../../states/workflow-canvas-scene'
import {SlotModelSnapshot, type NodeModelSnapshot, type NodeOverlay, SlotOccupancySnapshot, SlotDropStateSnapshot, type WorkflowModelSnapshot} from '../../states/WorkflowStudioState'
import { HoneycombNodeSnapshot } from './HoneycombNodeSnapshot'
import type { HoneycombWorkflowModelAdapter } from './adapters/HoneycombWorkflowModelAdapter'
import { readableSlotLabel, type HoneycombNodePlacement, type HoneycombSlot, type HoveredNodeSlot, type ImmediateRouteSeam } from './honeycomb-types'

export const hexCornerPortPolygonPoints = '0,0 2.375,-4.113612301986423 9.5,0 2.375,4.113612301986423'

export function flatTopHexFrameSidePath(side: FlatTopHexFrameSide, { slotHeight, slotWidth }: HoneycombGeometry): string {
  const vertices = {
    'top-left': `${slotWidth / 4} 0`,
    'top-right': `${slotWidth * 3 / 4} 0`,
    right: `${slotWidth} ${slotHeight / 2}`,
    'bottom-right': `${slotWidth * 3 / 4} ${slotHeight}`,
    'bottom-left': `${slotWidth / 4} ${slotHeight}`,
    left: `0 ${slotHeight / 2}`,
  }
  const sideVertices: Record<FlatTopHexFrameSide, readonly [keyof typeof vertices, keyof typeof vertices]> = {
    top: ['top-left', 'top-right'],
    'upper-right': ['top-right', 'right'],
    'lower-right': ['right', 'bottom-right'],
    bottom: ['bottom-right', 'bottom-left'],
    'lower-left': ['bottom-left', 'left'],
    'upper-left': ['left', 'top-left'],
  }
  const [start, end] = sideVertices[side]
  return `M ${vertices[start]} L ${vertices[end]}`
}

export interface HoneycombSlotFrameSnapshotProps {
  slot: HoneycombSlot & { point: HoneycombPoint }
  metrics: { left: number; top: number; height: number; width: number; gutter: number }
  geometry: HoneycombGeometry
  immediateRouteSeams: { controlsBySourceSlotId: Map<string, ImmediateRouteSeam[]>; sidesBySlotId: Map<string, Set<FlatTopHexFrameSide>> }
  boundaryPoints: string
  routeModel: {
    assignedRoles: Map<string, PortTrafficRole>
    connectionCounts: Map<string, number>
    routes: { points: { x: number, y: number }[], id: string, ports: HoneycombRoutePortHook[], portStates: HoneycombRoutePortState[], turnPortIds: string[], turnSlotIds: string[], }[]
    turnPortIds: Set<string>
  }
  workflowJunctionIds: Set<string>
  placementBySlotId?: Map<string, HoneycombNodePlacement>
  nodesById?: Map<string, WorkflowNode>
  selectedSlotId?: string | null
  hoveredNodeSlot?: HoveredNodeSlot | null
  scene?: WorkflowCanvasScene
  model?: WorkflowModelSnapshot
  interactionAdapter?: HoneycombWorkflowModelAdapter
  setSelectedSlotId: (value: (((prevState: (string | null)) => (string | null)) | string | null)) => void
  setHoveredNodeSlot: (value: (((prevState: (HoveredNodeSlot | null)) => (HoveredNodeSlot | null)) | HoveredNodeSlot | null)) => void

  // Optional manual overrides
  placement?: HoneycombNodePlacement
  node?: WorkflowNode
  selected?: boolean
  nodeKind?: string
  isHoveredNodeSlot?: boolean
  ownedRouteSideControls?: ImmediateRouteSeam[]
  slotLabel?: string
  nodeModel?: NodeModelSnapshot | null
}

let currentSlotFrameParams: HoneycombSlotFrameSnapshotProps | null = null

export class HoneycombSlotFrameSnapshot extends SlotModelSnapshot {
  private readonly props: HoneycombSlotFrameSnapshotProps
  private readonly placement?: HoneycombNodePlacement
  private readonly node?: WorkflowNode
  private readonly selected: boolean
  private readonly nodeKind: string
  private readonly isHoveredNodeSlot: boolean
  private readonly ownedRouteSideControls: ImmediateRouteSeam[]
  private readonly slotLabel: string

  constructor(props: HoneycombSlotFrameSnapshotProps) {
    const placement = props.placement ?? (props.slot.kind === 'workflow' ? props.placementBySlotId?.get(props.slot.id) : undefined)
    const node = props.node ?? (placement ? props.nodesById?.get(placement.nodeId) : undefined)
    const selected = props.selected ?? (props.selectedSlotId === props.slot.id)
    const nodeKind = props.nodeKind ?? (node ? `${node.toolId?.split('.').map((p) => p.charAt(0)).join('•') ?? node.toolId} ` : '')
    const isHoveredNodeSlot = props.isHoveredNodeSlot ?? (node !== undefined && props.hoveredNodeSlot?.slotId === props.slot.id)
    const ownedRouteSideControls = props.ownedRouteSideControls ?? (props.immediateRouteSeams.controlsBySourceSlotId.get(props.slot.id) ?? [])
    const slotLabel = props.slotLabel ?? readableSlotLabel(props.slot)
    const nodeModel = props.nodeModel ?? (node ? new HoneycombNodeSnapshot({ scene: props.scene, model: props.model, node, geometry: props.geometry, boundaryPoints: props.boundaryPoints, nodeKind }) : null)
    const resolvedProps: HoneycombSlotFrameSnapshotProps = {
      ...props,
      isHoveredNodeSlot,
      node,
      nodeKind,
      nodeModel,
      ownedRouteSideControls,
      placement,
      selected,
      slotLabel,
    }

    const prevParams = currentSlotFrameParams
    currentSlotFrameParams = resolvedProps
    try {
      super(nodeModel)
    } finally {
      currentSlotFrameParams = prevParams
    }
    this.props = resolvedProps
    this.placement = placement
    this.node = node
    this.selected = selected
    this.nodeKind = nodeKind
    this.isHoveredNodeSlot = isHoveredNodeSlot
    this.ownedRouteSideControls = ownedRouteSideControls
    this.slotLabel = slotLabel
  }

  public getPlacement(): HoneycombNodePlacement | undefined {
    return this.placement
  }

  public getNodeData(): WorkflowNode | undefined {
    return this.node
  }

  public getSlotKindWorkflow(): JSX.Element | null {
    const p = this.props ?? currentSlotFrameParams
    if (!p) return null
    const { boundaryPoints, geometry, interactionAdapter, scene, model, setHoveredNodeSlot, setSelectedSlotId, slot } = p
    const selected = this.selected ?? p.selected
    const slotLabel = this.slotLabel ?? p.slotLabel
    const nodeKind = this.nodeKind ?? p.nodeKind
    const nodeModel = this.nodeModel ?? p.nodeModel
    const node = this.node ?? nodeModel?.getNodeData() ?? p.node
    const setNodeOverlay = (phase: NodeOverlay, active: boolean = true) => {
      if (!node) return
      if (interactionAdapter) interactionAdapter.setNodeOverlay(node.id, phase, active)
      else nodeModel?.overlay.setPhase(phase, active)
    }

    return (
      <button
        aria-label={node ? `${node.title} node slot` : `Empty workflow slot ${slot.id}`}
        className={`hwp-slot${node ? ' hwp-slot--occupied' : ' hwp-slot--empty'}${selected ? ' hwp-slot--selected' : ''}`}
        data-slot-id={slot.id}
        {...(model?.getSlotModel(slot.id)?.getDataAttributes() ?? scene?.slots[slot.id]?.attributes)}
        onClick={() => {
          setSelectedSlotId(slot.id)
          if (node) {
            setNodeOverlay('selected')
          }
        }}
        onPointerEnter={(event) => {
          if (!node) return
          setNodeOverlay('hovered')
          setHoveredNodeSlot({ clientX: event.clientX, clientY: event.clientY, label: slotLabel, slotId: slot.id })
        }}
        onPointerLeave={() => {
          setHoveredNodeSlot((current) => current?.slotId === slot.id ? null : current)
          if (node) {
            setNodeOverlay('hovered', false)
          }
        }}
        onFocus={() => {
          if (node) {
            setNodeOverlay('focused')
          }
        }}
        onBlur={() => {
          if (node) {
            setNodeOverlay('focused', false)
          }
        }}
        onPointerMove={(event) => {
          if (!node) return
          setHoveredNodeSlot((current) => current?.slotId === slot.id
            ? { ...current, clientX: event.clientX, clientY: event.clientY }
            : current)
        }}
        type="button"
      >
        <span className="hwp-slot-content">
          {nodeModel ? (nodeModel.getNode() ?? (node ? new HoneycombNodeSnapshot({ scene, model, node, geometry, boundaryPoints, nodeKind }).getNode() : null)) : (
            <span className="hwp-slot-empty-label">{slotLabel}</span>
          )}
        </span>
      </button>
    )
  }

  public getSlotKindCanvasEnd(): JSX.Element | null {
    const p = this.props ?? currentSlotFrameParams
    if (!p) return null
    const { slot } = p
    return <span aria-hidden="true" className="hwp-slot hwp-slot--canvas-end" data-slot-id={slot.id} />
  }

  protected override snapshot(_nodeModel: NodeModelSnapshot | null, _occupancy: SlotOccupancySnapshot, _dropState: SlotDropStateSnapshot): JSX.Element | null {
    const p = this.props ?? currentSlotFrameParams
    if (!p) return null
    const { geometry, immediateRouteSeams, metrics, routeModel, slot, workflowJunctionIds, } = p

    const isHoveredNodeSlot = this.isHoveredNodeSlot ?? p.isHoveredNodeSlot
    const ownedRouteSideControls = this.ownedRouteSideControls ?? p.ownedRouteSideControls

    return (
      <span
        className={`hwp-slot-frame${ownedRouteSideControls.length > 0 ? ' hwp-slot-frame--route-control-owner' : ''}`}
        key={slot.id}
        style={{ left: slot.point.x - metrics.left + metrics.gutter, top: slot.point.y - metrics.top + metrics.gutter }}
      >
        <svg aria-hidden="true" className="hwp-slot-frame-boundary" preserveAspectRatio="none" viewBox={`0 0 ${geometry.slotWidth} ${geometry.slotHeight}`}>
          {flatTopHexFrameSides.map((side) => (
            <path
              className="hwp-slot-frame-side"
              d={flatTopHexFrameSidePath(side, geometry)}
              data-frame-side={side}
              data-route-immediate-seam={immediateRouteSeams.sidesBySlotId.get(slot.id)?.has(side) || undefined}
              key={side}
            />
          ))}
        </svg>
        {slot.kind === 'canvas-end' ? this.getSlotKindCanvasEnd() : this.getSlotKindWorkflow()}

        {flatTopHexPortCorners.map((corner) => {
          const point = flatTopHexCornerPortPoint(corner, geometry)
          const identity = flatTopHexSlotPortIdentity(slot, corner)
          const availability = slot.kind === 'canvas-end' && !workflowJunctionIds.has(identity.junctionId) ? 'closed' : 'open'
          const isTurnPort = routeModel.turnPortIds.has(identity.slotPortId)
          const trafficRole = routeModel.assignedRoles.get(identity.slotPortId)
          const connectionCount = routeModel.connectionCounts.get(identity.slotPortId) ?? 0
          const junctionLabel = identity.junctionId.split("=").pop()
          return (
            <Fragment key={corner}>
              <svg
                aria-hidden="true"
                className="hwp-slot-port"
                data-junction-id={identity.junctionId}
                data-port-availability={availability}
                data-port-connection-count={connectionCount}
                data-slot-port={identity.slotPortId}
                data-slot-port-corner={corner}
                data-traffic-role={trafficRole}
                data-route-turn-allocation={isTurnPort || undefined}
                style={{ left: point.x, top: point.y }}
              >
                <polygon points={hexCornerPortPolygonPoints} transform={`rotate(${flatTopHexCornerInwardAngle(corner)})`} />
              </svg>
              {isHoveredNodeSlot && (
                <span aria-hidden="true" className={`hwp-slot-port-label hwp-slot-port-label--${corner}`} data-slot-port={identity.slotPortId} style={{ left: point.x, top: point.y }}>{junctionLabel}</span>
              )}
            </Fragment>
          )
        })}
        {ownedRouteSideControls.map((control) => (
          <span
            aria-hidden="true"
            className="hwp-route-side-control-anchor"
            data-owner-node-id={control.sourceNodeId}
            data-owner-slot-id={control.sourceSlotId}
            data-destination-angle={control.destinationAngle}
            data-route-side-control={control.id}
            data-target-slot-id={control.targetSlotId}
            key={control.id}
            style={{ left: control.point.x, top: control.point.y }}
          >
            <svg aria-hidden="true" className="hwp-route-side-control" preserveAspectRatio="none" viewBox="0 0 2 1.7320508075688772">
              <polygon points="0.5,0 1.5,0 2,0.8660254037844386 1.5,1.7320508075688772 0.5,1.7320508075688772 0,0.8660254037844386" />
              <path
                className="hwp-route-side-control-arrow"
                d="M 0.6 0.5160254037844386 L 0.95 0.8660254037844386 L 0.6 1.2160254037844386 M 1.05 0.5160254037844386 L 1.4 0.8660254037844386 L 1.05 1.2160254037844386"
                transform={`rotate(${control.destinationAngle} 1 0.8660254037844386)`}
              />
            </svg>
          </span>
        ))}
      </span>
    )
  }
}

export { HoneycombSlotFrameSnapshot as HoneycombSlotModelSnapshot, HoneycombSlotFrameSnapshot as HoneycombSlotSnapshot }

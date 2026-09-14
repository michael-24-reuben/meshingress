import type { JSX } from 'react'
import type { WorkflowNode } from '../../types'
import type { FlatTopHexFrameSide, HoneycombGeometry, HoneycombPoint } from './honeycomb-grid'
import type { HoneycombRoutePortHook, HoneycombRoutePortState, PortTrafficRole } from './route-variants/honeycomb-route-variant'
import type { WorkflowCanvasScene } from '../../states/workflow-canvas-scene'
import { CanvasModelSnapshot, type WorkflowModelSnapshot } from '../../states/WorkflowStudioState'
import { HoneycombSlotFrameSnapshot } from './HoneycombSlotFrameSnapshot'
import type { HoneycombWorkflowModelAdapter } from './adapters/HoneycombWorkflowModelAdapter'
import type { HoneycombNodePlacement, HoneycombSlot, HoveredNodeSlot, ImmediateRouteSeam } from './honeycomb-types'

export interface HoneycombCanvasModelSnapshotProps {
  scene?: WorkflowCanvasScene
  model?: WorkflowModelSnapshot
  interactionAdapter?: HoneycombWorkflowModelAdapter
  setGeometryProbe: (value: (((prevState: (HTMLElement | null)) => (HTMLElement | null)) | HTMLElement | null)) => void
  geometry: HoneycombGeometry | null
  metrics: null | { left: number; top: number; height: number; width: number; gutter: number }
  routeModel: {
    assignedRoles: Map<string, PortTrafficRole>
    connectionCounts: Map<string, number>
    routes: { points: { x: number, y: number }[], id: string, ports: HoneycombRoutePortHook[], portStates: HoneycombRoutePortState[], turnPortIds: string[], turnSlotIds: string[], }[]
    turnPortIds: Set<string>
  }
  positionedSlots: (HoneycombSlot & { point: HoneycombPoint })[]
  placementBySlotId: Map<string, HoneycombNodePlacement>
  nodesById: Map<string, WorkflowNode>
  selectedSlotId: string | null
  hoveredNodeSlot: HoveredNodeSlot | null
  immediateRouteSeams: { controlsBySourceSlotId: Map<string, ImmediateRouteSeam[]>; sidesBySlotId: Map<string, Set<FlatTopHexFrameSide>> }
  setSelectedSlotId: (value: (((prevState: (string | null)) => (string | null)) | string | null)) => void
  setHoveredNodeSlot: (value: (((prevState: (HoveredNodeSlot | null)) => (HoveredNodeSlot | null)) | HoveredNodeSlot | null)) => void
  boundaryPoints: string
  slotFrameSnapshots?: HoneycombSlotFrameSnapshot[]
  workflowJunctionIds: Set<string>
}

let currentCanvasParams: HoneycombCanvasModelSnapshotProps | null = null

export class HoneycombCanvasModelSnapshot extends CanvasModelSnapshot {
  private readonly props: HoneycombCanvasModelSnapshotProps

  constructor(props: HoneycombCanvasModelSnapshotProps) {
    const slotFrameSnapshots = props.slotFrameSnapshots ?? (
      props.geometry && props.metrics
        ? props.positionedSlots.map((slot) =>
            new HoneycombSlotFrameSnapshot({
              boundaryPoints: props.boundaryPoints,
              geometry: props.geometry!,
              hoveredNodeSlot: props.hoveredNodeSlot,
              immediateRouteSeams: props.immediateRouteSeams,
              interactionAdapter: props.interactionAdapter,
              metrics: props.metrics!,
              nodesById: props.nodesById,
              placementBySlotId: props.placementBySlotId,
              routeModel: props.routeModel,
              scene: props.scene,
              model: props.model,
              selectedSlotId: props.selectedSlotId,
              setHoveredNodeSlot: props.setHoveredNodeSlot,
              setSelectedSlotId: props.setSelectedSlotId,
              slot,
              workflowJunctionIds: props.workflowJunctionIds,
            }),
          )
        : []
    )

    const resolvedProps: HoneycombCanvasModelSnapshotProps = {
      ...props,
      slotFrameSnapshots,
    }

    const prevParams = currentCanvasParams
    currentCanvasParams = resolvedProps
    try {
      super(slotFrameSnapshots)
    } finally {
      currentCanvasParams = prevParams
    }
    this.props = resolvedProps
  }

  protected override snapshot(): JSX.Element | null {
    const p = this.props ?? currentCanvasParams
    if (!p) return null
    const {
      geometry,
      hoveredNodeSlot,
      metrics,
      routeModel,
      scene,
      model,
      setGeometryProbe,
    } = p
    const canvasAttributes = {
      ...scene?.workflow.attributes,
      ...scene?.canvas.attributes,
      ...model?.getWorkflowState().getDataAttributes(),
      ...model?.getCanvasModel().getDataAttributes(),
    }

    return (
      <section aria-label="Honeycomb workflow canvas inspiration" className="hwp-canvas" {...canvasAttributes}>
        <span aria-hidden="true" className="hwp-geometry-probe" ref={setGeometryProbe} />
        {geometry && metrics && (
          <div className="hwp-canvas-scroll">
            <div className="hwp-stage" style={{ height: metrics.height, width: metrics.width }}>
              <svg aria-hidden="true" className="hwp-path-layer" height={metrics.height} width={metrics.width}>
                {routeModel.routes.map((route) => (
                  <path
                    className="hwp-path"
                    d={route.points.map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x} ${point.y}`).join(' ')}
                    data-route-junction-ids={route.ports.map((port) => port.junctionId).join(' ')}
                    data-route-port-ids={route.ports.map((port) => port.id).join(' ')}
                    data-route-port-roles={route.portStates.map(({ portId, role }) => `${portId}:${role}`).join(' ')}
                    data-route-turn-port-ids={route.turnPortIds.join(' ')}
                    data-route-turn-slot-ids={route.turnSlotIds.join(' ')}
                    data-source-port={route.ports[0]?.id}
                    data-target-port={route.ports.at(-1)?.id}
                    key={route.id}
                    {...(model?.getPathModel(route.id)?.getDataAttributes() ?? scene?.paths[route.id]?.attributes)}
                  />
                ))}
              </svg>
              {this.getSlotModels().map((slotModel) => slotModel.getSlot())}
              {hoveredNodeSlot && (
                <span
                  aria-hidden="true"
                  className="hwp-slot-hover-tooltip"
                  data-slot-id={hoveredNodeSlot.slotId}
                  style={{ left: hoveredNodeSlot.clientX, top: hoveredNodeSlot.clientY }}
                >
                  {hoveredNodeSlot.label}
                </span>
              )}
            </div>
          </div>
        )}
      </section>
    )
  }
}

export { HoneycombCanvasModelSnapshot as HoneycombCanvasSnapshot }

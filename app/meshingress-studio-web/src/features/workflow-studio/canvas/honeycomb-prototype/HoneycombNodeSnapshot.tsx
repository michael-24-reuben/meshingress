import type { JSX } from 'react'
import type { WorkflowNode } from '../../types'
import type { HoneycombGeometry } from './honeycomb-grid'
import type { WorkflowCanvasScene } from '../../states/workflow-canvas-scene'
import { NodeConnectionStateSnapshot, NodeExecutionStateSnapshot, NodeModelSnapshot, NodeOverlaySnapshot, type WorkflowModelSnapshot } from '../../states/WorkflowStudioState'

/**
 * A snapshot of a honeycomb node.
 *
 * <table>
 *   <thead>
 *     <tr>
 *       <th>States</th>
 *       <th>Completed</th>
 *       <th>Usages</th>
 *       <th>Calls</th>
 *     </tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <td>nodeExecutionState.idle</td>
 *       <td></td>
 *       <td>0</td>
 *       <td>NodeExecutionStateSnapshot#setExecutionIdle</td>
 *     </tr>
 *     <tr>
 *       <td>nodeExecutionState.running</td>
 *       <td></td>
 *       <td>0</td>
 *       <td>NodeExecutionStateSnapshot#setExecutionRunning</td>
 *     </tr>
 *     <tr>
 *       <td>nodeExecutionState.success</td>
 *       <td></td>
 *       <td>0</td>
 *       <td>NodeExecutionStateSnapshot#setExecutionSuccess</td>
 *     </tr>
 *     <tr>
 *       <td>nodeExecutionState.warning</td>
 *       <td></td>
 *       <td>0</td>
 *       <td>NodeExecutionStateSnapshot#setExecutionWarning</td>
 *     </tr>
 *     <tr>
 *       <td>nodeExecutionState.error</td>
 *       <td></td>
 *       <td>0</td>
 *       <td>NodeExecutionStateSnapshot#setExecutionError</td>
 *     </tr>
 *     <tr>
 *       <td>nodeExecutionState.disabled</td>
 *       <td></td>
 *       <td>0</td>
 *       <td>NodeExecutionStateSnapshot#setExecutionDisabled</td>
 *     </tr>
 *     <tr>
 *       <td>nodeExecutionState.unavailable</td>
 *       <td></td>
 *       <td>0</td>
 *       <td>NodeExecutionStateSnapshot#setExecutionUnavailable</td>
 *     </tr>
 *     <tr>
 *       <td>nodeConnectionState.isolated</td>
 *       <td></td>
 *       <td>0</td>
 *       <td>NodeConnectionStateSnapshot#setConnectionIsolated</td>
 *     </tr>
 *     <tr>
 *       <td>nodeConnectionState.connected</td>
 *       <td></td>
 *       <td>0</td>
 *       <td>NodeConnectionStateSnapshot#setConnectionConnected</td>
 *     </tr>
 *     <tr>
 *       <td>nodeConnectionState.missing-required-input</td>
 *       <td></td>
 *       <td>0</td>
 *       <td>NodeConnectionStateSnapshot#setConnectionMissingRequiredInput</td>
 *     </tr>
 *     <tr>
 *       <td>nodeConnectionState.blocked-by-upstream-error</td>
 *       <td></td>
 *       <td>0</td>
 *       <td>NodeConnectionStateSnapshot#setConnectionBlockedByUpstreamError</td>
 *     </tr>
 *     <tr>
 *       <td>nodeConnectionState.stale-output</td>
 *       <td></td>
 *       <td>0</td>
 *       <td>NodeConnectionStateSnapshot#setConnectionStaleOutput</td>
 *     </tr>
 *     <tr>
 *       <td>nodeOverlay.selected</td>
 *       <td></td>
 *       <td>1</td>
 *       <td>NodeOverlaySnapshot#setOverlaySelected</td>
 *     </tr>
 *     <tr>
 *       <td>nodeOverlay.hovered</td>
 *       <td>true</td>
 *       <td>2</td>
 *       <td>NodeOverlaySnapshot#setOverlayHovered</td>
 *     </tr>
 *     <tr>
 *       <td>nodeOverlay.focused</td>
 *       <td>true</td>
 *       <td>2</td>
 *       <td>NodeOverlaySnapshot#setOverlayFocused</td>
 *     </tr>
 *     <tr>
 *       <td>nodeOverlay.bookmarked</td>
 *       <td></td>
 *       <td>0</td>
 *       <td>NodeOverlaySnapshot#setOverlayBookmarked</td>
 *     </tr>
 *     <tr>
 *       <td>nodeOverlay.dragging</td>
 *       <td></td>
 *       <td>0</td>
 *       <td>NodeOverlaySnapshot#setOverlayDragging</td>
 *     </tr>
 *     <tr>
 *       <td>nodeOverlay.lifted-from-origin</td>
 *       <td></td>
 *       <td>0</td>
 *       <td>NodeOverlaySnapshot#setOverlayLiftedFromOrigin</td>
 *     </tr>
 *     <tr>
 *       <td>nodeOverlay.has-unsaved-changes</td>
 *       <td></td>
 *       <td>0</td>
 *       <td>NodeOverlaySnapshot#setOverlayHasUnsavedChanges</td>
 *     </tr>
 *     <tr>
 *       <td>nodeOverlay.unreachable</td>
 *       <td>true</td>
 *       <td>1</td>
 *       <td>NodeOverlaySnapshot#setOverlayUnreachable</td>
 *     </tr>
 *     <tr>
 *       <td>nodeOverlay.valid</td>
 *       <td></td>
 *       <td>0</td>
 *       <td>NodeOverlaySnapshot#setOverlayValid</td>
 *     </tr>
 *     <tr>
 *       <td>nodeOverlay.invalid</td>
 *       <td>true</td>
 *       <td>1</td>
 *       <td>NodeOverlaySnapshot#setOverlayInvalid</td>
 *     </tr>
 *   </tbody>
 * </table>
 */
export interface HoneycombNodeSnapshotProps {
  scene?: WorkflowCanvasScene
  model?: WorkflowModelSnapshot
  node: WorkflowNode
  geometry: HoneycombGeometry
  boundaryPoints: string
  nodeKind: string
}

let currentNodeParams: HoneycombNodeSnapshotProps | null = null

export class HoneycombNodeSnapshot extends NodeModelSnapshot {
  private readonly scene?: WorkflowCanvasScene
  private readonly model?: WorkflowModelSnapshot
  private readonly geometry: HoneycombGeometry
  private readonly boundaryPoints: string
  private readonly nodeKind: string

  constructor(props: HoneycombNodeSnapshotProps) {
    const prevParams = currentNodeParams
    currentNodeParams = props
    try {
      super(props.node)
    } finally {
      currentNodeParams = prevParams
    }
    this.scene = props.scene
    this.model = props.model
    this.geometry = props.geometry
    this.boundaryPoints = props.boundaryPoints
    this.nodeKind = props.nodeKind
  }

  protected override snapshot(_execution: NodeExecutionStateSnapshot, _connection: NodeConnectionStateSnapshot, _overlay: NodeOverlaySnapshot): JSX.Element | null {
    const scene = this.scene ?? currentNodeParams?.scene
    const model = this.model ?? currentNodeParams?.model
    const node = this.data ?? currentNodeParams?.node
    const geometry = this.geometry ?? currentNodeParams?.geometry
    const boundaryPoints = this.boundaryPoints ?? currentNodeParams?.boundaryPoints
    const nodeKind = this.nodeKind ?? currentNodeParams?.nodeKind

    if (!node || !geometry) return null

    return (
      <span className="hwp-node" {...(model?.getNodeModel(node.id)?.getDataAttributes() ?? scene?.nodes[node.id]?.attributes)}>
        <svg aria-hidden="true" className="hwp-node-boundary" preserveAspectRatio="none" viewBox={`0 0 ${geometry.slotWidth} ${geometry.slotHeight}`}>
          <polygon points={boundaryPoints} />
        </svg>
        <span className="hwp-node-content">
          <span aria-hidden="true" className="hwp-node-layout-grid">
            {[1, 5, 6].map((row) => <span className="hwp-node-layout-grid-cell" data-grid-column="full" key={`full-${row}`} style={{ gridColumn: '1 / -1', gridRow: row }} />)}
            <span className="hwp-node-layout-grid-half hwp-node-layout-grid-half--left">
              {[1, 2, 3].map((row) => <span className="hwp-node-layout-grid-cell" data-grid-column="1" key={`left-${row}`} style={{ gridColumn: 1, gridRow: row }} />)}
              <span className="hwp-node-layout-grid-cell" data-grid-column="2" style={{ gridColumn: 2, gridRow: '1 / span 3' }} />
            </span>
            {[2, 3].map((row) => {
              const isImageGuide = row === 3

              return <span className={`hwp-node-layout-grid-cell${isImageGuide ? ' hwp-node-layout-grid-cell--image' : ''}`} data-grid-column="3" key={`center-${row}`}
                style={{ gridColumn: 2, gridRow: isImageGuide ? '3 / span 2' : row }} />
            })}
            <span className="hwp-node-layout-grid-half hwp-node-layout-grid-half--right">
              <span className="hwp-node-layout-grid-cell" data-grid-column="4" style={{ gridColumn: 1, gridRow: '1 / span 3' }} />
              {[1, 2, 3].map((row) => <span className="hwp-node-layout-grid-cell" data-grid-column="5" key={`right-${row}`} style={{ gridColumn: 2, gridRow: row }} />)}
            </span>
          </span>
          <span className="hwp-node-id">{node.id}</span>
          <div className="hwp-node-kind">{nodeKind}•{node.functionName}</div>
          <img alt="" className="hwp-node-image" src="/favicon.svg" />
          <span className="hwp-node-title">{node.title}</span>
        </span>
      </span>
    )
  }
}

export { HoneycombNodeSnapshot as HoneycombNodeModelSnapshot }

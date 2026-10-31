import { defaultHoneycombPrototypePlacements, type HoneycombNodePlacement } from '../../canvas/honeycomb-prototype/HoneycombWorkflowCanvasInspiration'
import { StateCategory, type NodeConnectionState, type NodeExecutionState, type PathFlowState, type PathLifecycleState, type WorkflowModelSnapshot, type WorkflowState } from '../../states/WorkflowStudioState'
import type { NodeRunState, WorkflowEdge, WorkflowNode } from '../../types'

const temporaryPlacementSlots: ReadonlyArray<Pick<HoneycombNodePlacement, 'q' | 'r'>> = Array.from(
  { length: 7 },
  (_, q) => Array.from({ length: 5 }, (_, r) => ({ q: q - 1, r: r - 2 })),
).flat()

/**
 * Assigns the Studio's live nodes to temporary lattice positions. Production
 * x/y layout and persistence stay with the rectangular Workbench.
 */
export function createStudioWorkbenchTmpPlacements(nodes: WorkflowNode[]): HoneycombNodePlacement[] {
  const nodeIds = new Set(nodes.map(({ id }) => id))
  const preferred = defaultHoneycombPrototypePlacements.filter(({ nodeId }) => nodeIds.has(nodeId))
  const placedNodeIds = new Set(preferred.map(({ nodeId }) => nodeId))
  const occupiedSlots = new Set(preferred.map(({ q, r }) => `${q}/${r}`))
  const availableSlots = temporaryPlacementSlots.filter(({ q, r }) => !occupiedSlots.has(`${q}/${r}`))

  return [
    ...preferred,
    ...nodes
      .filter(({ id }) => !placedNodeIds.has(id))
      .map((node, index) => {
        const slot = availableSlots[index]
        if (!slot) throw new Error('Studio Workbench Tmp cannot place more than 35 workflow nodes in the temporary Honeycomb lattice.')
        return { nodeId: node.id, ...slot }
      }),
  ]
}

export function honeycombExecutionStateForRunState(runState: NodeRunState | undefined): NodeExecutionState {
  switch (runState) {
    case StateCategory.ofRunning:
      return StateCategory.ofRunning
    case StateCategory.ofSuccess:
      return StateCategory.ofSuccess
    case StateCategory.ofError:
      return StateCategory.ofError
    case StateCategory.ofIdle:
    default:
      return StateCategory.ofIdle
  }
}

/** Derives only the lifecycle states that are unambiguous from live Studio execution. */
export function honeycombWorkflowStateForRun(nodes: WorkflowNode[], runStates: Record<string, NodeRunState>, running: boolean): WorkflowState {
  if (running) return StateCategory.ofExecuting
  const states = nodes.map(({ id }) => runStates[id])
  if (states.some((state) => state === StateCategory.ofError)) return StateCategory.ofFailed
  if (nodes.length > 0 && states.every((state) => state === StateCategory.ofSuccess)) return StateCategory.ofCompleted
  return StateCategory.ofIncomplete
}

/** A node is connected when it participates in the live Studio edge topology. */
export function honeycombNodeConnectionStateForEdges(nodeId: string, edges: WorkflowEdge[]): NodeConnectionState {
  return edges.some(({ source, target }) => source === nodeId || target === nodeId)
    ? StateCategory.ofConnected
    : StateCategory.ofIsolated
}

export interface HoneycombPathState {
  pathLifecycleState: PathLifecycleState
  pathFlowState: PathFlowState
}

/**
 * Studio edges use the runtime's default output route. A successful source and
 * running target are evidence of active delivery; source execution alone is
 * not. These projections do not claim port-level telemetry or editing state.
 */
export function honeycombPathStatesForRun(
  nodes: WorkflowNode[],
  edges: WorkflowEdge[],
  runStates: Record<string, NodeRunState>,
  running: boolean,
): Map<string, HoneycombPathState> {
  const nodesById = new Map(nodes.map((node) => [node.id, node]))
  const outgoing = new Map<string, string[]>()
  const incoming = new Map<string, string[]>()
  edges.forEach(({ source, target }) => {
    if (!nodesById.has(source) || !nodesById.has(target)) return
    outgoing.set(source, [...(outgoing.get(source) ?? []), target])
    incoming.set(target, [...(incoming.get(target) ?? []), source])
  })

  const canReach = (start: string, matches: (id: string) => boolean, adjacency: Map<string, string[]>): boolean => {
    const pending = [start]
    const visited = new Set<string>()
    while (pending.length) {
      const id = pending.pop()!
      if (visited.has(id)) continue
      visited.add(id)
      if (matches(id)) return true
      pending.push(...(adjacency.get(id) ?? []))
    }
    return false
  }

  return new Map(edges.map((edge) => {
    const source = nodesById.get(edge.source)
    const target = nodesById.get(edge.target)
    let pathLifecycleState: PathLifecycleState = StateCategory.ofConnected
    let pathFlowState: PathFlowState = StateCategory.ofIdle

    if (!source || !target) {
      pathLifecycleState = StateCategory.ofUnconnected
    } else if (!source.capabilities.acceptsOutput || !target.capabilities.acceptsInput
      || canReach(edge.target, (id) => id === edge.source, outgoing)) {
      pathLifecycleState = StateCategory.ofInvalid
    } else {
      const sourceState = runStates[edge.source] ?? StateCategory.ofIdle
      const targetState = runStates[edge.target] ?? StateCategory.ofIdle
      // A downstream path is blocked only while its source has not executed.
      // A failed node on another branch must not mark this path as blocked.
      if (sourceState === StateCategory.ofError
        || (sourceState === StateCategory.ofIdle
          && canReach(edge.source, (id) => runStates[id] === StateCategory.ofError, incoming))) {
        pathLifecycleState = StateCategory.ofBlocked
      }
      if (sourceState === StateCategory.ofError || targetState === StateCategory.ofError) {
        pathFlowState = StateCategory.ofError
      } else if (sourceState === StateCategory.ofSuccess && targetState === StateCategory.ofSuccess) {
        pathFlowState = StateCategory.ofSuccess
      } else if (running && sourceState === StateCategory.ofSuccess && targetState === StateCategory.ofRunning) {
        pathFlowState = StateCategory.ofActive
      }
    }
    return [`${edge.source}->${edge.target}`, { pathLifecycleState, pathFlowState }]
  }))
}

/** Applies each projection through the root contract, including resets. */
export function syncHoneycombPathStates(
  model: WorkflowModelSnapshot,
  nodes: WorkflowNode[],
  edges: WorkflowEdge[],
  runStates: Record<string, NodeRunState>,
  running: boolean,
): void {
  honeycombPathStatesForRun(nodes, edges, runStates, running).forEach((state, pathId) => {
    model.applyMutation({ target: { kind: 'path', pathId }, category: 'pathLifecycleState', phase: state.pathLifecycleState })
    model.applyMutation({ target: { kind: 'path', pathId }, category: 'pathFlowState', phase: state.pathFlowState })
  })
}

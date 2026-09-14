import { describe, expect, it } from 'vitest'
import { initialNodes } from '../features/workflow-studio/compilation/sample-workflow'
import { createWorkflowCanvasScene, parseWorkflowCanvasScene } from '../features/workflow-studio/states/workflow-canvas-scene'
import { HoneycombWorkflowModelAdapter } from '../features/workflow-studio/canvas/honeycomb-prototype/adapters/HoneycombWorkflowModelAdapter'
import {
  CanvasModelSnapshot,
  NodeModelSnapshot,
  PathModelSnapshot,
  SlotModelSnapshot,
  WorkflowModelSnapshot,
  WorkflowStateSnapshot,
  parseWorkflowStudioScene,
  type NodeConnectionStateSnapshot,
  type NodeExecutionStateSnapshot,
  type NodeOverlaySnapshot,
  type SlotDropStateSnapshot,
  type SlotOccupancySnapshot,
  type WorkflowStudioSceneChange,
  type WorkflowStudioSceneMutation,
} from '../features/workflow-studio/states/WorkflowStudioState'
import { WorkflowModelSnapshotFactory } from '../features/workflow-studio/states/factories/WorkflowModelSnapshotFactory'
import type { WorkflowNode } from '../features/workflow-studio/types'

class TestNodeModelSnapshot extends NodeModelSnapshot {
  public constructor(node: WorkflowNode) {
    super(node)
  }

  protected snapshot(
    _execution: NodeExecutionStateSnapshot,
    _connection: NodeConnectionStateSnapshot,
    _overlay: NodeOverlaySnapshot,
  ) {
    return null
  }
}

class TestSlotModelSnapshot extends SlotModelSnapshot {
  public constructor() {
    super(null)
  }

  protected snapshot(
    _node: NodeModelSnapshot | null,
    _occupancy: SlotOccupancySnapshot,
    _dropState: SlotDropStateSnapshot,
  ) {
    return null
  }
}

class TestCanvasModelSnapshot extends CanvasModelSnapshot {
  public constructor() {
    super()
  }

  protected snapshot() {
    return null
  }
}

describe('WorkflowModelSnapshot', () => {
  it('normalizes the same valid scene shape as the legacy parser without changing its input', () => {
    const nodeA = { ...initialNodes[0], capabilities: { ...initialNodes[0].capabilities } }
    const nodeB = { ...initialNodes[1], capabilities: { ...initialNodes[1].capabilities } }
    const source = createWorkflowCanvasScene({
      edges: [{ source: nodeA.id, target: nodeB.id }],
      nodes: [nodeA, nodeB],
      placements: [{ nodeId: nodeA.id, q: 0, r: 0 }],
    })
    source.nodes[nodeA.id].states.nodeOverlay = ['selected', 'hovered']
    source.nodes[nodeA.id].attributes['data-node-overlay'] = 'stale'

    const parsed = parseWorkflowStudioScene(source)

    expect(parsed).toEqual(parseWorkflowCanvasScene(source))
    expect(parsed).not.toBe(source)
    expect(parsed.nodes[nodeA.id]).not.toBe(source.nodes[nodeA.id])
    expect(parsed.nodes[nodeA.id].attributes).toMatchObject({
      'data-node-overlay': 'selected hovered',
    })
  })

  it('rejects a scene state that belongs to the wrong target', () => {
    const source = createWorkflowCanvasScene({ edges: [], nodes: [], placements: [] })
    source.canvas.states.nodeOverlay = ['hovered']

    expect(() => parseWorkflowStudioScene(source)).toThrow('nodeOverlay cannot be assigned to a canvas.')
    expect(() => parseWorkflowCanvasScene(source)).toThrow('nodeOverlay cannot be assigned to a canvas.')
  })

  it('routes Honeycomb node overlays through the stable workflow model', () => {
    const nodeA = { ...initialNodes[0], capabilities: { ...initialNodes[0].capabilities } }
    const workflow = new WorkflowModelSnapshotFactory().create({
      edges: [],
      nodes: [nodeA],
      placements: [{ nodeId: nodeA.id, q: 0, r: 0 }],
    })
    const adapter = new HoneycombWorkflowModelAdapter(workflow)
    const events: WorkflowStudioSceneChange[] = []
    const unsubscribe = workflow.subscribe((event) => events.push(event))

    try {
      adapter.setNodeOverlay(nodeA.id, 'hovered')

      expect(workflow.getNodeModel(nodeA.id)?.getDataAttributes()).toMatchObject({
        'data-node-overlay': 'hovered',
      })
      expect(events.at(-1)).toEqual({
        target: { kind: 'node', nodeId: nodeA.id },
        category: 'nodeOverlay',
        phase: 'hovered',
        active: true,
        activePhases: ['hovered'],
      })
    } finally {
      unsubscribe()
    }
  })

  it('creates Scene Builder model defaults without a tracker', () => {
    const nodeA = { ...initialNodes[0], capabilities: { ...initialNodes[0].capabilities } }
    const nodeB = { ...initialNodes[1], capabilities: { ...initialNodes[1].capabilities } }
    const pathId = `${nodeA.id}->${nodeB.id}`
    const workflow = new WorkflowModelSnapshotFactory().create({
      edges: [{ source: nodeA.id, target: nodeB.id }],
      nodes: [nodeA, nodeB],
      placements: [{ nodeId: nodeA.id, q: 0, r: 0 }],
    })

    expect(workflow.getCanvasModel().getDataAttributes()).toEqual({ 'data-canvas-interaction': 'idle' })
    expect(workflow.getWorkflowState().getDataAttributes()).toEqual({ 'data-workflow-state': 'incomplete' })
    expect(workflow.getSlotModel('slot/q0-r0')?.getDataAttributes()).toMatchObject({ 'data-slot-occupancy': 'occupied' })
    expect(workflow.getNodeModel(nodeA.id)?.getDataAttributes()).toMatchObject({
      'data-node-execution-state': 'idle',
      'data-node-connection-state': 'isolated',
      'data-accepts-input': String(nodeA.capabilities.acceptsInput),
      'data-accepts-output': String(nodeA.capabilities.acceptsOutput),
    })
    expect(workflow.getPathModel(pathId)?.getDataAttributes()).toEqual({
      'data-path-lifecycle-state': 'connected',
      'data-path-flow-state': 'idle',
    })
  })

  it('projects exclusive node selection and emits the selected node identity', () => {
    const nodeA = new TestNodeModelSnapshot({ ...initialNodes[0] })
    const nodeB = new TestNodeModelSnapshot({ ...initialNodes[1] })
    const path = new PathModelSnapshot({ source: nodeA.getNodeId()!, target: nodeB.getNodeId()! })
    const slot = new TestSlotModelSnapshot()
    const workflow = new WorkflowModelSnapshot({
      workflowState: new WorkflowStateSnapshot(),
      canvasModel: new TestCanvasModelSnapshot(),
      nodeModels: new Map([
        [nodeA.getNodeId()!, nodeA],
        [nodeB.getNodeId()!, nodeB],
      ]),
      pathModels: new Map([[path.getPathId(), path]]),
      slotModels: new Map([['slot/q0-r0', slot]]),
    })
    const events: WorkflowStudioSceneChange[] = []
    const unsubscribe = workflow.subscribe((event) => events.push(event))

    try {
      workflow.applyMutation({
        target: { kind: 'node', nodeId: nodeA.getNodeId()! },
        category: 'nodeOverlay',
        phase: 'selected',
      })
      workflow.applyMutation({
        target: { kind: 'node', nodeId: nodeB.getNodeId()! },
        category: 'nodeOverlay',
        phase: 'selected',
      })

      expect(nodeA.overlay.getDataAttributes()).toEqual({})
      expect(nodeB.overlay.getDataAttributes()).toEqual({
        'data-node-overlay': 'selected',
      })
      expect(events.at(-1)).toEqual({
        target: { kind: 'node', nodeId: nodeB.getNodeId() },
        category: 'nodeOverlay',
        phase: 'selected',
        active: true,
        activePhases: ['selected'],
      })
    } finally {
      unsubscribe()
    }
  })

  it('applies every phase mutation with its target identity and projected renderer attribute', () => {
    const nodeA = { ...initialNodes[0], capabilities: { ...initialNodes[0].capabilities } }
    const nodeB = { ...initialNodes[1], capabilities: { ...initialNodes[1].capabilities } }
    const pathId = `${nodeA.id}->${nodeB.id}`
    const slotId = 'slot/q0-r0'
    const workflow = new WorkflowModelSnapshotFactory().create({
      edges: [{ source: nodeA.id, target: nodeB.id }],
      nodes: [nodeA, nodeB],
      placements: [{ nodeId: nodeA.id, q: 0, r: 0 }],
    })
    const events: WorkflowStudioSceneChange[] = []
    const unsubscribe = workflow.subscribe((event) => events.push(event))

    const apply = (
      mutation: WorkflowStudioSceneMutation,
      getAttributes: () => Record<string, string>,
      expectedEvent: Omit<WorkflowStudioSceneChange, 'activePhases'>,
    ) => {
      workflow.applyMutation(mutation)
      expect(events.filter((event) => (
        event.category === mutation.category
        && event.phase === mutation.phase
        && event.active
      )).at(-1)).toEqual({
        ...expectedEvent,
        activePhases: [mutation.phase],
      })
      expect(getAttributes()).toMatchObject({
        [`data-${mutation.category.replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`)}`]: mutation.phase,
      })
    }

    try {
      apply(
        { target: { kind: 'canvas' }, category: 'canvasInteraction', phase: 'panning' },
        () => workflow.getCanvasModel().getDataAttributes(),
        { target: { kind: 'canvas' }, category: 'canvasInteraction', phase: 'panning', active: true },
      )
      apply(
        { target: { kind: 'workflow' }, category: 'workflowState', phase: 'executing' },
        () => workflow.getWorkflowState().getDataAttributes(),
        { target: { kind: 'workflow' }, category: 'workflowState', phase: 'executing', active: true },
      )
      apply(
        { target: { kind: 'slot', slotId }, category: 'slotOccupancy', phase: 'reserved' },
        () => workflow.getSlotModel(slotId)!.getDataAttributes(),
        { target: { kind: 'slot', slotId }, category: 'slotOccupancy', phase: 'reserved', active: true },
      )
      apply(
        { target: { kind: 'slot', slotId }, category: 'slotDropState', phase: 'valid-drop-target' },
        () => workflow.getSlotModel(slotId)!.getDataAttributes(),
        { target: { kind: 'slot', slotId }, category: 'slotDropState', phase: 'valid-drop-target', active: true },
      )
      apply(
        { target: { kind: 'node', nodeId: nodeA.id }, category: 'nodeExecutionState', phase: 'running' },
        () => workflow.getNodeModel(nodeA.id)!.getDataAttributes(),
        { target: { kind: 'node', nodeId: nodeA.id }, category: 'nodeExecutionState', phase: 'running', active: true },
      )
      apply(
        { target: { kind: 'node', nodeId: nodeA.id }, category: 'nodeConnectionState', phase: 'connected' },
        () => workflow.getNodeModel(nodeA.id)!.getDataAttributes(),
        { target: { kind: 'node', nodeId: nodeA.id }, category: 'nodeConnectionState', phase: 'connected', active: true },
      )
      apply(
        { target: { kind: 'node', nodeId: nodeA.id }, category: 'nodeOverlay', phase: 'hovered' },
        () => workflow.getNodeModel(nodeA.id)!.getDataAttributes(),
        { target: { kind: 'node', nodeId: nodeA.id }, category: 'nodeOverlay', phase: 'hovered', active: true },
      )
      apply(
        { target: { kind: 'path', pathId }, category: 'pathLifecycleState', phase: 'pending-validation' },
        () => workflow.getPathModel(pathId)!.getDataAttributes(),
        { target: { kind: 'path', pathId }, category: 'pathLifecycleState', phase: 'pending-validation', active: true },
      )
      apply(
        { target: { kind: 'path', pathId }, category: 'pathFlowState', phase: 'active' },
        () => workflow.getPathModel(pathId)!.getDataAttributes(),
        { target: { kind: 'path', pathId }, category: 'pathFlowState', phase: 'active', active: true },
      )
    } finally {
      unsubscribe()
    }
  })

})

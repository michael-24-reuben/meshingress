import { useEffect, useMemo, useReducer, useRef, useState } from 'react'
import { defaultHoneycombPrototypePlacements, HoneycombWorkflowCanvasInspiration } from './canvas/honeycomb-prototype/HoneycombWorkflowCanvasInspiration'
import { initialEdges, initialNodes } from './compilation/sample-workflow'
import type { WorkflowNode } from './types'
import './workflow-canvas-scene-builder.css'
import type {
  CanvasInteraction,
  CanvasModelSnapshot,
  NodeConnectionState,
  NodeExecutionState,
  NodeOverlay,
  PathFlowState,
  PathLifecycleState,
  SlotDropState,
  SlotOccupancy,
  WorkflowModelSnapshot,
  WorkflowState,
  WorkflowStudioSceneTargetKind,
  WorkflowStudioStateCategory,
} from './states/WorkflowStudioState'
import { workflowStudioStateAttributeName, WorkflowStudioCategoryValues, workflowStudioSceneTargetCategories } from './states/WorkflowStudioState'
import { WorkflowModelSnapshotFactory } from './states/factories/WorkflowModelSnapshotFactory'

const sceneNodes: WorkflowNode[] = [
  ...initialNodes,
  {
    id: 'r-005', title: 'New Node', kind: 'tool', iconKind: 'tool', toolId: 'prototype.placeholder', functionName: 'placeholder', output: 'prototype', x: 0, y: 0, arguments: {}, annotations: { audit: false, scopes: [] }, capabilities: { acceptsInput: true, acceptsOutput: true },
  },
]

const placements = defaultHoneycombPrototypePlacements

const targets: readonly WorkflowStudioSceneTargetKind[] = ['canvas', 'workflow', 'slot', 'node', 'path']

function targetIdsFor(target: WorkflowStudioSceneTargetKind): string[] {
  if (target === 'canvas' || target === 'workflow') return ['scene']
  if (target === 'slot') return placements.map(({ q, r }) => `slot/q${q}-r${r}`)
  if (target === 'node') return sceneNodes.map(({ id }) => id)
  return initialEdges.map(({ source, target: targetNode }) => `${source}->${targetNode}`)
}

export function WorkflowCanvasSceneBuilderPage() {
  const workflowModel = useRef<WorkflowModelSnapshot | null>(null)
  if (!workflowModel.current) workflowModel.current = new WorkflowModelSnapshotFactory().create({ edges: initialEdges, nodes: sceneNodes, placements })
  const [, requestRender] = useReducer((revision: number) => revision + 1, 0)
  const [target, setTarget] = useState<WorkflowStudioSceneTargetKind>('node')
  const [targetId, setTargetId] = useState('r-001')
  const [category, setCategory] = useState('nodeOverlay')
  const [state, setState] = useState('hovered')
  const [acceptsInput, setAcceptsInput] = useState(true)
  const [acceptsOutput, setAcceptsOutput] = useState(true)

  useEffect(() => {
    return workflowModel.current!.subscribe(requestRender)
  }, [])

  const targetIds = useMemo(() => targetIdsFor(target), [target])
  const categories = workflowStudioSceneTargetCategories[target]
  const states = WorkflowStudioCategoryValues[category as WorkflowStudioStateCategory] ?? []

  const changeTarget = (nextTarget: WorkflowStudioSceneTargetKind) => {
    const nextCategories = workflowStudioSceneTargetCategories[nextTarget]
    const nextCategory = nextCategories[0]
    setTarget(nextTarget)
    setCategory(nextCategory)
    setState(WorkflowStudioCategoryValues[nextCategory][0])
    setTargetId(targetIdsFor(nextTarget)[0])
  }

  const applyState = () => {
    const id = targetId
    const model = workflowModel.current!
    switch (category) {
      case 'canvasInteraction': model.applyMutation({ target: { kind: 'canvas' }, category, phase: state as CanvasInteraction }); break
      case 'workflowState': model.applyMutation({ target: { kind: 'workflow' }, category, phase: state as WorkflowState }); break
      case 'slotOccupancy': model.applyMutation({ target: { kind: 'slot', slotId: id }, category, phase: state as SlotOccupancy }); break
      case 'slotDropState': model.applyMutation({ target: { kind: 'slot', slotId: id }, category, phase: state as SlotDropState }); break
      case 'nodeExecutionState': model.applyMutation({ target: { kind: 'node', nodeId: id }, category, phase: state as NodeExecutionState }); break
      case 'nodeConnectionState': model.applyMutation({ target: { kind: 'node', nodeId: id }, category, phase: state as NodeConnectionState }); break
      case 'nodeOverlay': model.applyMutation({ target: { kind: 'node', nodeId: id }, category, phase: state as NodeOverlay }); break
      case 'pathLifecycleState': model.applyMutation({ target: { kind: 'path', pathId: id }, category, phase: state as PathLifecycleState }); break
      case 'pathFlowState': model.applyMutation({ target: { kind: 'path', pathId: id }, category, phase: state as PathFlowState }); break
    }
  }

  const applyCapabilities = () => {
    workflowModel.current!.setNodeCapabilities(targetId, { acceptsInput, acceptsOutput })
    requestRender()
  }
  const currentAttributes = target === 'canvas'
    ? workflowModel.current!.getCanvasModel().getDataAttributes()
    : target === 'workflow'
      ? workflowModel.current!.getWorkflowState().getDataAttributes()
      : target === 'slot'
        ? workflowModel.current!.getSlotModel(targetId)?.getDataAttributes()
        : target === 'node'
          ? workflowModel.current!.getNodeModel(targetId)?.getDataAttributes()
          : workflowModel.current!.getPathModel(targetId)?.getDataAttributes()
  let canvasModelSnapshot: CanvasModelSnapshot = HoneycombWorkflowCanvasInspiration({
    edges: initialEdges,
    nodes: sceneNodes,
    placements,
    model: workflowModel.current,
  })

  return (
    <main className="wcsb-page">
      <header className="wcsb-header">
        <div><p>Workflow state model</p><h1>Workflow Canvas Scene Builder</h1></div>
        <span>{Object.keys(WorkflowStudioCategoryValues).length} state groups · {Object.values(WorkflowStudioCategoryValues).flat().length} declared states</span>
      </header>
      <div className="wcsb-layout">
        <aside className="wcsb-controls" aria-label="Canvas scene controls">
          <h2>Workflow model</h2>
          <p>Applies typed mutations, then publishes model attributes to the canvas.</p>
          <label>Element kind
            <select value={target} onChange={(event) => changeTarget(event.target.value as WorkflowStudioSceneTargetKind)}>{targets.map((kind) => <option key={kind}>{kind}</option>)}</select>
          </label>
          <label>Element
            <select value={targetId} onChange={(event) => setTargetId(event.target.value)}>{targetIds.map((id) => <option key={id}>{id}</option>)}</select>
          </label>
          <label>State group
            <select value={category} onChange={(event) => { const nextCategory = event.target.value as WorkflowStudioStateCategory; setCategory(nextCategory); setState(WorkflowStudioCategoryValues[nextCategory][0]) }}>{categories.map((item) => <option key={item}>{item}</option>)}</select>
          </label>
          <label>State
            <select value={state} onChange={(event) => setState(event.target.value)}>{states.map((item) => <option key={item}>{item}</option>)}</select>
          </label>
          <button className="wcsb-apply" onClick={applyState} type="button">Apply state mutation</button>
          {target === 'node' && <fieldset><legend>Node capabilities</legend><label><input checked={acceptsInput} onChange={(event) => setAcceptsInput(event.target.checked)} type="checkbox" /> accepts input</label><label><input checked={acceptsOutput} onChange={(event) => setAcceptsOutput(event.target.checked)} type="checkbox" /> accepts output</label><button onClick={applyCapabilities} type="button">Apply capabilities</button></fieldset>}
          <section className="wcsb-attributes"><h2>Published attributes</h2>{currentAttributes ? Object.entries(currentAttributes).map(([key, value]) => <code key={key}>{key}={JSON.stringify(value)}</code>) : <span>Choose an element.</span>}</section>
        </aside>
        <section className="wcsb-canvas" aria-label="Scene canvas preview">
          {canvasModelSnapshot.getCanvas()}
        </section>
      </div>
      <footer className="wcsb-state-catalog">{Object.entries(WorkflowStudioCategoryValues).map(([group, entries]) => <section key={group}><h2>{group}</h2><div>{entries.map((entry) => <span data-state-attribute={workflowStudioStateAttributeName(group as WorkflowStudioStateCategory)} key={entry}>{entry}</span>)}</div></section>)}</footer>
    </main>
  )
}

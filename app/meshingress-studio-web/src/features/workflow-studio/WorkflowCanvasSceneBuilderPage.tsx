import { useEffect, useMemo, useReducer, useRef, useState, type CSSProperties, type KeyboardEvent, type PointerEvent } from 'react'
import { defaultHoneycombPrototypePlacements, HoneycombWorkflowCanvasInspiration } from './canvas/honeycomb-prototype/HoneycombWorkflowCanvasInspiration'
import { initialEdges, initialNodes } from './compilation/sample-workflow'
import { hasCanvasPanStarted, panCanvas, type CanvasPanPosition } from './scene-builder-pan'
import { clampSceneBuilderControlsWidth, resizeSceneBuilderControls, sceneBuilderControlsMaxWidth, sceneBuilderControlsMinWidth } from './scene-builder-controls-resize'
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
import { StateCategory, WorkflowStudioCategoryValues, workflowStudioSceneTargetCategories } from './states/WorkflowStudioState'
import { WorkflowModelSnapshotFactory } from './states/factories/WorkflowModelSnapshotFactory'

const sceneNodes: WorkflowNode[] = [
  ...initialNodes,
  {
    id: 'r-005', title: 'New Node', kind: 'tool', iconKind: 'tool', toolId: 'prototype.placeholder', functionName: 'placeholder', output: 'prototype', x: 0, y: 0, arguments: {}, annotations: { audit: false, scopes: [] }, capabilities: { acceptsInput: true, acceptsOutput: true },
  },
]

const placements = defaultHoneycombPrototypePlacements

const targets: readonly WorkflowStudioSceneTargetKind[] = ['canvas', 'workflow', 'slot', 'node', 'path']
const stateGroupCount = Object.keys(WorkflowStudioCategoryValues).length
const declaredStateCount = Object.values(WorkflowStudioCategoryValues).flat().length

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
  const panGesture = useRef<{
    current: CanvasPanPosition
    pointerId: number
    previousCanvasInteraction: CanvasInteraction
    start: CanvasPanPosition
    started: boolean
  } | null>(null)
  const controlsResizeGesture = useRef<{
    maxWidth: number
    pointerId: number
    startClientX: number
    startWidth: number
  } | null>(null)
  const [target, setTarget] = useState<WorkflowStudioSceneTargetKind>('node')
  const [targetId, setTargetId] = useState('r-001')
  const [category, setCategory] = useState('nodeOverlay')
  const [state, setState] = useState('hovered')
  const [acceptsInput, setAcceptsInput] = useState(true)
  const [acceptsOutput, setAcceptsOutput] = useState(true)
  const [pan, setPan] = useState<CanvasPanPosition>({ x: 0, y: 0 })
  const [isPanning, setIsPanning] = useState(false)
  const [controlsWidth, setControlsWidth] = useState(320)

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

  const beginCanvasPan = (event: PointerEvent<HTMLDivElement>) => {
    if (!event.isPrimary || event.button !== 0) return
    if (event.target instanceof Element && event.target.closest('button, input, select, textarea, a')) return
    const model = workflowModel.current!
    panGesture.current = {
      current: { x: event.clientX, y: event.clientY },
      pointerId: event.pointerId,
      previousCanvasInteraction: model.getCanvasModel().getActivePhases().at(0) ?? StateCategory.ofIdle,
      start: { x: event.clientX, y: event.clientY },
      started: false,
    }
    event.currentTarget.setPointerCapture(event.pointerId)
  }

  const moveCanvasPan = (event: PointerEvent<HTMLDivElement>) => {
    const gesture = panGesture.current
    if (!gesture || gesture.pointerId !== event.pointerId) return
    const current = { x: event.clientX, y: event.clientY }
    if (!gesture.started && !hasCanvasPanStarted(gesture.start, current)) return
    if (!gesture.started) {
      gesture.started = true
      workflowModel.current!.applyMutation({ target: { kind: 'canvas' }, category: 'canvasInteraction', phase: StateCategory.ofPanning })
      setIsPanning(true)
    }
    event.preventDefault()
    setPan((position) => panCanvas(position, gesture.current, current))
    gesture.current = current
  }

  const finishCanvasPan = (event: PointerEvent<HTMLDivElement>) => {
    const gesture = panGesture.current
    if (!gesture || gesture.pointerId !== event.pointerId) return
    if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId)
    if (gesture.started) {
      workflowModel.current!.applyMutation({ target: { kind: 'canvas' }, category: 'canvasInteraction', phase: gesture.previousCanvasInteraction })
      setIsPanning(false)
    }
    panGesture.current = null
  }

  const beginControlsResize = (event: PointerEvent<HTMLDivElement>) => {
    if (!event.isPrimary || event.button !== 0) return
    const layoutWidth = event.currentTarget.parentElement?.getBoundingClientRect().width ?? 0
    controlsResizeGesture.current = {
      maxWidth: Math.max(sceneBuilderControlsMinWidth, Math.min(sceneBuilderControlsMaxWidth, layoutWidth - 280)),
      pointerId: event.pointerId,
      startClientX: event.clientX,
      startWidth: controlsWidth,
    }
    event.currentTarget.setPointerCapture(event.pointerId)
  }

  const moveControlsResize = (event: PointerEvent<HTMLDivElement>) => {
    const gesture = controlsResizeGesture.current
    if (!gesture || gesture.pointerId !== event.pointerId) return
    setControlsWidth(resizeSceneBuilderControls(gesture.startWidth, gesture.startClientX, event.clientX, gesture.maxWidth))
  }

  const finishControlsResize = (event: PointerEvent<HTMLDivElement>) => {
    const gesture = controlsResizeGesture.current
    if (!gesture || gesture.pointerId !== event.pointerId) return
    if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId)
    controlsResizeGesture.current = null
  }

  const resizeControlsWithKeyboard = (event: KeyboardEvent<HTMLDivElement>) => {
    const amount = event.key === 'ArrowLeft' ? 16 : event.key === 'ArrowRight' ? -16 : 0
    if (!amount) return
    event.preventDefault()
    setControlsWidth((width) => clampSceneBuilderControlsWidth(width + amount))
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
      <div className="wcsb-layout" style={{ '--wcsb-controls-width': `${controlsWidth}px` } as CSSProperties}>
        <section className="wcsb-canvas" aria-label="Scene canvas preview">
          <div
            aria-label="Pannable scene canvas. Drag to pan; mouse wheel does not scroll this canvas."
            className={`wcsb-pan-scrollpane${isPanning ? ' wcsb-pan-scrollpane--panning' : ''}`}
            onPointerCancel={finishCanvasPan}
            onPointerDown={beginCanvasPan}
            onPointerMove={moveCanvasPan}
            onPointerUp={finishCanvasPan}
            onWheelCapture={(event) => event.preventDefault()}
            role="region"
          >
            <div className="wcsb-pan-scrollpane__content" style={{ transform: `translate3d(${pan.x}px, ${pan.y}px, 0)` }}>
              {canvasModelSnapshot.getCanvas()}
            </div>
          </div>
        </section>
        <div
          aria-label="Resize canvas controls"
          aria-orientation="vertical"
          aria-valuemax={sceneBuilderControlsMaxWidth}
          aria-valuemin={sceneBuilderControlsMinWidth}
          aria-valuenow={controlsWidth}
          className="wcsb-controls-resize"
          onKeyDown={resizeControlsWithKeyboard}
          onPointerCancel={finishControlsResize}
          onPointerDown={beginControlsResize}
          onPointerMove={moveControlsResize}
          onPointerUp={finishControlsResize}
          role="separator"
          tabIndex={0}
        />
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
      </div>
      <footer className="wcsb-footer">
        <span>{stateGroupCount} state groups · {declaredStateCount} declared states</span>
      </footer>
    </main>
  )
}

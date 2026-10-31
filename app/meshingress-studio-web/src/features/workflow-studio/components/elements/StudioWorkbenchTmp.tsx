import { useEffect, useMemo, useReducer } from 'react'
import { HoneycombWorkflowCanvas } from '../../canvas/honeycomb-prototype/HoneycombWorkflowCanvasInspiration'
import { WorkflowModelSnapshotFactory } from '../../states/factories/WorkflowModelSnapshotFactory'
import type { NodeRunState, WorkflowEdge, WorkflowNode } from '../../types'
import { createStudioWorkbenchTmpPlacements, honeycombExecutionStateForRunState, honeycombNodeConnectionStateForEdges, honeycombWorkflowStateForRun, syncHoneycombPathStates } from './studio-workbench-tmp-state'
import './studio-workbench-tmp.css'

export interface StudioWorkbenchTmpProps {
  edges: WorkflowEdge[]
  nodes: WorkflowNode[]
  onRun: () => void
  onSelectNode: (nodeId: string) => void
  onValidate: () => boolean
  runStates: Record<string, NodeRunState>
  running: boolean
  selectedNodeId: string
  workflowFileName: string
}

/**
 * Opt-in Honeycomb workbench used to inspect live execution state. It is
 * deliberately read-only for topology and node editing; the regular Workbench
 * remains the production canvas for those operations.
 */
export function StudioWorkbenchTmp({
  edges,
  nodes,
  onRun,
  onSelectNode,
  onValidate,
  runStates,
  running,
  selectedNodeId,
  workflowFileName,
}: StudioWorkbenchTmpProps) {
  const placements = useMemo(() => createStudioWorkbenchTmpPlacements(nodes), [nodes])
  const model = useMemo(
    () => new WorkflowModelSnapshotFactory().create({ edges, nodes, placements }),
    [edges, nodes, placements],
  )
  const [, requestRender] = useReducer((revision: number) => revision + 1, 0)

  useEffect(() => model.subscribe(requestRender), [model])

  useEffect(() => {
    model.applyMutation({
      target: { kind: 'workflow' },
      category: 'workflowState',
      phase: honeycombWorkflowStateForRun(nodes, runStates, running),
    })
    nodes.forEach((node) => {
      model.applyMutation({
        target: { kind: 'node', nodeId: node.id },
        category: 'nodeExecutionState',
        phase: honeycombExecutionStateForRunState(runStates[node.id]),
      })
      model.applyMutation({
        target: { kind: 'node', nodeId: node.id },
        category: 'nodeConnectionState',
        phase: honeycombNodeConnectionStateForEdges(node.id, edges),
      })
    })
    syncHoneycombPathStates(model, nodes, edges, runStates, running)
  }, [edges, model, nodes, runStates, running])

  return (
    <section aria-label="Studio Workbench Tmp" className="studio-workbench-tmp">
      <header className="studio-workbench-tmp__header">
        <div>
          <p>Temporary Honeycomb execution view</p>
          <h1>{workflowFileName}</h1>
        </div>
        <div aria-label="Workflow actions" className="studio-workbench-tmp__actions">
          <button disabled={running} onClick={onRun} type="button">{running ? 'Running…' : 'Run workflow'}</button>
          <button onClick={() => void onValidate()} type="button">Validate</button>
        </div>
      </header>
      <div className="studio-workbench-tmp__summary">
        <span>Read-only topology · live execution state</span>
        <span>Selected: {selectedNodeId}</span>
      </div>
      <div className="studio-workbench-tmp__canvas">
        <HoneycombWorkflowCanvas
          edges={edges}
          model={model}
          nodes={nodes}
          onNodeSelect={onSelectNode}
          placements={placements}
        />
      </div>
    </section>
  )
}

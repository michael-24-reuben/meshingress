import type { InspectorView, WorkflowEdge, WorkflowNode } from '../types'
import { workflowDefinition } from '../definition'
import { Empty } from './ExplorerPanel'

export function InspectorPanel({ inspector, selectedNode, nodes, edges, onInspectorChange, onNodeChange }: { inspector: InspectorView; selectedNode?: WorkflowNode; nodes: WorkflowNode[]; edges: WorkflowEdge[]; onInspectorChange: (view: InspectorView) => void; onNodeChange: (id: string, changes: Partial<WorkflowNode>) => void }) {
  const tabs: Array<{ id: InspectorView; label: string }> = [{ id: 'node', label: 'Workflow node' }, { id: 'workflow', label: 'Workflow definition' }, { id: 'annotations', label: 'Node annotations' }, { id: 'payload', label: 'Node payload' }]
  return <aside className="right-panel"><div className="panel-head"><div className="panel-title">{selectedNode?.title ?? 'No node selected'}</div></div><div className="tabs">{tabs.map((tab) => <button className={`tab${inspector === tab.id ? ' active' : ''}`} key={tab.id} onClick={() => onInspectorChange(tab.id)} type="button">{tab.label}</button>)}</div><div className="inspector">{!selectedNode ? <Empty message="Select a workflow node." /> : <InspectorContent edges={edges} inspector={inspector} node={selectedNode} nodes={nodes} onNodeChange={onNodeChange} />}</div></aside>
}

function InspectorContent({ inspector, node, nodes, edges, onNodeChange }: { inspector: InspectorView; node: WorkflowNode; nodes: WorkflowNode[]; edges: WorkflowEdge[]; onNodeChange: (id: string, changes: Partial<WorkflowNode>) => void }) {
  if (inspector === 'workflow') return <JsonBox value={workflowDefinition(nodes, edges)} />
  if (inspector === 'annotations') return <JsonBox value={node.annotations} />
  if (inspector === 'payload') return <JsonBox value={node.arguments} />
  const field = (label: string, property: keyof Pick<WorkflowNode, 'title' | 'toolId' | 'functionName' | 'output'>) => <div className="field"><label htmlFor={`node-${property}`}>{label}</label><input id={`node-${property}`} onChange={(event) => onNodeChange(node.id, { [property]: event.target.value })} value={node[property]} /></div>
  return <>{field('Display name', 'title')}{field('Tool ID', 'toolId')}{field('Function', 'functionName')}{field('Output variable', 'output')}<div className="section-heading">Arguments in</div>{Object.entries(node.arguments).length ? Object.entries(node.arguments).map(([key, value]) => <div className="box" key={key}><label className="muted" htmlFor={`argument-${key}`}>{key}</label><textarea id={`argument-${key}`} onChange={(event) => onNodeChange(node.id, { arguments: { ...node.arguments, [key]: event.target.value } })} value={value} /></div>) : <Empty message="No input arguments." />}</>
}

function JsonBox({ value }: { value: unknown }) { return <div className="box"><pre>{JSON.stringify(value, null, 2)}</pre></div> }

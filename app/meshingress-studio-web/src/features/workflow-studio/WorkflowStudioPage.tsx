import { useState, type CSSProperties } from 'react'
import { apiRequest } from '../../api/client'
import { initialEdges, initialNodes, workflowName } from './sample-workflow'
import { ActivityRail } from './components/ActivityRail'
import { ExplorerPanel } from './components/ExplorerPanel'
import { InspectorPanel } from './components/InspectorPanel'
import { TopBar } from './components/TopBar'
import { WorkflowCanvas } from './components/WorkflowCanvas'
import { WorkflowDrawer } from './components/WorkflowDrawer'
import type { DrawerView, InspectorView, LogEntry, NodeRunState, StudioView, WorkflowRunResult } from './types'
import './workflow-studio.css'

const sampleRunPath = '/api/v1/workflows/samples/desktop-volume-greeting-solo-leveling/run'

export function WorkflowStudioPage() {
  const [nodes, setNodes] = useState(initialNodes)
  const [view, setView] = useState<StudioView>('files')
  const [drawer, setDrawer] = useState<DrawerView>('current')
  const [inspector, setInspector] = useState<InspectorView>('node')
  const [selectedNodeId, setSelectedNodeId] = useState('r-002')
  const [logs, setLogs] = useState<LogEntry[]>([])
  const [lastRun, setLastRun] = useState<WorkflowRunResult | null>(null)
  const [runStates, setRunStates] = useState<Record<string, NodeRunState>>({})
  const [running, setRunning] = useState(false)
  const [status, setStatus] = useState({ text: 'Not validated', kind: '' })
  const [widths, setWidths] = useState({ explorer: 245, inspector: 330 })
  const selectedNode = nodes.find((node) => node.id === selectedNodeId)
  const addLog = (source: string, message: string) => setLogs((current) => [{ time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }), source, message }, ...current])
  const setMessage = (text: string) => { setStatus({ text, kind: 'status-success' }); addLog('Editor', text) }
  const validate = () => { const names = new Set<string>(); const errors = nodes.flatMap((node) => { if (!/^[A-Za-z_$][A-Za-z0-9_$]*$/.test(node.output)) return [`${node.title}: invalid output variable`]; if (names.has(node.output)) return [`${node.title}: duplicate output variable`]; names.add(node.output); return [] }); if (errors.length) { setStatus({ text: `${errors.length} validation issue(s)`, kind: 'status-error' }); addLog('Validator', errors[0]); return false } setStatus({ text: 'Workflow is valid', kind: 'status-success' }); addLog('Validator', 'Workflow passed validation.'); return true }
  const run = async () => { if (running || !validate()) return; setRunning(true); setLogs([]); setLastRun(null); setDrawer('current'); setRunStates(Object.fromEntries(nodes.map((node) => [node.id, 'running']))); try { addLog('Runtime', 'Submitting the desktop workflow to Meshingress.'); const result = await apiRequest<WorkflowRunResult>(sampleRunPath, { method: 'POST', headers: { 'Content-Type': 'application/json', 'Mcp-Session-Id': crypto.randomUUID(), 'X-Request-Id': crypto.randomUUID() } }); setLastRun(result); const nextStates: Record<string, NodeRunState> = {}; result.nodeOutcomes?.forEach((outcome) => { nextStates[outcome.requestId] = outcome.failed ? 'error' : 'success'; const node = nodes.find((candidate) => candidate.id === outcome.requestId); addLog(node?.title ?? outcome.requestId, outcome.failed ? `Failed after ${outcome.attempts} attempt(s): ${outcome.message ?? 'unknown error'}` : `Completed through ${outcome.port} on attempt ${outcome.attempts}.`) }); setRunStates(nextStates); const complete = result.status === 'COMPLETED'; setStatus({ text: complete ? 'Last run succeeded — outputs shown below' : 'Last run failed — outputs shown below', kind: complete ? 'status-success' : 'status-error' }); setDrawer('variables') } catch (error) { setRunStates(Object.fromEntries(nodes.map((node) => [node.id, 'error']))); const message = error instanceof Error ? error.message : 'Unknown runtime error'; setStatus({ text: `Run failed: ${message}`, kind: 'status-error' }); addLog('Runtime', message) } finally { setRunning(false) } }
  const resize = (panel: keyof typeof widths, event: React.PointerEvent<HTMLDivElement>) => { const target = event.currentTarget; const start = event.clientX; const original = widths[panel]; target.setPointerCapture(event.pointerId); const move = (moveEvent: PointerEvent) => setWidths((current) => ({ ...current, [panel]: Math.max(panel === 'explorer' ? 190 : 260, Math.min(panel === 'explorer' ? 460 : 520, original + (panel === 'explorer' ? 1 : -1) * (moveEvent.clientX - start))) })); const end = () => { target.removeEventListener('pointermove', move); target.removeEventListener('pointerup', end); target.removeEventListener('pointercancel', end) }; target.addEventListener('pointermove', move); target.addEventListener('pointerup', end); target.addEventListener('pointercancel', end) }
  return <div className="studio-shell" style={{ '--explorer-width': `${widths.explorer}px`, '--inspector-width': `${widths.inspector}px` } as CSSProperties}><TopBar onRun={run} onStatus={setMessage} onValidate={validate} running={running} workflowName={workflowName} /><div className="layout"><ActivityRail onChange={setView} view={view} /><ExplorerPanel logs={logs} nodes={nodes} onSelectNode={setSelectedNodeId} onStatus={setMessage} view={view} /><div aria-label="Resize explorer panel" aria-orientation="vertical" className="panel-resizer" onPointerDown={(event) => resize('explorer', event)} role="separator" /><main className="center"><div className="canvas-head"><div><span className="muted">Workflows › </span>Desktop Volume, Greeting, and Solo Leveling Search</div><div aria-live="polite" className={`muted ${status.kind}`}>{status.text}</div></div><WorkflowCanvas edges={initialEdges} nodeRunStates={runStates} nodes={nodes} onNodesChange={setNodes} onSelect={setSelectedNodeId} selectedNodeId={selectedNodeId} /><WorkflowDrawer drawer={drawer} edges={initialEdges} lastRun={lastRun} logs={logs} nodes={nodes} onChange={setDrawer} /></main><div aria-label="Resize workflow details panel" aria-orientation="vertical" className="panel-resizer" onPointerDown={(event) => resize('inspector', event)} role="separator" /><InspectorPanel edges={initialEdges} inspector={inspector} nodes={nodes} onInspectorChange={setInspector} onNodeChange={(id, changes) => setNodes((current) => current.map((node) => node.id === id ? { ...node, ...changes } : node))} selectedNode={selectedNode} /></div></div>
}

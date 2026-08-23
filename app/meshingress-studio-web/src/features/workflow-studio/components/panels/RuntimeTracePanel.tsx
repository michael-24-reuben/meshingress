import { useState } from 'react'
import type { RuntimeTrace, RuntimeTraceEntry, WorkflowNode } from '../../types'
import { Empty } from '../elements/Empty'
import { InitiatorLink } from '../elements/links/InitiatorLink'
import { DeleteIcon, FilterIcon, SettingsIcon, StopIcon } from '../../../../components/icons/node-icons'

export interface RuntimeTracePanelProps {
  trace: RuntimeTrace | null
  running: boolean
  onClear: () => void
  onSelectNode: (nodeId: string) => boolean
  nodes: WorkflowNode[]
}

export function RuntimeTracePanel({ trace, running, onClear, onSelectNode, nodes }: RuntimeTracePanelProps) {
  const [filtersOpen, setFiltersOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [tool, setTool] = useState('all')
  const [showSettings, setShowSettings] = useState(false)
  const [showCompleted, setShowCompleted] = useState(true)
  const entries = trace?.entries ?? []
  const tools = [...new Set(entries.map((entry) => entry.initiator))].toSorted()
  const filteredEntries = entries.filter((entry) => {
    const haystack = `${entry.variable} ${entry.initiator} ${entry.requestId} ${entry.status} ${entry.type ?? ''}`.toLowerCase()
    return (tool === 'all' || entry.initiator === tool) && haystack.includes(query.trim().toLowerCase())
  })
  const requestCount = entries.filter((entry) => entry.kind === 'tool').length
  const variableCount = entries.filter((entry) => entry.type !== undefined).length
  const capturedBytes = entries.reduce((total, entry) => total + (entry.size ?? 0), 0)
  const now = trace ? serverTimelineNow(trace) : 0
  const duration = trace ? now - trace.startedAt : 0

  return (
    <div className="runtime-panel">
      <div className="runtime-toolbar">
        <div className="runtime-toolbar-actions">
          <button className="runtime-action runtime-stop" disabled={!running} title="Workflow cancellation is not available in the beta runtime yet." type="button">
            <StopIcon size={20} />
          </button>
          <button className="runtime-action" disabled={!trace} onClick={onClear} title="Clear runtime log" type="button">
            <DeleteIcon size={20} />
          </button>
          <button aria-expanded={filtersOpen} className={`runtime-action${filtersOpen ? ' is-active' : ''}`} onClick={() => setFiltersOpen((current) => !current)} title={filtersOpen ? 'Hide filters' : 'Show filters'} type="button">
            <FilterIcon size={20} />
          </button>
        </div>
        <div className="runtime-settings-wrap">
          <button aria-expanded={showSettings} className={`runtime-action${showSettings ? ' is-active' : ''}`} onClick={() => setShowSettings((current) => !current)} title={showSettings ? 'Hide settings' : 'Show settings'} type="button">
            <SettingsIcon size={20} />
          </button>
          {showSettings && (
            <div className="runtime-settings" role="dialog">
              <label>
                <input checked={showCompleted} onChange={(event) => setShowCompleted(event.target.checked)} type="checkbox" /> Show completed spans
              </label>
              <span>Trace data stays in this Studio session.</span>
            </div>
          )}
        </div>
      </div>
      {filtersOpen && (
        <div className="runtime-filters">
          <input aria-label="Search runtime variables or tools" onChange={(event) => setQuery(event.target.value)} placeholder="Filter variables, tools, or node IDs" type="search" value={query} />
          <label>
            Tool
            <select aria-label="Filter by tool" onChange={(event) => setTool(event.target.value)} value={tool}>
              <option value="all">All tools</option>
              {tools.map((entry) => (
                <option key={entry} value={entry}>{entry}</option>
              ))}
            </select>
          </label>
        </div>
      )}
      {trace ? (
        <>
          <RuntimeTimeline entries={filteredEntries} nodes={nodes} now={now} showCompleted={showCompleted} startedAt={trace.startedAt} />
          <div className="runtime-table-wrap">
            <table className="runtime-table">
              <thead>
                <tr><th>Variable</th><th>Node ID</th><th>Status</th><th>Type</th><th>Initiator</th><th>Size</th><th>Time</th></tr>
              </thead>
              <tbody>
                {filteredEntries.map((entry) => (
                  <RuntimeTraceRow entry={entry} key={entry.requestId} now={now} onSelectNode={onSelectNode} />
                ))}
              </tbody>
            </table>
          </div>
          <footer className="runtime-footer">
            <span>{requestCount} request{requestCount === 1 ? '' : 's'}</span>
            <span>{variableCount} variable{variableCount === 1 ? '' : 's'}</span>
            <span>{formatBytes(capturedBytes)} captured data</span>
            <span>{trace.completedAt ? `Finish: ${formatDuration(duration)}` : `Runtime: ${formatDuration(duration)}`}</span>
          </footer>
        </>
      ) : (
        <Empty message="No runtime trace yet. Run the workflow to capture its live execution." />
      )}
      {running && <span aria-live="polite" className="runtime-live-indicator">Recording live runtime events</span>}
    </div>
  )
}

function serverTimelineNow(trace: RuntimeTrace): number {
  if (trace.completedAt !== undefined) return trace.completedAt
  const anchor = trace.serverTimeAnchor
  if (!anchor) return trace.startedAt
  return anchor.at + Math.max(0, Date.now() - anchor.receivedAt)
}

function RuntimeTimeline({ entries, nodes, startedAt, now, showCompleted }: { entries: RuntimeTraceEntry[]; nodes: WorkflowNode[]; startedAt: number; now: number; showCompleted: boolean }) {
  const timedEntries = entries.filter((entry) => entry.startedAt !== undefined && (showCompleted || entry.status === 'running'))
  const total = Math.max(1, ...timedEntries.map((entry) => (entry.completedAt ?? now) - startedAt), now - startedAt)
  // WorkflowRuntime currently has one serial execution worker. Do not invent lanes
  // from arrival order or timing; introduce a server-provided worker ID if that changes.
  const lanes = timedEntries.length ? [0] : []
  const markers = [0, .25, .5, .75, 1]
  return (
    <section aria-label="Runtime timeline in milliseconds" className="runtime-timeline">
      <div className="runtime-timeline-scale">
        {markers.map((marker) => (
          <span key={marker} style={{ left: `${marker * 100}%` }}>{formatMilliseconds(total * marker)}</span>
        ))}
      </div>
      {lanes.length ? (
        <div className="runtime-timeline-lanes">
          {lanes.map((lane) => (
            <div className="runtime-timeline-lane" key={lane}>
              {timedEntries.map((entry) => {
                const left = (((entry.startedAt ?? startedAt) - startedAt) / total) * 100
                const width = Math.max(1.2, (((entry.completedAt ?? now) - (entry.startedAt ?? now)) / total) * 100)
                const node = nodes.find((candidate) => candidate.id === entry.requestId)
                const nodeId = node?.id ?? entry.requestId
                const nodeName = node?.title ?? entry.variable
                return (
                  <div aria-describedby={`runtime-timeline-tooltip-${entry.requestId}`} className={`runtime-timeline-span ${entry.status}`} key={entry.requestId} style={{ left: `${left}%`, width: `${width}%` }} tabIndex={0} title={`${nodeId} — ${nodeName}`}>
                    <span className="runtime-timeline-span-label">{entry.variable}</span>
                    <span className="runtime-timeline-tooltip" id={`runtime-timeline-tooltip-${entry.requestId}`} role="tooltip">
                      <code>{nodeId}</code>
                      <span>{nodeName}</span>
                    </span>
                  </div>
                )
              })}
            </div>
          ))}
        </div>
      ) : (
        <div className="runtime-timeline-empty">Waiting for live node timing events.</div>
      )}
    </section>
  )
}

function RuntimeTraceRow({ entry, now, onSelectNode }: { entry: RuntimeTraceEntry; now: number; onSelectNode: (nodeId: string) => boolean }) {
  const elapsed = entry.startedAt === undefined ? undefined : (entry.completedAt ?? now) - entry.startedAt
  const status = entry.status === 'idle' ? 'Queued' : entry.status === 'success' ? 'Success' : entry.status === 'error' ? 'Error' : 'Running'
  return (
    <tr>
      <td>
        <span className="runtime-variable-identity">
          <strong>{entry.variable}</strong>
        </span>
      </td>
      <td><code>{entry.requestId}</code></td>
      <td><span className={`runtime-status ${entry.status}`}>{status}</span></td>
      <td>{entry.type ?? '—'}</td>
      <td><InitiatorLink nodeId={entry.requestId} onSelectNode={onSelectNode} path={entry.initiator} /></td>
      <td>{entry.size === undefined ? '—' : formatBytes(entry.size)}</td>
      <td>{elapsed === undefined ? '—' : formatDuration(elapsed)}</td>
    </tr>
  )
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  return `${(bytes / 1024).toFixed(bytes < 10 * 1024 ? 1 : 0)} kB`
}

function formatDuration(milliseconds: number): string {
  return milliseconds < 1000 ? `${Math.max(0, Math.round(milliseconds))} ms` : `${(milliseconds / 1000).toFixed(2)} s`
}

function formatMilliseconds(milliseconds: number): string {
  return `${Math.round(milliseconds)} ms`
}

export default RuntimeTracePanel

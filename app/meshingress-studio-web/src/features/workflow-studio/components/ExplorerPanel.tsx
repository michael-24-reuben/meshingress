import { useMemo, useState } from 'react'
import type { LogEntry, StudioView, WorkflowNode } from '../types'

const files = [{ name: 'desktop-volume-greeting-solo-leveling.json', path: 'samples', size: '1.6 KB', group: 'Workflow samples' }]
const tools = [{ id: 'cli.powershell', title: 'PowerShell', functions: 1 }, { id: 'helloworld', title: 'Hello World', functions: 1 }, { id: 'toonverse', title: 'Toonverse', functions: 6 }]

export function ExplorerPanel({ view, nodes, logs, onSelectNode, onStatus }: { view: StudioView; nodes: WorkflowNode[]; logs: LogEntry[]; onSelectNode: (id: string) => void; onStatus: (message: string) => void }) {
  const [query, setQuery] = useState('')
  const needle = query.trim().toLowerCase()
  const matches = (value: string) => value.toLowerCase().includes(needle)
  const { title, placeholder, body } = useMemo(() => {
    if (view === 'files') return { title: 'Files in storage', placeholder: 'Filter files', body: <div className="list">{files.filter((file) => matches(`${file.name} ${file.path}`)).map((file) => <ListItem icon="▣" key={file.name} meta={file.size} subtitle={file.path} title={file.name} />)}</div> }
    if (view === 'tools') return { title: 'Registered tools', placeholder: 'Filter registered tools', body: <div className="list">{tools.filter((tool) => matches(`${tool.id} ${tool.title}`)).map((tool) => <ListItem icon="T" key={tool.id} meta={`${tool.functions} fn`} subtitle={tool.id} title={tool.title} />)}</div> }
    if (view === 'used') return { title: 'Used tools and variables', placeholder: 'Filter used tools', body: <div>{nodes.filter((node) => node.kind !== 'trigger' && matches(`${node.title} ${node.toolId} ${node.output}`)).map((node) => <div key={node.id}><button className="list-item" onClick={() => onSelectNode(node.id)} type="button"><span className="item-icon">T</span><span className="item-main"><span className="item-title">{node.title}</span><span className="item-sub">{node.toolId}</span></span></button><div className="vars"><button className="var" onClick={() => onStatus(`Selected \${${node.output}}`)} type="button"><span>{node.output}</span><span>object</span></button>{Object.keys(node.arguments).map((key) => <button className="var" key={key} onClick={() => onStatus(`Selected \${${node.output}.${key}}`)} type="button"><span>{node.output}.{key}</span><span>field</span></button>)}</div></div>)}</div> }
    if (view === 'history') return { title: 'Previous executions', placeholder: 'Filter executions', body: <div className="list"><ListItem icon="L" meta="Not run" subtitle="Ready" title="sample" /></div> }
    return { title: 'Current execution logs', placeholder: 'Filter current logs', body: logs.length ? <div className="list">{logs.filter((log) => matches(`${log.source} ${log.message}`)).map((log) => <ListItem icon="▶" key={`${log.time}-${log.source}`} meta={log.time} subtitle={log.message} title={log.source} />)}</div> : <Empty message="No active execution logs." /> }
  // `matches` intentionally captures the current filter and only re-renders this panel.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [view, nodes, logs, needle])

  return <aside className="explorer"><div className="panel-head"><div className="panel-title">{title}</div><button aria-label="Add item" className="button small" type="button">＋</button></div><div className="panel-body"><input className="search" onChange={(event) => setQuery(event.target.value)} placeholder={placeholder} type="search" value={query} />{body}</div></aside>
}

function ListItem({ icon, title, subtitle, meta }: { icon: string; title: string; subtitle: string; meta?: string }) { return <button className="list-item" type="button"><span className="item-icon">{icon}</span><span className="item-main"><span className="item-title">{title}</span><span className="item-sub">{subtitle}</span></span>{meta && <span className="item-meta">{meta}</span>}</button> }
export function Empty({ message }: { message: string }) { return <div className="empty">{message}</div> }

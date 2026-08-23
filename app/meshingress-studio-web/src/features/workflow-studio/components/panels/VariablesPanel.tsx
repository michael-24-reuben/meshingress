import { useState } from 'react'
import type { WorkflowNode, WorkflowRunResult } from '../../types'
import type { ToolPresentationIndex } from '../../node-presentation'
import { Empty } from '../elements/Empty'
import { GetAsIcon, WarningColoredIcon, WorkflowNodeIcon } from '../../../../components/icons/node-icons'

export interface VariablesPanelProps {
  lastRun: WorkflowRunResult | null
  nodes: WorkflowNode[]
  onGetAs: (output: string) => void
  presentations: ToolPresentationIndex
}

type ViewMode = 'content' | 'raw' | 'meta'

interface VariableBoxProps {
  name: string
  value: unknown
  node?: WorkflowNode
  onGetAs: (output: string) => void
  presentations: ToolPresentationIndex
  violationCount: number
}

function VariableBox({ name, value, node, onGetAs, presentations, violationCount }: VariableBoxProps) {
  const [mode, setMode] = useState<ViewMode>('content')
  const reference = `${name}`

  const getDisplayContent = (): string => {
    if (value === undefined) return ''

    const valObj = typeof value === 'object' && value !== null ? (value as Record<string, unknown>) : null

    if (mode === 'raw') {
      return typeof value === 'string' ? value : JSON.stringify(value, null, 2)
    }

    if (mode === 'meta') {
      const meta = valObj?._meta ?? (valObj?.result as Record<string, unknown> | undefined)?._meta
      if (meta !== undefined) {
        return typeof meta === 'string' ? meta : JSON.stringify(meta, null, 2)
      }
      return JSON.stringify(null, null, 2)
    }

    // mode === 'content'
    const content = valObj?.content !== undefined
      ? valObj.content
      : (valObj?.result as Record<string, unknown> | undefined)?.content !== undefined
        ? (valObj?.result as Record<string, unknown> | undefined)?.content
        : value

    return typeof content === 'string' ? content : JSON.stringify(content, null, 2)
  }

  return (
    <div className="box">
      <div className="box-topbar">
        {node && (
          <div className="drawer-node-identity">
            <span className="node-icon drawer-node-icon" title={`${node.toolId} / ${node.functionName}`}>
              <WorkflowNodeIcon descriptor={presentations.forNode(node)} />
            </span>
            <code className="drawer-node-id" title={`Node ID: ${node.id}`}>{node.id}</code>
          </div>
        )}
        <div className="box-heading">
          <div className="section-heading" title={`Output variable: ${name}`}>{node?.title ?? name}</div>
          {node && <code className="box-output-name" title={`Output variable: ${name}`}>{name}</code>}
        </div>
        {violationCount > 0 && (
          <span className="box-output-schema-warning" title={`${violationCount} output-schema violations`}>
            <WarningColoredIcon size={16} />
            <span>{violationCount} violations</span>
          </span>
        )}
        <div className="box-mode-selector">
          <button
            className={`box-mode-btn ${mode === 'content' ? 'is-active' : ''}`}
            onClick={() => setMode('content')}
            type="button"
          >
            Content
          </button>
          <button
            className={`box-mode-btn ${mode === 'raw' ? 'is-active' : ''}`}
            onClick={() => setMode('raw')}
            type="button"
          >
            Raw
          </button>
          <button
            className={`box-mode-btn ${mode === 'meta' ? 'is-active' : ''}`}
            onClick={() => setMode('meta')}
            type="button"
          >
            Meta
          </button>
        </div>
        <button aria-label={`Copy ${reference} as an input reference`} className="box-getter" onClick={() => onGetAs(name)} title={`Copy ${reference} as an input reference`} type="button">
          <GetAsIcon size={16} />
        </button>
      </div>
      <pre>{getDisplayContent()}</pre>
    </div>
  )
}

export function VariablesPanel({ lastRun, nodes, onGetAs, presentations }: VariablesPanelProps) {
  if (!lastRun) {
    return (
      <>
        {nodes.map((node) => (
          <div className="log-row" key={node.id}>
            <span className="log-time">object</span>
            <strong>{node.output}</strong>
            <span>{node.title} structured output</span>
          </div>
        ))}
      </>
    )
  }

  const entries = Object.entries(lastRun.results ?? {})
  if (!entries.length) {
    return <Empty message="This run produced no named results." />
  }

  return (
    <>
      {entries.map(([name, value]) => {
        const node = nodes.find((candidate) => candidate.output === name)
        const violationCount = node
          ? lastRun.nodeResults?.find((result) => result.nodeId === node.id)?.diagnostics
            ?.filter((diagnostic) => diagnostic.type === 'output.schema.violation').length ?? 0
          : 0
        return (
          <VariableBox
            key={name}
            name={name}
            value={value}
            node={node}
            onGetAs={onGetAs}
            presentations={presentations}
            violationCount={violationCount}
          />
        )
      })}
    </>
  )
}

export default VariablesPanel

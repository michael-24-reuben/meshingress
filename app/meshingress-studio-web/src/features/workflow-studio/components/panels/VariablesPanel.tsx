import type { WorkflowNode, WorkflowRunResult } from '../../types'
import type { ToolPresentationIndex } from '../../node-presentation'
import { Empty } from '../elements/Empty'
import { GetAsIcon, WorkflowNodeIcon } from '../../../../components/icons/node-icons'

export interface VariablesPanelProps {
  lastRun: WorkflowRunResult | null
  nodes: WorkflowNode[]
  onGetAs: (output: string) => void
  presentations: ToolPresentationIndex
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
        const reference = `{{ ${name} }}`
        return (
          <div className="box" key={name}>
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
              <button aria-label={`Copy ${reference} as an input reference`} className="box-getter" onClick={() => onGetAs(name)} title={`Copy ${reference} as an input reference`} type="button">
                <GetAsIcon size={16} />
              </button>
            </div>
            <pre>{JSON.stringify(value, null, 2)}</pre>
          </div>
        )
      })}
    </>
  )
}

export default VariablesPanel

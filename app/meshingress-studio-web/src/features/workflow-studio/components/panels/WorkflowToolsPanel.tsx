import type { WorkflowNode } from '../../types'
import { Empty } from '../elements/Empty'

export interface WorkflowToolsPanelProps {
  nodes: WorkflowNode[]
  onSelectNode: (id: string) => void
}

export function WorkflowToolsPanel({ nodes, onSelectNode }: WorkflowToolsPanelProps) {
  if (!nodes.length) {
    return <Empty message="No tools are used in this workflow." />
  }

  return (
    <div className="list">
      {nodes.map((node) => (
        <button className="tree-view-item" key={node.id} onClick={() => onSelectNode(node.id)} type="button">
          <span className="item-icon">N</span>
          <span className="item-main">
            <span className="item-title">{node.title}</span>
            <span className="item-sub">{node.toolId} / {node.functionName}</span>
          </span>
          <span className="item-meta">{node.output}</span>
        </button>
      ))}
    </div>
  )
}

export default WorkflowToolsPanel

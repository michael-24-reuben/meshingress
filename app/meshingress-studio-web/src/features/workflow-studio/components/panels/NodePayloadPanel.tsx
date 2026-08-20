import type { WorkflowNode } from '../../types'
import { JsonBox } from '../elements/JsonBox'

export interface NodePayloadPanelProps {
    node: WorkflowNode
}

export function NodePayloadPanel({ node }: NodePayloadPanelProps) {
    return <JsonBox value={node.arguments} />
}

export default NodePayloadPanel

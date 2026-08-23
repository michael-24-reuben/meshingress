import type { WorkflowEdge, WorkflowNode } from '../../types'
import { workflowDefinition } from '../../compilation/definition'
import { JsonBox } from '../elements/JsonBox'

export interface WorkflowDefinitionPanelProps {
    nodes: WorkflowNode[]
    edges: WorkflowEdge[]
}

export function WorkflowDefinitionPanel({ nodes, edges }: WorkflowDefinitionPanelProps) {
    return <JsonBox value={workflowDefinition(nodes, edges)} />
}

export default WorkflowDefinitionPanel

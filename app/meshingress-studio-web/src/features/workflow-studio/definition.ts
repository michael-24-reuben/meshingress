import type { WorkflowEdge, WorkflowNode } from './types'

export function workflowDefinition(nodes: WorkflowNode[], edges: WorkflowEdge[]) {
  return {
    id: 'desktop-volume-greeting-solo-leveling',
    version: 1,
    nodes: nodes.map((node) => ({ id: node.id, kind: node.kind, toolId: node.toolId, function: node.functionName, arguments: node.arguments, output: { variable: node.output, source: 'structuredContent' }, annotations: node.annotations })),
    edges: edges.map((edge, index) => ({ id: `edge-${index + 1}`, kind: 'control', source: edge.source, sourcePort: 'success', target: edge.target, targetPort: 'trigger' })),
  }
}

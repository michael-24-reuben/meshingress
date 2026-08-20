import type { WorkflowArgumentValue, WorkflowEdge, WorkflowNode } from './types'

export interface WorkflowDefinitionPayload {
  id: string
  version: number
  nodes: Array<{
    requestId: string
    kind: WorkflowNode['kind']
    functionName?: string
    arguments: Record<string, WorkflowArgumentValue>
    output: string
  }>
  edges: Array<{ source: string; target: string }>
}

export function workflowDefinition(nodes: WorkflowNode[], edges: WorkflowEdge[]): WorkflowDefinitionPayload {
  const trigger = nodes.find((node) => node.kind === 'trigger')
  const reachable = new Set<string>()
  const pending = trigger ? [trigger.id] : []
  while (pending.length) {
    const source = pending.shift()!
    if (reachable.has(source)) continue
    reachable.add(source)
    edges.filter((edge) => edge.source === source).forEach((edge) => pending.push(edge.target))
  }
  const activeNodes = nodes.filter((node) => reachable.has(node.id))
  const activeEdges = edges.filter((edge) => reachable.has(edge.source) && reachable.has(edge.target))
  return {
    id: 'desktop-volume-greeting-solo-leveling',
    version: 1,
    nodes: activeNodes.map((node) => ({
      requestId: node.id,
      kind: node.kind,
      functionName: node.kind === 'tool' ? `${node.toolId}.${node.functionName}` : undefined,
      arguments: node.arguments,
      output: node.output,
    })),
    edges: activeEdges,
  }
}

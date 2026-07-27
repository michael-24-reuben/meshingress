export interface WorkflowDefinition {
  id: string
  name: string
  version: string
  nodes: WorkflowNode[]
}

export interface WorkflowNode {
  requestId: string
  type: string
  inputs: Record<string, unknown>
}

export interface WorkflowRun {
  runId: string
  status: string
  results: Record<string, unknown>
}

export const GRID_SIZE = 20
export const NODE_STAGE_INSET = GRID_SIZE * 2
export const NODE_HEIGHT = GRID_SIZE * 6
export const CANVAS_SIZE = { width: 1500, height: 1000 }

export type StudioView = 'files' | 'tools' | 'used' | 'history' | 'current'
export type DrawerView = 'current' | 'variables' | 'definition'
export type InspectorView = 'node' | 'workflow' | 'annotations' | 'payload'
export type NodeRunState = 'idle' | 'running' | 'success' | 'error'

export interface WorkflowNode {
  id: string
  title: string
  kind: 'trigger' | 'tool'
  toolId: string
  functionName: string
  output: string
  x: number
  y: number
  width: number
  arguments: Record<string, string>
  annotations: { audit: boolean; timeoutMs: number; scopes: string[] }
}

export interface WorkflowEdge {
  source: string
  target: string
}

export interface LogEntry {
  time: string
  source: string
  message: string
}

export interface WorkflowRunResult {
  runId?: string
  status?: string
  results?: Record<string, unknown>
  nodeOutcomes?: Array<{ requestId: string; failed: boolean; attempts: number; port: string; message?: string }>
}

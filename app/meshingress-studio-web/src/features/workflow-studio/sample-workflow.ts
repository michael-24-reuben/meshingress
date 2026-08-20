import type { WorkflowEdge, WorkflowNode } from './types'

export const workflowName = 'Desktop Volume, Greeting, and Solo Leveling Search'

export const initialNodes: WorkflowNode[] = [
  { id: 'r-001', title: 'Manual Trigger', kind: 'trigger', iconKind: 'workflow-manual-trigger', toolId: 'trigger.manual', functionName: 'start', output: 'trigger', x: 40, y: 220, arguments: {}, annotations: { audit: false, timeoutMs: undefined, scopes: [] } },
  { id: 'r-002', title: 'Set System Volume', kind: 'tool', toolId: 'powershell.cli', functionName: 'execute', output: 'volume', x: 340, y: 220, arguments: { script: 'Set the default Windows render endpoint to 100%', timeoutMs: '30000' }, annotations: { audit: true, timeoutMs: 30000, scopes: ['SHELL_EXECUTE', 'FILES_WRITE'] } },
  { id: 'r-003', title: 'Greet Alphasunny', kind: 'tool', toolId: 'helloworld.greeting', functionName: 'greet', output: 'greeting', x: 640, y: 220, arguments: { name: 'Alphasunny' }, annotations: { audit: false, timeoutMs: 5000, scopes: ['USER_WRITE'] } },
  { id: 'r-004', title: 'Search Solo Leveling', kind: 'tool', toolId: 'open-ink-library.toonverse', functionName: 'search', output: 'soloLevelingSearch', x: 940, y: 220, arguments: { name: 'solo leveling', limit: '10', sortBy: 'popular' }, annotations: { audit: true, timeoutMs: 30000, scopes: ['HTTP_CLIENT', 'EXTERNAL_API_READ'] } },
]

export const initialEdges: WorkflowEdge[] = [
  { source: 'r-001', target: 'r-002' },
  { source: 'r-002', target: 'r-003' },
  { source: 'r-003', target: 'r-004' },
]

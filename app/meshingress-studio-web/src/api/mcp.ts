import { apiRequest } from './client'

export interface McpAvailabilityCondition {
  type: string
  parameters: Record<string, unknown>
}

export interface McpAvailability {
  version: 1
  mode: 'all' | 'any'
  conditions: McpAvailabilityCondition[]
}

export interface McpToolFunction {
  name: string
  moduleToolId?: string
  title?: string
  description?: string
  inputSchema?: Record<string, unknown>
  annotations?: {
    scopes?: string[]
    timeoutMs?: number
    availability?: McpAvailability
    returnType?: unknown
    [key: string]: unknown
  }
  outputSchema?: Record<string, unknown>
  returnType?: unknown
}

interface McpToolsListResponse {
  result?: { tools?: McpToolFunction[] }
  error?: { message?: string }
}

export async function listMcpTools(): Promise<McpToolFunction[]> {
  const response = await apiRequest<McpToolsListResponse>('/mcp', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Mcp-Session-Id': crypto.randomUUID(),
      'X-Request-Id': crypto.randomUUID(),
    },
    body: JSON.stringify({
      jsonrpc: '2.0',
      id: crypto.randomUUID(),
      method: 'tools/list',
      params: {},
    }),
  })

  if (response.error) {
    throw new Error(response.error.message ?? 'Meshingress rejected tools/list.')
  }

  return response.result?.tools ?? []
}

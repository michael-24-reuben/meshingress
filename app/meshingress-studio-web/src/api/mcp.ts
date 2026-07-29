import { apiRequest } from './client'

export interface McpToolFunction {
  name: string
  description?: string
  inputSchema?: Record<string, unknown>
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

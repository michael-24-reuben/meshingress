import { apiRequest } from './client'
import { generateUuid } from '../utils/uuid'
import { RuntimeConfiguration } from '../runtime/RuntimeConfiguration'
import type { WorkflowNodeOutcome, WorkflowNodeResult, WorkflowNodeStarted, WorkflowRunResult } from '../features/workflow-studio/types'
export type { WorkflowNodeOutcome, WorkflowNodeResult, WorkflowNodeStarted } from '../features/workflow-studio/types'
import type { WorkflowDefinitionPayload } from '../features/workflow-studio/compilation/definition'

const workflowRunPath = '/api/v1/workflows/run'

export interface WorkflowRunCallbacks {
  onRunStarted: (runId: string) => void
  onNodeStarted: (node: WorkflowNodeStarted) => void
  onNodeCompleted: (node: WorkflowNodeResult) => void
  onLifecycleGap?: (expectedSequence: number, receivedSequence: number) => void
}

export class WorkflowLiveTransportError extends Error {
  readonly canFallbackToHttp: boolean

  constructor(message: string, canFallbackToHttp: boolean) {
    super(message)
    this.name = 'WorkflowLiveTransportError'
    this.canFallbackToHttp = canFallbackToHttp
  }
}

export async function runWorkflowHttp(definition: WorkflowDefinitionPayload): Promise<WorkflowRunResult> {
  return apiRequest<WorkflowRunResult>(workflowRunPath, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Mcp-Session-Id': generateUuid(),
      'X-Request-Id': generateUuid(),
    },
    body: JSON.stringify(definition),
  })
}

export function runWorkflowLive(definition: WorkflowDefinitionPayload, callbacks: WorkflowRunCallbacks): Promise<WorkflowRunResult> {
  const requestId = generateUuid()
  return new Promise((resolve, reject) => {
    let settled = false
    let started = false
    let nextLifecycleSequence = 1
    let socket: WebSocket
    try {
      socket = new WebSocket(workflowWebSocketUrl())
    } catch (error) {
      reject(new WorkflowLiveTransportError(messageFor(error), true))
      return
    }

    const fail = (message: string, canFallbackToHttp: boolean) => {
      if (settled) return
      settled = true
      socket.close()
      reject(new WorkflowLiveTransportError(message, canFallbackToHttp))
    }

    socket.addEventListener('open', () => {
      socket.send(JSON.stringify({ action: 'run', definition, requestId }))
    })
    socket.addEventListener('message', (event) => {
      const payload = parseEvent(event.data)
      if (!payload || payload.requestId !== requestId) return
      if (typeof payload.sequence === 'number') {
        if (payload.sequence !== nextLifecycleSequence) {
          callbacks.onLifecycleGap?.(nextLifecycleSequence, payload.sequence)
        }
        nextLifecycleSequence = payload.sequence + 1
      }
      if (payload.type === 'workflow.started' && typeof payload.runId === 'string') {
        started = true
        callbacks.onRunStarted(payload.runId)
        return
      }
      if (payload.type === 'workflow.node.started' && isWorkflowNodeStarted(payload)) {
        callbacks.onNodeStarted({ requestId: payload.nodeRequestId, startedAt: payload.startedAt, sequence: payload.sequence as number | undefined })
        return
      }
      if (payload.type === 'workflow.node.completed' && isWorkflowNodeResult(payload.node)) {
        callbacks.onNodeCompleted(payload.node)
        return
      }
      if (payload.type === 'workflow.completed' && isWorkflowRunResult(payload.run)) {
        if (settled) return
        settled = true
        socket.close()
        resolve(payload.run)
        return
      }
      if (payload.type === 'workflow.error') {
        fail(typeof payload.message === 'string' ? payload.message : 'Workflow WebSocket run failed.', !started)
      }
    })
    socket.addEventListener('error', () => fail('Workflow live connection failed.', !started))
    socket.addEventListener('close', () => fail('Workflow live connection closed before completion.', !started))
  })
}

function workflowWebSocketUrl(): string {
  const url = new URL(RuntimeConfiguration.current.apiBaseUrl)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  url.pathname = `${url.pathname.replace(/\/$/, '')}${RuntimeConfiguration.current.workflowWebSocketPath}`
  return url.toString()
}

function parseEvent(data: unknown): Record<string, unknown> | undefined {
  if (typeof data !== 'string') return undefined
  try {
    const parsed: unknown = JSON.parse(data)
    return parsed !== null && typeof parsed === 'object' ? parsed as Record<string, unknown> : undefined
  } catch {
    return undefined
  }
}

function isNodeOutcome(value: unknown): value is WorkflowNodeOutcome {
  if (value === null || typeof value !== 'object') return false
  const outcome = value as Record<string, unknown>
  return typeof outcome.requestId === 'string'
    && typeof outcome.failed === 'boolean'
    && typeof outcome.attempts === 'number'
    && typeof outcome.port === 'string'
    && (outcome.message === undefined || typeof outcome.message === 'string')
}

function isWorkflowNodeResult(value: unknown): value is WorkflowNodeResult {
  if (value === null || typeof value !== 'object') return false
  const node = value as Record<string, unknown>
  return typeof node.nodeId === 'string'
    && (node.nodePath === undefined || node.nodePath === null || typeof node.nodePath === 'string')
    && typeof node.variable === 'string'
    && typeof node.startedAt === 'number'
    && typeof node.completedAt === 'number'
    && node.completedAt >= node.startedAt
    && isNodeOutcome(node.outcome)
}

function isWorkflowNodeStarted(value: Record<string, unknown>): value is Record<string, unknown> & { nodeRequestId: string, startedAt: number } {
  return typeof value.nodeRequestId === 'string'
    && typeof value.startedAt === 'number'
}

function isWorkflowRunResult(value: unknown): value is WorkflowRunResult {
  return value !== null && typeof value === 'object'
}

function messageFor(error: unknown): string {
  return error instanceof Error ? error.message : 'Workflow live connection could not be opened.'
}

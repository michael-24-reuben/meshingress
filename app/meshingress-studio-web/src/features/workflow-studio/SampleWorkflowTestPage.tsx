import React, { useState, useEffect, useRef, useMemo } from 'react'
import { initialNodes, initialEdges, workflowName } from './compilation/sample-workflow'
import { workflowDefinition, type WorkflowDefinitionPayload } from './compilation/definition'
import { ToolResultStore } from './storage/tool-result-store'
import { RuntimeConfiguration } from '../../runtime/RuntimeConfiguration'
import { apiRequest } from '../../api/client'
import type { WorkflowNode, WorkflowEdge, WorkflowNodeResult, WorkflowRunResult, NodeRunState } from './types'

export interface RawConsoleMessage {
  id: string
  timestamp: Date
  relativeMs: number
  direction: 'WS_IN' | 'WS_OUT' | 'HTTP_REQ' | 'HTTP_RES' | 'SYS' | 'STORE' | 'ERROR'
  eventType?: string
  summary: string
  rawPayload: unknown
}

interface NodeExecutionInfo {
  id: string
  status: NodeRunState
  startedAt?: number
  completedAt?: number
  durationMs?: number
  outcome?: {
    failed: boolean
    attempts: number
    port: string
    message?: string
  }
  result?: unknown
  storedRecordPath?: string
}

export function SampleWorkflowTestPage({ onBackToStudio }: { onBackToStudio?: () => void }) {
  const [nodes] = useState<WorkflowNode[]>(initialNodes)
  const [edges] = useState<WorkflowEdge[]>(initialEdges)
  const [activeTab, setActiveTab] = useState<'console' | 'definition' | 'results' | 'store' | 'arguments'>('console')
  const [runningMode, setRunningMode] = useState<'idle' | 'ws' | 'http' | 'mock'>('idle')
  const [runStatus, setRunStatus] = useState<'idle' | 'running' | 'completed' | 'failed'>('idle')
  const [runId, setRunId] = useState<string>('')
  const [rawMessages, setRawMessages] = useState<RawConsoleMessage[]>([])
  const [nodeExecutions, setNodeExecutions] = useState<Record<string, NodeExecutionInfo>>(() =>
    Object.fromEntries(initialNodes.map((n) => [n.id, { id: n.id, status: 'idle' }]))
  )
  const [finalRunResult, setFinalRunResult] = useState<WorkflowRunResult | null>(null)
  const [storedRecords, setStoredRecords] = useState<Array<{ nodeId: string; fullPath: string; contents: unknown }>>([])

  // Console filters
  const [filterType, setFilterType] = useState<'all' | 'ws' | 'http' | 'results' | 'errors'>('all')
  const [searchTerm, setSearchTerm] = useState('')
  const [autoScroll, setAutoScroll] = useState(true)
  const [expandedMessageIds, setExpandedMessageIds] = useState<Set<string>>(new Set())

  // Custom editable node arguments
  const [customArgs, setCustomArgs] = useState<Record<string, Record<string, string>>>(() => {
    const args: Record<string, Record<string, string>> = {}
    initialNodes.forEach((node) => {
      args[node.id] = { ...node.arguments as Record<string, string> }
    })
    return args
  })

  const consoleEndRef = useRef<HTMLDivElement | null>(null)
  const activeSocketRef = useRef<WebSocket | null>(null)
  const runStartTimeRef = useRef<number>(0)
  const toolStoreRef = useRef<ToolResultStore>(new ToolResultStore(workflowName))

  // Compile definition
  const compiledDefinition: WorkflowDefinitionPayload = useMemo(() => {
    const updatedNodes = nodes.map((node) => ({
      ...node,
      arguments: customArgs[node.id] || node.arguments,
    }))
    return workflowDefinition(updatedNodes, edges)
  }, [nodes, edges, customArgs])

  const appendLog = (
    direction: RawConsoleMessage['direction'],
    summary: string,
    rawPayload: unknown,
    eventType?: string
  ) => {
    const now = new Date()
    const relativeMs = runStartTimeRef.current ? Date.now() - runStartTimeRef.current : 0
    const newMsg: RawConsoleMessage = {
      id: `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
      timestamp: now,
      relativeMs,
      direction,
      eventType,
      summary,
      rawPayload,
    }
    setRawMessages((prev) => [...prev, newMsg])
  }

  useEffect(() => {
    if (autoScroll && consoleEndRef.current) {
      consoleEndRef.current.scrollIntoView({ behavior: 'smooth' })
    }
  }, [rawMessages, autoScroll])

  const getWebSocketUrl = () => {
    const url = new URL(RuntimeConfiguration.current.apiBaseUrl)
    url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
    url.pathname = `${url.pathname.replace(/\/$/, '')}${RuntimeConfiguration.current.workflowWebSocketPath}`
    return url.toString()
  }

  const resetStateForRun = (mode: 'ws' | 'http' | 'mock') => {
    runStartTimeRef.current = Date.now()
    setRunningMode(mode)
    setRunStatus('running')
    setRunId('')
    setFinalRunResult(null)
    setStoredRecords([])
    toolStoreRef.current = new ToolResultStore({ workflowName })

    setNodeExecutions(
      Object.fromEntries(
        nodes.map((n) => [
          n.id,
          { id: n.id, status: 'idle', durationMs: undefined, outcome: undefined, result: undefined },
        ])
      )
    )
  }

  // Run via Live WebSocket
  const runLiveWebSocket = () => {
    if (runningMode !== 'idle') return
    resetStateForRun('ws')

    const wsUrl = getWebSocketUrl()
    const requestId = crypto.randomUUID()
    const outboundPayload = { action: 'run', definition: compiledDefinition, requestId }

    appendLog('SYS', `Opening WebSocket connection to ${wsUrl}`, { url: wsUrl })

    try {
      const socket = new WebSocket(wsUrl)
      activeSocketRef.current = socket

      socket.addEventListener('open', () => {
        appendLog('SYS', 'WebSocket connection established (OPEN). Sending run action frame...', null)
        appendLog('WS_OUT', `Action: run [requestId=${requestId}]`, outboundPayload, 'action.run')
        socket.send(JSON.stringify(outboundPayload))
      })

      socket.addEventListener('message', (event) => {
        const rawText = typeof event.data === 'string' ? event.data : ''
        let parsed: Record<string, unknown> | null = null
        try {
          parsed = JSON.parse(rawText)
        } catch {
          // not json
        }

        const eventType = typeof parsed?.type === 'string' ? (parsed.type as string) : 'raw.message'
        appendLog('WS_IN', `Frame received: ${eventType}`, parsed ?? rawText, eventType)

        if (!parsed || parsed.requestId !== requestId) {
          return
        }

        // 1. workflow.started
        if (parsed.type === 'workflow.started') {
          const rId = String(parsed.runId || '')
          setRunId(rId)
          appendLog('SYS', `Workflow run started with runId: ${rId}`, parsed)
        }

        // 2. workflow.node.started
        if (parsed.type === 'workflow.node.started') {
          const nodeReqId = String(parsed.nodeRequestId || '')
          const startedAt = Number(parsed.startedAt) || Date.now()
          setNodeExecutions((prev) => ({
            ...prev,
            [nodeReqId]: {
              ...(prev[nodeReqId] || { id: nodeReqId }),
              status: 'running',
              startedAt,
            },
          }))
          appendLog('SYS', `Node started execution: ${nodeReqId}`, parsed)
        }

        // 3. workflow.node.completed
        if (parsed.type === 'workflow.node.completed' && parsed.node && typeof parsed.node === 'object') {
          const nodeResult = parsed.node as WorkflowNodeResult
          const nodeId = nodeResult.nodeId || nodeResult.outcome?.requestId || ''
          const duration = nodeResult.completedAt - nodeResult.startedAt
          const isError = Boolean(nodeResult.outcome?.failed)

          // Save to store simulation
          const savedPath = ToolResultStore.buildPath(workflowName, new Date(), nodeId, nodeResult.nodePath || 'tool')

          setStoredRecords((prev) => [
            ...prev,
            { nodeId, fullPath: savedPath, contents: nodeResult },
          ])

          setNodeExecutions((prev) => ({
            ...prev,
            [nodeId]: {
              id: nodeId,
              status: isError ? 'error' : 'success',
              startedAt: nodeResult.startedAt,
              completedAt: nodeResult.completedAt,
              durationMs: duration,
              outcome: nodeResult.outcome,
              result: nodeResult.result,
              storedRecordPath: savedPath,
            },
          }))

          appendLog(
            'STORE',
            `Stored node result for [${nodeId}] -> ${savedPath}`,
            { nodeId, path: savedPath, nodeResult }
          )
        }

        // 4. workflow.completed
        if (parsed.type === 'workflow.completed') {
          const runRes = (parsed.run as WorkflowRunResult) || {}
          setFinalRunResult(runRes)
          setRunStatus('completed')
          setRunningMode('idle')
          appendLog('SYS', `Workflow run completed successfully [status: ${runRes.status || 'OK'}]`, runRes)
          try {
            socket.close()
          } catch {
            // ignore
          }
        }

        // 5. workflow.error
        if (parsed.type === 'workflow.error') {
          const errMsg = String(parsed.message || 'Workflow execution error')
          setRunStatus('failed')
          setRunningMode('idle')
          appendLog('ERROR', `Workflow execution error: ${errMsg}`, parsed)
          try {
            socket.close()
          } catch {
            // ignore
          }
        }
      })

      socket.addEventListener('error', (err) => {
        appendLog('ERROR', 'WebSocket transport error. Make sure backend is running or try HTTP/Mock.', err)
        setRunStatus('failed')
        setRunningMode('idle')
      })

      socket.addEventListener('close', (event) => {
        appendLog('SYS', `WebSocket connection closed (code=${event.code}, reason=${event.reason || 'normal'})`, {
          code: event.code,
          reason: event.reason,
        })
        setRunningMode((prev) => (prev === 'ws' ? 'idle' : prev))
      })
    } catch (err) {
      appendLog('ERROR', `Failed to initialize WebSocket: ${err instanceof Error ? err.message : String(err)}`, err)
      setRunStatus('failed')
      setRunningMode('idle')
    }
  }

  // Run via HTTP POST
  const runHttp = async () => {
    if (runningMode !== 'idle') return
    resetStateForRun('http')

    const path = '/api/v1/workflows/run'
    const endpoint = `${RuntimeConfiguration.current.apiBaseUrl}${path}`

    appendLog('HTTP_REQ', `POST ${endpoint}`, compiledDefinition, 'request.run')

    try {
      // Mark trigger as running
      setNodeExecutions((prev) => ({
        ...prev,
        'r-001': { id: 'r-001', status: 'running', startedAt: Date.now() },
      }))

      const result = await apiRequest<WorkflowRunResult>(path, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Mcp-Session-Id': crypto.randomUUID(),
          'X-Request-Id': crypto.randomUUID(),
        },
        body: JSON.stringify(compiledDefinition),
      })

      appendLog('HTTP_RES', `200 OK Workflow Run Result [runId=${result.runId}]`, result, 'response.run')
      setFinalRunResult(result)
      setRunId(result.runId || 'run-http')
      setRunStatus('completed')

      // Process node results
      const nextExecs: Record<string, NodeExecutionInfo> = {}
      result.nodeResults?.forEach((nr) => {
        const savedPath = ToolResultStore.buildPath(workflowName, new Date(), nr.nodeId, nr.nodePath || 'tool')
        nextExecs[nr.nodeId] = {
          id: nr.nodeId,
          status: nr.outcome.failed ? 'error' : 'success',
          startedAt: nr.startedAt,
          completedAt: nr.completedAt,
          durationMs: nr.completedAt - nr.startedAt,
          outcome: nr.outcome,
          result: nr.result,
          storedRecordPath: savedPath,
        }
        setStoredRecords((prev) => [...prev, { nodeId: nr.nodeId, fullPath: savedPath, contents: nr }])
        appendLog('STORE', `Stored ${nr.nodeId} -> ${savedPath}`, nr)
      })
      setNodeExecutions((prev) => ({ ...prev, ...nextExecs }))
    } catch (err) {
      appendLog('ERROR', `HTTP Request failed: ${err instanceof Error ? err.message : String(err)}`, err)
      setRunStatus('failed')
    } finally {
      setRunningMode('idle')
    }
  }

  // Simulated Mock Run for testing UI without active backend
  const runMock = async () => {
    if (runningMode !== 'idle') return
    resetStateForRun('mock')

    const mockRunId = `run-mock-${Date.now().toString().slice(-4)}`
    setRunId(mockRunId)
    appendLog('SYS', `Starting Simulated Mock Run [${mockRunId}]`, { definition: compiledDefinition })

    const sequenceOrder = ['r-001', 'r-002', 'r-003', 'r-004']
    const mockOutputs: Record<string, unknown> = {
      'r-001': { triggeredAt: new Date().toISOString(), triggerType: 'manual' },
      'r-002': { stdout: 'I have gained consciousness, and I prefer PowerShell over Bash.', exitCode: 0, status: 'SUCCESS' },
      'r-003': { message: 'Hello Alphasunny! Welcome to Meshingress Workflow Engine.', timestamp: Date.now() },
      'r-004': {
        query: 'solo leveling',
        results: [
          { title: 'Solo Leveling: Ragnarok', rank: 1, views: '14.2M', chapters: 68 },
          { title: 'Solo Leveling (Original)', rank: 2, views: '98.5M', chapters: 201 },
          { title: 'Solo Leveling Side Story', rank: 3, views: '8.1M', chapters: 21 },
        ],
      },
    }

    appendLog('WS_IN', `workflow.started: runId=${mockRunId}`, { type: 'workflow.started', runId: mockRunId, sequence: 1 }, 'workflow.started')

    for (let i = 0; i < sequenceOrder.length; i++) {
      const nodeId = sequenceOrder[i]
      const node = nodes.find((n) => n.id === nodeId)!
      const startedAt = Date.now()

      // Node started
      setNodeExecutions((prev) => ({
        ...prev,
        [nodeId]: { id: nodeId, status: 'running', startedAt },
      }))
      appendLog('WS_IN', `workflow.node.started: ${nodeId} (${node.title})`, {
        type: 'workflow.node.started',
        nodeRequestId: nodeId,
        startedAt,
        sequence: i * 2 + 2,
      }, 'workflow.node.started')

      // Simulated network/execution delay
      await new Promise((r) => setTimeout(r, 600 + Math.random() * 400))

      const completedAt = Date.now()
      const durationMs = completedAt - startedAt
      const nodeResult: WorkflowNodeResult = {
        nodeId,
        nodePath: node.kind === 'tool' ? `${node.toolId}.${node.functionName}` : 'trigger.manual',
        variable: node.output,
        startedAt,
        completedAt,
        outcome: {
          requestId: nodeId,
          failed: false,
          attempts: 1,
          port: 'output',
        },
        result: mockOutputs[nodeId],
      }

      const savedPath = ToolResultStore.buildPath(workflowName, new Date(), nodeId, nodeResult.nodePath || 'tool')

      setNodeExecutions((prev) => ({
        ...prev,
        [nodeId]: {
          id: nodeId,
          status: 'success',
          startedAt,
          completedAt,
          durationMs,
          outcome: nodeResult.outcome,
          result: nodeResult.result,
          storedRecordPath: savedPath,
        },
      }))

      setStoredRecords((prev) => [...prev, { nodeId, fullPath: savedPath, contents: nodeResult }])

      appendLog('WS_IN', `workflow.node.completed: ${nodeId}`, {
        type: 'workflow.node.completed',
        node: nodeResult,
        sequence: i * 2 + 3,
      }, 'workflow.node.completed')

      appendLog('STORE', `Stored simulated result for ${nodeId} -> ${savedPath}`, nodeResult)
    }

    const finalResult: WorkflowRunResult = {
      runId: mockRunId,
      status: 'COMPLETED',
      results: mockOutputs,
      nodeResults: sequenceOrder.map((id) => ({
        nodeId: id,
        variable: nodes.find((n) => n.id === id)!.output,
        startedAt: Date.now() - 2500,
        completedAt: Date.now(),
        outcome: { requestId: id, failed: false, attempts: 1, port: 'output' },
        result: mockOutputs[id],
      })),
    }

    setFinalRunResult(finalResult)
    setRunStatus('completed')
    setRunningMode('idle')
    appendLog('WS_IN', `workflow.completed: ${mockRunId}`, {
      type: 'workflow.completed',
      run: finalResult,
      sequence: 10,
    }, 'workflow.completed')
  }

  const abortRun = () => {
    if (activeSocketRef.current) {
      try {
        activeSocketRef.current.close()
      } catch {
        // ignore
      }
      activeSocketRef.current = null
    }
    setRunningMode('idle')
    setRunStatus('failed')
    appendLog('SYS', 'User requested workflow run abort / disconnected.', null)
  }

  const clearConsole = () => {
    setRawMessages([])
  }

  const copyConsoleJson = () => {
    const text = JSON.stringify(rawMessages, null, 2)
    navigator.clipboard.writeText(text)
    appendLog('SYS', 'Copied raw console logs to clipboard.', { messageCount: rawMessages.length })
  }

  const toggleExpand = (id: string) => {
    setExpandedMessageIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const filteredMessages = useMemo(() => {
    return rawMessages.filter((msg) => {
      if (filterType === 'ws' && !msg.direction.startsWith('WS')) return false
      if (filterType === 'http' && !msg.direction.startsWith('HTTP')) return false
      if (filterType === 'results' && msg.direction !== 'STORE' && !msg.eventType?.includes('completed')) return false
      if (filterType === 'errors' && msg.direction !== 'ERROR' && !msg.summary.toLowerCase().includes('error')) return false
      if (searchTerm) {
        const text = `${msg.direction} ${msg.summary} ${msg.eventType} ${JSON.stringify(msg.rawPayload)}`.toLowerCase()
        if (!text.includes(searchTerm.toLowerCase())) return false
      }
      return true
    })
  }, [rawMessages, filterType, searchTerm])

  const getDirectionBadgeStyle = (dir: RawConsoleMessage['direction']): React.CSSProperties => {
    switch (dir) {
      case 'WS_IN':
        return { backgroundColor: '#065f46', color: '#6ee7b7', border: '1px solid #047857' }
      case 'WS_OUT':
        return { backgroundColor: '#1e3a8a', color: '#93c5fd', border: '1px solid #2563eb' }
      case 'HTTP_REQ':
        return { backgroundColor: '#701a75', color: '#f5d0fe', border: '1px solid #a21caf' }
      case 'HTTP_RES':
        return { backgroundColor: '#831843', color: '#fbcfe8', border: '1px solid #be185d' }
      case 'STORE':
        return { backgroundColor: '#78350f', color: '#fde68a', border: '1px solid #b45309' }
      case 'ERROR':
        return { backgroundColor: '#7f1d1d', color: '#fca5a5', border: '1px solid #dc2626' }
      case 'SYS':
      default:
        return { backgroundColor: '#27272a', color: '#d4d4d8', border: '1px solid #3f3f46' }
    }
  }

  const getStatusBadgeStyle = (status: NodeRunState | 'completed' | 'failed' | 'running' | 'idle'): React.CSSProperties => {
    switch (status) {
      case 'running':
        return { backgroundColor: '#1e3a8a', color: '#bfdbfe', border: '1px solid #3b82f6', animation: 'pulse 2s infinite' }
      case 'success':
      case 'completed':
        return { backgroundColor: '#064e3b', color: '#a7f3d0', border: '1px solid #10b981' }
      case 'error':
      case 'failed':
        return { backgroundColor: '#7f1d1d', color: '#fecaca', border: '1px solid #ef4444' }
      case 'idle':
      default:
        return { backgroundColor: '#27272a', color: '#a1a1aa', border: '1px solid #3f3f46' }
    }
  }

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        width: '100%',
        backgroundColor: '#09090b',
        color: '#f4f4f5',
        fontFamily: 'ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, monospace',
        overflow: 'hidden',
      }}
    >
      {/* Header bar */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '8px 16px',
          backgroundColor: '#18181b',
          borderBottom: '1px solid #27272a',
          gap: '12px',
          flexShrink: 0,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          {onBackToStudio && (
            <button
              onClick={onBackToStudio}
              style={{
                padding: '4px 10px',
                fontSize: '12px',
                borderRadius: '4px',
                backgroundColor: '#27272a',
                color: '#e4e4e7',
                border: '1px solid #3f3f46',
                cursor: 'pointer',
              }}
            >
              ← Back to Studio
            </button>
          )}
          <div>
            <div style={{ fontWeight: 700, fontSize: '14px', color: '#38bdf8' }}>
              Sample Workflow Live Test Runner
            </div>
            <div style={{ fontSize: '11px', color: '#a1a1aa' }}>
              {workflowName} ({nodes.length} nodes, {edges.length} edges)
            </div>
          </div>
        </div>

        {/* Server & Status Info */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '11px' }}>
          <span style={{ color: '#71717a' }}>Target API:</span>
          <code style={{ backgroundColor: '#27272a', padding: '2px 6px', borderRadius: '4px', color: '#fbbf24' }}>
            {RuntimeConfiguration.current.apiBaseUrl}
          </code>
          <span style={{ color: '#71717a' }}>WS Path:</span>
          <code style={{ backgroundColor: '#27272a', padding: '2px 6px', borderRadius: '4px', color: '#a7f3d0' }}>
            {RuntimeConfiguration.current.workflowWebSocketPath}
          </code>
          <div
            style={{
              padding: '3px 8px',
              borderRadius: '9999px',
              fontSize: '11px',
              fontWeight: 600,
              textTransform: 'uppercase',
              ...getStatusBadgeStyle(runStatus),
            }}
          >
            {runningMode !== 'idle' ? `Running (${runningMode.toUpperCase()})` : runStatus}
          </div>
          {runId && (
            <span style={{ color: '#93c5fd', fontSize: '11px' }}>
              RunId: <strong>{runId}</strong>
            </span>
          )}
        </div>

        {/* Action Buttons */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <button
            onClick={runLiveWebSocket}
            disabled={runningMode !== 'idle'}
            style={{
              padding: '6px 14px',
              fontSize: '12px',
              fontWeight: 600,
              borderRadius: '4px',
              backgroundColor: runningMode === 'ws' ? '#1e3a8a' : '#2563eb',
              color: '#ffffff',
              border: 'none',
              cursor: runningMode !== 'idle' ? 'not-allowed' : 'pointer',
              opacity: runningMode !== 'idle' ? 0.6 : 1,
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
            }}
            title="Execute workflow over live WebSocket"
          >
            ▶ Run Live (WebSocket)
          </button>

          <button
            onClick={runHttp}
            disabled={runningMode !== 'idle'}
            style={{
              padding: '6px 12px',
              fontSize: '12px',
              fontWeight: 500,
              borderRadius: '4px',
              backgroundColor: '#9333ea',
              color: '#ffffff',
              border: 'none',
              cursor: runningMode !== 'idle' ? 'not-allowed' : 'pointer',
              opacity: runningMode !== 'idle' ? 0.6 : 1,
            }}
            title="Execute workflow via HTTP POST /api/v1/workflows/run"
          >
            ⚡ Run HTTP
          </button>

          <button
            onClick={runMock}
            disabled={runningMode !== 'idle'}
            style={{
              padding: '6px 12px',
              fontSize: '12px',
              fontWeight: 500,
              borderRadius: '4px',
              backgroundColor: '#059669',
              color: '#ffffff',
              border: 'none',
              cursor: runningMode !== 'idle' ? 'not-allowed' : 'pointer',
              opacity: runningMode !== 'idle' ? 0.6 : 1,
            }}
            title="Run simulated workflow with realistic live WebSocket events"
          >
            🎭 Run Mock Simulation
          </button>

          {runningMode !== 'idle' && (
            <button
              onClick={abortRun}
              style={{
                padding: '6px 12px',
                fontSize: '12px',
                fontWeight: 600,
                borderRadius: '4px',
                backgroundColor: '#dc2626',
                color: '#ffffff',
                border: 'none',
                cursor: 'pointer',
              }}
            >
              ⏹ Abort
            </button>
          )}
        </div>
      </div>

      {/* Node Pipeline Preview Cards */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
          gap: '12px',
          padding: '12px 16px',
          backgroundColor: '#111113',
          borderBottom: '1px solid #27272a',
          flexShrink: 0,
        }}
      >
        {nodes.map((node, index) => {
          const exec = nodeExecutions[node.id] || { id: node.id, status: 'idle' }
          return (
            <div
              key={node.id}
              style={{
                backgroundColor: '#18181b',
                border: exec.status === 'running' ? '1px solid #3b82f6' : '1px solid #27272a',
                borderRadius: '6px',
                padding: '10px 12px',
                display: 'flex',
                flexDirection: 'column',
                gap: '6px',
                boxShadow: exec.status === 'running' ? '0 0 10px rgba(59, 130, 246, 0.2)' : 'none',
                position: 'relative',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <span
                    style={{
                      fontSize: '10px',
                      fontWeight: 700,
                      backgroundColor: '#27272a',
                      color: '#a1a1aa',
                      padding: '1px 5px',
                      borderRadius: '3px',
                    }}
                  >
                    #{index + 1}
                  </span>
                  <span style={{ fontSize: '13px', fontWeight: 600, color: '#e4e4e7' }}>{node.title}</span>
                </div>
                <div
                  style={{
                    fontSize: '10px',
                    fontWeight: 700,
                    padding: '2px 6px',
                    borderRadius: '4px',
                    textTransform: 'uppercase',
                    ...getStatusBadgeStyle(exec.status),
                  }}
                >
                  {exec.status}
                </div>
              </div>

              <div style={{ fontSize: '11px', color: '#71717a' }}>
                ID: <code style={{ color: '#38bdf8' }}>{node.id}</code> &bull; Output:{' '}
                <code style={{ color: '#a7f3d0' }}>${node.output}</code>
              </div>

              <div style={{ fontSize: '11px', color: '#94a3b8', fontFamily: 'monospace' }}>
                {node.kind === 'trigger' ? 'trigger.manual.start' : `${node.toolId}.${node.functionName}`}
              </div>

              {exec.durationMs !== undefined && (
                <div style={{ fontSize: '10px', color: '#6ee7b7' }}>
                  ⏱ Duration: <strong>{exec.durationMs}ms</strong>
                </div>
              )}

              {exec.outcome?.failed && (
                <div style={{ fontSize: '11px', color: '#fca5a5', backgroundColor: '#450a0a', padding: '4px', borderRadius: '3px' }}>
                  Error: {exec.outcome.message || 'Execution failed'}
                </div>
              )}

              {exec.result !== undefined && (
                <div
                  style={{
                    marginTop: '4px',
                    fontSize: '10px',
                    backgroundColor: '#09090b',
                    padding: '4px 6px',
                    borderRadius: '4px',
                    border: '1px solid #27272a',
                    maxHeight: '60px',
                    overflowY: 'auto',
                    fontFamily: 'monospace',
                    color: '#d1d5db',
                  }}
                >
                  {JSON.stringify(exec.result)}
                </div>
              )}
            </div>
          )
        })}
      </div>

      {/* Tabs navigation */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0 16px',
          backgroundColor: '#18181b',
          borderBottom: '1px solid #27272a',
          flexShrink: 0,
        }}
      >
        <div style={{ display: 'flex', gap: '2px' }}>
          {[
            { key: 'console', label: `Live Raw Console (${filteredMessages.length})` },
            { key: 'definition', label: 'Compiled Definition JSON' },
            { key: 'results', label: `Final Result Data ${finalRunResult ? '✓' : ''}` },
            { key: 'store', label: `Tool Store History (${storedRecords.length})` },
            { key: 'arguments', label: 'Edit Node Arguments' },
          ].map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key as any)}
              style={{
                padding: '8px 14px',
                fontSize: '12px',
                fontWeight: activeTab === tab.key ? 600 : 400,
                color: activeTab === tab.key ? '#60a5fa' : '#a1a1aa',
                backgroundColor: activeTab === tab.key ? '#09090b' : 'transparent',
                border: 'none',
                borderBottom: activeTab === tab.key ? '2px solid #3b82f6' : '2px solid transparent',
                cursor: 'pointer',
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {activeTab === 'console' && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '11px' }}>
            {/* Filter buttons */}
            <div style={{ display: 'flex', backgroundColor: '#09090b', borderRadius: '4px', padding: '2px', border: '1px solid #27272a' }}>
              {(['all', 'ws', 'http', 'results', 'errors'] as const).map((f) => (
                <button
                  key={f}
                  onClick={() => setFilterType(f)}
                  style={{
                    padding: '2px 8px',
                    fontSize: '10px',
                    borderRadius: '3px',
                    border: 'none',
                    backgroundColor: filterType === f ? '#27272a' : 'transparent',
                    color: filterType === f ? '#f4f4f5' : '#71717a',
                    cursor: 'pointer',
                    textTransform: 'uppercase',
                  }}
                >
                  {f}
                </button>
              ))}
            </div>

            <input
              type="text"
              placeholder="Search raw stream..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              style={{
                backgroundColor: '#09090b',
                border: '1px solid #27272a',
                color: '#f4f4f5',
                fontSize: '11px',
                padding: '3px 8px',
                borderRadius: '4px',
                width: '140px',
              }}
            />

            <label style={{ display: 'flex', alignItems: 'center', gap: '4px', cursor: 'pointer', color: '#a1a1aa' }}>
              <input
                type="checkbox"
                checked={autoScroll}
                onChange={(e) => setAutoScroll(e.target.checked)}
              />
              Auto-scroll
            </label>

            <button
              onClick={copyConsoleJson}
              style={{
                padding: '3px 8px',
                fontSize: '11px',
                backgroundColor: '#27272a',
                border: '1px solid #3f3f46',
                color: '#e4e4e7',
                borderRadius: '4px',
                cursor: 'pointer',
              }}
            >
              📋 Copy Logs
            </button>

            <button
              onClick={clearConsole}
              style={{
                padding: '3px 8px',
                fontSize: '11px',
                backgroundColor: '#27272a',
                border: '1px solid #3f3f46',
                color: '#f87171',
                borderRadius: '4px',
                cursor: 'pointer',
              }}
            >
              Clear
            </button>
          </div>
        )}
      </div>

      {/* Content Area */}
      <div style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
        {/* Tab 1: Live Console */}
        {activeTab === 'console' && (
          <div
            style={{
              flex: 1,
              overflowY: 'auto',
              padding: '12px 16px',
              backgroundColor: '#09090b',
              fontFamily: 'Consolas, Monaco, "Courier New", monospace',
              fontSize: '12px',
              lineHeight: '1.5',
            }}
          >
            {filteredMessages.length === 0 ? (
              <div style={{ color: '#52525b', textAlign: 'center', marginTop: '40px' }}>
                No console messages yet. Click <strong>▶ Run Live (WebSocket)</strong> or <strong>⚡ Run HTTP</strong> to start streaming workflow data.
              </div>
            ) : (
              filteredMessages.map((msg) => {
                const isExpanded = expandedMessageIds.has(msg.id)
                const payloadStr =
                  msg.rawPayload !== null && msg.rawPayload !== undefined
                    ? typeof msg.rawPayload === 'string'
                      ? msg.rawPayload
                      : JSON.stringify(msg.rawPayload, null, 2)
                    : ''

                return (
                  <div
                    key={msg.id}
                    style={{
                      marginBottom: '6px',
                      padding: '6px 10px',
                      backgroundColor: '#121215',
                      border: '1px solid #1f1f23',
                      borderRadius: '4px',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: '4px',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '8px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span style={{ color: '#71717a', fontSize: '11px' }}>
                          {msg.timestamp.toTimeString().split(' ')[0]}.{String(msg.timestamp.getMilliseconds()).padStart(3, '0')}
                        </span>
                        <span style={{ color: '#52525b', fontSize: '10px' }}>+{msg.relativeMs}ms</span>
                        <span
                          style={{
                            fontSize: '10px',
                            fontWeight: 700,
                            padding: '1px 5px',
                            borderRadius: '3px',
                            ...getDirectionBadgeStyle(msg.direction),
                          }}
                        >
                          {msg.direction}
                        </span>
                        {msg.eventType && (
                          <span
                            style={{
                              fontSize: '10px',
                              backgroundColor: '#18181b',
                              color: '#38bdf8',
                              border: '1px solid #0284c7',
                              padding: '1px 5px',
                              borderRadius: '3px',
                            }}
                          >
                            {msg.eventType}
                          </span>
                        )}
                        <span style={{ color: '#e4e4e7', fontWeight: 500 }}>{msg.summary}</span>
                      </div>

                      {payloadStr && (
                        <button
                          onClick={() => toggleExpand(msg.id)}
                          style={{
                            fontSize: '11px',
                            color: '#93c5fd',
                            backgroundColor: '#1e293b',
                            border: '1px solid #334155',
                            borderRadius: '3px',
                            padding: '1px 6px',
                            cursor: 'pointer',
                          }}
                        >
                          {isExpanded ? 'Collapse ▲' : 'Expand JSON ▼'}
                        </button>
                      )}
                    </div>

                    {payloadStr && (
                      <pre
                        style={{
                          margin: 0,
                          marginTop: '4px',
                          padding: '8px',
                          backgroundColor: '#09090b',
                          border: '1px solid #27272a',
                          borderRadius: '4px',
                          color: '#a5f3fc',
                          fontSize: '11px',
                          maxHeight: isExpanded ? '600px' : '90px',
                          overflowY: 'auto',
                          whiteSpace: 'pre-wrap',
                          wordBreak: 'break-all',
                        }}
                      >
                        {payloadStr}
                      </pre>
                    )}
                  </div>
                )
              })
            )}
            <div ref={consoleEndRef} />
          </div>
        )}

        {/* Tab 2: Workflow Definition Payload */}
        {activeTab === 'definition' && (
          <div style={{ flex: 1, padding: '16px', overflowY: 'auto', backgroundColor: '#09090b' }}>
            <div style={{ marginBottom: '10px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span style={{ fontSize: '13px', color: '#a1a1aa' }}>
                This is the compiled payload passed to <code>/api/v1/workflows/ws</code> or <code>/api/v1/workflows/run</code>:
              </span>
              <button
                onClick={() => navigator.clipboard.writeText(JSON.stringify(compiledDefinition, null, 2))}
                style={{
                  padding: '4px 10px',
                  fontSize: '11px',
                  backgroundColor: '#27272a',
                  color: '#e4e4e7',
                  border: '1px solid #3f3f46',
                  borderRadius: '4px',
                  cursor: 'pointer',
                }}
              >
                Copy Definition JSON
              </button>
            </div>
            <pre
              style={{
                backgroundColor: '#121215',
                border: '1px solid #27272a',
                borderRadius: '6px',
                padding: '16px',
                color: '#67e8f9',
                fontFamily: 'monospace',
                fontSize: '12px',
                overflowX: 'auto',
              }}
            >
              {JSON.stringify(compiledDefinition, null, 2)}
            </pre>
          </div>
        )}

        {/* Tab 3: Final Results */}
        {activeTab === 'results' && (
          <div style={{ flex: 1, padding: '16px', overflowY: 'auto', backgroundColor: '#09090b' }}>
            {finalRunResult ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                <div style={{ backgroundColor: '#18181b', padding: '12px', borderRadius: '6px', border: '1px solid #27272a' }}>
                  <div style={{ fontWeight: 600, color: '#38bdf8', marginBottom: '6px' }}>Run Summary</div>
                  <div style={{ fontSize: '12px', color: '#d4d4d8', display: 'flex', gap: '16px' }}>
                    <div>Run ID: <strong>{finalRunResult.runId || 'N/A'}</strong></div>
                    <div>Status: <strong>{finalRunResult.status || 'OK'}</strong></div>
                    <div>Node Results Count: <strong>{finalRunResult.nodeResults?.length || 0}</strong></div>
                  </div>
                </div>

                <div>
                  <div style={{ fontWeight: 600, color: '#a7f3d0', marginBottom: '8px' }}>Compiled Bound Variables (results):</div>
                  <pre
                    style={{
                      backgroundColor: '#121215',
                      border: '1px solid #27272a',
                      borderRadius: '6px',
                      padding: '12px',
                      color: '#a7f3d0',
                      fontFamily: 'monospace',
                      fontSize: '12px',
                      overflowX: 'auto',
                    }}
                  >
                    {JSON.stringify(finalRunResult.results || {}, null, 2)}
                  </pre>
                </div>

                <div>
                  <div style={{ fontWeight: 600, color: '#93c5fd', marginBottom: '8px' }}>Individual Node Execution Results:</div>
                  <pre
                    style={{
                      backgroundColor: '#121215',
                      border: '1px solid #27272a',
                      borderRadius: '6px',
                      padding: '12px',
                      color: '#93c5fd',
                      fontFamily: 'monospace',
                      fontSize: '12px',
                      overflowX: 'auto',
                    }}
                  >
                    {JSON.stringify(finalRunResult.nodeResults || [], null, 2)}
                  </pre>
                </div>
              </div>
            ) : (
              <div style={{ color: '#71717a', textAlign: 'center', marginTop: '40px' }}>
                No completed run result yet. Execute the workflow to view results.
              </div>
            )}
          </div>
        )}

        {/* Tab 4: Store History */}
        {activeTab === 'store' && (
          <div style={{ flex: 1, padding: '16px', overflowY: 'auto', backgroundColor: '#09090b' }}>
            <div style={{ marginBottom: '12px', color: '#a1a1aa', fontSize: '13px' }}>
              Tool Result Store writes outputs to <code>var/runs/&lt;workflow&gt;/&lt;timestamp&gt;/&lt;nodeId&gt;_&lt;nodePath&gt;.out.json</code>:
            </div>
            {storedRecords.length === 0 ? (
              <div style={{ color: '#52525b', textAlign: 'center', marginTop: '40px' }}>
                No tool results stored in this session yet.
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {storedRecords.map((rec, idx) => (
                  <div
                    key={idx}
                    style={{
                      backgroundColor: '#18181b',
                      border: '1px solid #27272a',
                      borderRadius: '6px',
                      padding: '10px 14px',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '6px' }}>
                      <span style={{ color: '#fde68a', fontWeight: 600, fontSize: '12px' }}>
                        Node [{rec.nodeId}]
                      </span>
                      <code style={{ fontSize: '11px', color: '#38bdf8' }}>{rec.fullPath}</code>
                    </div>
                    <pre
                      style={{
                        margin: 0,
                        backgroundColor: '#09090b',
                        border: '1px solid #27272a',
                        borderRadius: '4px',
                        padding: '8px',
                        fontSize: '11px',
                        color: '#d4d4d8',
                        maxHeight: '120px',
                        overflowY: 'auto',
                      }}
                    >
                      {JSON.stringify(rec.contents, null, 2)}
                    </pre>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Tab 5: Edit Node Arguments */}
        {activeTab === 'arguments' && (
          <div style={{ flex: 1, padding: '16px', overflowY: 'auto', backgroundColor: '#09090b' }}>
            <div style={{ marginBottom: '14px', color: '#a1a1aa', fontSize: '13px' }}>
              Customize arguments for sample workflow nodes before executing:
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {nodes.map((node) => {
                const args = customArgs[node.id] || {}
                const argKeys = Object.keys(args)
                return (
                  <div
                    key={node.id}
                    style={{
                      backgroundColor: '#18181b',
                      border: '1px solid #27272a',
                      borderRadius: '6px',
                      padding: '12px',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
                      <span style={{ fontWeight: 600, color: '#38bdf8' }}>[{node.id}] {node.title}</span>
                      <span style={{ fontSize: '11px', color: '#71717a' }}>({node.toolId}.{node.functionName})</span>
                    </div>

                    {argKeys.length === 0 ? (
                      <div style={{ color: '#52525b', fontSize: '11px' }}>No arguments required for this trigger/node.</div>
                    ) : (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                        {argKeys.map((key) => (
                          <div key={key} style={{ display: 'flex', flexDirection: 'column', gap: '3px' }}>
                            <label style={{ fontSize: '11px', color: '#a1a1aa', fontWeight: 500 }}>{key}:</label>
                            {key === 'script' ? (
                              <textarea
                                value={args[key] || ''}
                                onChange={(e) => {
                                  const val = e.target.value
                                  setCustomArgs((prev) => ({
                                    ...prev,
                                    [node.id]: { ...prev[node.id], [key]: val },
                                  }))
                                }}
                                rows={3}
                                style={{
                                  backgroundColor: '#09090b',
                                  border: '1px solid #27272a',
                                  borderRadius: '4px',
                                  color: '#f4f4f5',
                                  padding: '6px',
                                  fontFamily: 'monospace',
                                  fontSize: '11px',
                                  resize: 'vertical',
                                }}
                              />
                            ) : (
                              <input
                                type="text"
                                value={args[key] || ''}
                                onChange={(e) => {
                                  const val = e.target.value
                                  setCustomArgs((prev) => ({
                                    ...prev,
                                    [node.id]: { ...prev[node.id], [key]: val },
                                  }))
                                }}
                                style={{
                                  backgroundColor: '#09090b',
                                  border: '1px solid #27272a',
                                  borderRadius: '4px',
                                  color: '#f4f4f5',
                                  padding: '6px',
                                  fontSize: '12px',
                                }}
                              />
                            )}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

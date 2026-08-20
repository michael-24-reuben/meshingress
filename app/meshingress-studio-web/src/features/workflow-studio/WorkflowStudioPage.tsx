import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties } from 'react'
import { listMcpTools, type McpToolFunction } from '../../api/mcp'
import { validateNativeLogin } from '../../api/auth'
import { setStudioAccessToken } from '../../api/client'
import { readGoogleIdentity } from '../../auth/google-identity'
import { RuntimeConfiguration } from '../../runtime/RuntimeConfiguration'
import { listToolModules, type ToolModuleSummary } from '../../api/tool-modules'
import { runWorkflowHttp, runWorkflowLive, WorkflowLiveTransportError, type WorkflowNodeOutcome } from '../../api/workflow'
import { initialEdges, initialNodes, workflowName } from './sample-workflow'
import { workflowDefinition } from './definition'
import { createToolPresentationIndex } from './node-presentation'
import { ActivityRail } from './components/elements/layout/ActivityRail'
import { StudioLeftPanel } from './components/StudioLeftPanel'
import { PanelSearchDialog } from './components/PanelSearchDialog'
import { InteractiveRail } from './components/elements/InteractiveRail'
import { StudioRightPanel } from './components/StudioRightPanel'
import { TopBar } from './components/TopBar'
import type { StudioProfile } from './components/ProfileMenu'
import { WorkspaceDialog } from './components/WorkspaceDialog'
import { Workbench } from './components/elements/Workbench'
import { getPanelContentKind, getPanelForContentKind, getSurfaceContentKinds, type PanelSurface, type PanelViewContentKind, type PanelViewSetContentParams, type StudioPanel } from './components/panel-catalog'
import type { DrawerPanelViewContentKind, DrawerPanelViewContentParams, LeftPanelViewContentKind, LeftPanelViewContentParams, LogEntry, NodeRunState, RegisteredTool, RightPanelViewContentKind, RightPanelViewContentParams, RuntimeTrace, WorkflowNode, WorkflowRunResult } from './types'
import { DEFAULT_STUDIO_PROPERTIES } from './types'
import { chooseLocalDirectory, chooseStoredDirectory, hasStoredDirectoryHandle, indexWorkspaceBackground, readRecentWorkspaceEntries, readStoredDirectory, storeDirectoryHandle, upsertRecentEntry, writeRecentWorkspaceEntries, type DirectorySelection, type LocalWorkspace, type LocalWorkspaceNode, type RecentWorkspaceEntry } from './local-workspace'
import './workflow-studio.css'
import { StudioDrawerPanel } from './components/StudioDrawerPanel'

const workflowFileName = 'desktop-volume-greeting-solo-leveling.json'

function registeredTools(functions: McpToolFunction[]): RegisteredTool[] {
  return functions
    .map((functionEntry) => ({
      id: functionEntry.name,
      moduleToolId: functionEntry.moduleToolId,
      title: functionEntry.title?.trim() || functionEntry.name,
      description: functionEntry.description,
    }))
    .toSorted((left, right) => left.title.localeCompare(right.title))
}

function newRuntimeTrace(nodes: WorkflowNode[]): RuntimeTrace {
  return {
    startedAt: Date.now(),
    entries: nodes.map((node) => ({
      requestId: node.id,
      variable: node.output,
      initiator: node.kind === 'trigger' ? 'Manual trigger' : `${node.toolId}.${node.functionName}`,
      kind: node.kind,
      status: 'idle',
    })),
  }
}

function firstOpenLane(trace: RuntimeTrace): number {
  const occupied = new Set(trace.entries.filter((entry) => entry.status === 'running').flatMap((entry) => entry.lane === undefined ? [] : [entry.lane]))
  let lane = 0
  while (occupied.has(lane)) lane += 1
  return lane
}

function valueDetails(value: unknown): Pick<RuntimeTrace['entries'][number], 'size' | 'type'> {
  const encoded = JSON.stringify(value)
  const type = value === null ? 'null' : Array.isArray(value) ? 'array' : typeof value
  return { type, size: encoded === undefined ? 0 : new TextEncoder().encode(encoded).byteLength }
}

function nextNodeId(nodes: WorkflowNode[]) {
  const highest = nodes.reduce((current, node) => {
    const match = /^r-(\d+)$/.exec(node.id)
    return match ? Math.max(current, Number(match[1])) : current
  }, 0)
  return `r-${String(highest + 1).padStart(3, '0')}`
}

function outputName(functionName: string, nodes: WorkflowNode[]) {
  const words = functionName.split(/[^A-Za-z0-9_$]+/).filter(Boolean)
  const base = words.map((word, index) => index === 0 ? word.charAt(0).toLowerCase() + word.slice(1) : word.charAt(0).toUpperCase() + word.slice(1)).join('') || 'result'
  const validBase = /^[A-Za-z_$]/.test(base) ? base : `result${base}`
  const used = new Set(nodes.map((node) => node.output))
  let candidate = validBase
  let suffix = 2
  while (used.has(candidate)) candidate = `${validBase}${suffix++}`
  return candidate
}

function nativeValidationTransport() {
  const endpoint = new URL(RuntimeConfiguration.current.apiBaseUrl)
  const loopback = endpoint.hostname === 'localhost' || endpoint.hostname === '127.0.0.1' || endpoint.hostname === '::1'
  return {
    available: endpoint.protocol === 'https:' || loopback,
    notice: 'Native credential validation requires an HTTPS API endpoint or a loopback development server.',
  }
}


export function WorkflowStudioPage() {
  const [nodes, setNodes] = useState(initialNodes)
  const [edges, setEdges] = useState(initialEdges)
  const [leftPanelView, setLeftPanel] = useState<LeftPanelViewContentKind>('tool-catalog')
  const [drawerView, setDrawer] = useState<DrawerPanelViewContentKind>('workflow-activity')
  const [rightPanelView, setRightPanel] = useState<RightPanelViewContentKind>('node-editor')
  const [selectedNodeId, setSelectedNodeId] = useState('r-001')
  const [logs, setLogs] = useState<LogEntry[]>([])
  const [lastRun, setLastRun] = useState<WorkflowRunResult | null>(null)
  const [runtimeTrace, setRuntimeTrace] = useState<RuntimeTrace | null>(null)
  const [runStates, setRunStates] = useState<Record<string, NodeRunState>>({})
  const [running, setRunning] = useState(false)
  const [profile, setProfile] = useState<StudioProfile | null>(null)
  const [authenticationRevision, setAuthenticationRevision] = useState(0)
  const [studioProperties, setStudioProperties] = useState(DEFAULT_STUDIO_PROPERTIES)
  const toggleReattachOnEmptyRelease = () => setStudioProperties((current) => {
    const nextVal = !current.canvas.reattachOnEmptyRelease
    addLog('Editor', `Edge release mode set to ${nextVal ? 'reattach back' : 'remove path (default)'}.`)
    return {
      ...current,
      canvas: {
        ...current.canvas,
        reattachOnEmptyRelease: nextVal,
      },
    }
  })
  const [widths, setWidths] = useState({
    leftPanel: DEFAULT_STUDIO_PROPERTIES.panels.leftPanel.defaultWidth,
    rightPanelBody: DEFAULT_STUDIO_PROPERTIES.panels.rightPanelBody.defaultWidth,
  })
  const [drawerHeight, setDrawerHeight] = useState(DEFAULT_STUDIO_PROPERTIES.drawer.defaultHeight)
  const [isLeftPanelVisible, setIsLeftPanelVisible] = useState(true)
  const [isRightPanelBodyVisible, setIsRightPanelBodyVisible] = useState(true)
  const [isDrawerVisible, setIsDrawerVisible] = useState(true)
  const [tools, setTools] = useState<RegisteredTool[]>([])
  const [toolModules, setToolModules] = useState<ToolModuleSummary[]>([])
  const [toolFunctions, setToolFunctions] = useState<McpToolFunction[]>([])
  const [pendingToolNode, setPendingToolNode] = useState<{ requestId: string; toolName: string } | null>(null)
  const [toolsState, setToolsState] = useState<'refreshing' | 'loading' | 'ready' | 'error'>('loading')
  const [isPanelSearchOpen, setIsPanelSearchOpen] = useState(false)
  const [workspace, setWorkspace] = useState<LocalWorkspace | null>(null)
  const [workspaceHistory, setWorkspaceHistory] = useState<Record<string, LocalWorkspace>>({})
  const [recents, setRecents] = useState<RecentWorkspaceEntry[]>(readRecentWorkspaceEntries)
  const [recentItemsWithoutHandle, setRecentItemsWithoutHandle] = useState<ReadonlySet<string>>(() => new Set())
  const [isWorkspaceDialogOpen, setIsWorkspaceDialogOpen] = useState(false)
  const presentations = useMemo(() => createToolPresentationIndex(toolFunctions, toolModules), [toolFunctions, toolModules])
  const bookmarkedTools = useMemo(() => {
    const favoriteToolIds = new Set(nodes
      .filter((node) => node.kind === 'tool' && node.isFavorite)
      .map((node) => `${node.toolId}.${node.functionName}`))
    return tools.filter((tool) => favoriteToolIds.has(tool.id))
  }, [nodes, tools])
  const selectedNode = nodes.find((node) => node.id === selectedNodeId)
  const nativeValidation = nativeValidationTransport()
  useEffect(() => {
    if (!running) return
    const timer = window.setInterval(() => setRuntimeTrace((current) => current ? { ...current } : current), 50)
    return () => window.clearInterval(timer)
  }, [running])
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        setIsPanelSearchOpen(true)
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [authenticationRevision])
  useEffect(() => {
    let cancelled = false
    void listMcpTools()
      .then(async (functions) => {
        if (cancelled) return
        setToolFunctions(functions)
        setTools(registeredTools(functions))
        setToolsState('ready')
        const modules = await listToolModules().catch(() => [])
        if (!cancelled) setToolModules(modules)
      })
      .catch(() => {
        if (!cancelled) setToolsState('error')
      })
    return () => { cancelled = true }
  }, [])
  useEffect(() => {
    let cancelled = false
    void Promise.all(recents.map(async (recent) => ({
      id: recent.id,
      hasHandle: recent.workspaceId ? await hasStoredDirectoryHandle(recent.workspaceId) : false,
    }))).then((availability) => {
      if (cancelled) return
      setRecentItemsWithoutHandle(new Set(availability.filter((recent) => !recent.hasHandle).map((recent) => recent.id)))
    })
    return () => { cancelled = true }
  }, [recents])
  const addLog = (source: string, message: string, severity?: LogEntry['severity']) => setLogs((current) => [{ time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }), source, message, severity }, ...current])
  const setMessage = (text: string) => addLog('Editor', text)

  const acceptGoogleCredential = (credential: string) => {
    try {
      const identity = readGoogleIdentity(credential)
      setStudioAccessToken(credential)
      setProfile({ displayName: identity.displayName, email: identity.email, avatarUrl: identity.avatarUrl, provider: 'google' })
      setAuthenticationRevision((current) => current + 1)
      addLog('Authentication', `Google identity connected for ${identity.displayName}.`)
    } catch (error) {
      addLog('Authentication', error instanceof Error ? error.message : 'Google sign-in could not be read.', 'error')
    }
  }

  const signOut = () => {
    setStudioAccessToken(null)
    setProfile(null)
    setAuthenticationRevision((current) => current + 1)
    addLog('Authentication', 'Signed out. The Google credential was removed from browser memory.')
  }

  const indexingAbortControllerRef = useRef<AbortController | null>(null)

  const triggerBackgroundIndexing = (ws: LocalWorkspace) => {
    indexingAbortControllerRef.current?.abort()
    const controller = new AbortController()
    indexingAbortControllerRef.current = controller
    void indexWorkspaceBackground(ws.root, (updatedRoot) => {
      setWorkspace((prev) => (prev?.id === ws.id ? { ...prev, root: updatedRoot } : prev))
      setWorkspaceHistory((current) => current[ws.id] ? { ...current, [ws.id]: { ...current[ws.id], root: updatedRoot } } : current)
    }, controller.signal)
  }

  const rememberRecent = (entry: RecentWorkspaceEntry) => {
    setRecents((current) => {
      const next = upsertRecentEntry(current, entry)
      writeRecentWorkspaceEntries(next)
      return next
    })
  }
  const activateWorkspace = (name: string, selection: DirectorySelection) => {
    const earliestMatchingRecent = recents.findLast((entry) => entry.kind === 'workspace' && entry.label === name && entry.path === selection.name)
    const workspaceId = earliestMatchingRecent?.workspaceId ?? crypto.randomUUID()
    const nextWorkspace: LocalWorkspace = { id: workspaceId, name, pathLabel: selection.name, root: selection.root }
    setWorkspace(nextWorkspace)
    setWorkspaceHistory((current) => ({ ...current, [nextWorkspace.id]: nextWorkspace }))
    if (!earliestMatchingRecent) {
      rememberRecent({ id: `workspace:${nextWorkspace.id}`, kind: 'workspace', label: name, path: selection.name, workspaceId: nextWorkspace.id })
    }
    if (selection.handle) {
      void storeDirectoryHandle(nextWorkspace.id, selection.handle).then(() => {
        setRecents((current) => [...current])
      })
    }
    setLeftPanel('workspace-explorer')
    setIsLeftPanelVisible(true)
    triggerBackgroundIndexing(nextWorkspace)
  }
  const openWorkspace = async () => {
    try {
      const selection = await chooseLocalDirectory()
      if (!selection) return
      activateWorkspace(selection.name, selection)
      setMessage(`Opened local workspace ${selection.name}.`)
    } catch {
      setMessage('Could not open the local workspace.')
    }
  }
  const createWorkspace = (name: string, selection: DirectorySelection) => {
    activateWorkspace(name, selection)
    setIsWorkspaceDialogOpen(false)
    setMessage(`Configured local workspace ${name}. No files were created.`)
  }
  const selectWorkspaceFile = (file: LocalWorkspaceNode) => {
    if (!workspace) return
    rememberRecent({ id: `file:${workspace.id}:${file.path}`, kind: 'file', label: file.name, path: file.path, workspaceId: workspace.id })
    setMessage(`${file.name} is selected in Explorer. Studio rendering is not enabled.`)
  }
  const restoreWorkspace = (id: string, name: string, selection: DirectorySelection) => {
    const restoredWorkspace: LocalWorkspace = { id, name, pathLabel: selection.name, root: selection.root }
    setWorkspace(restoredWorkspace)
    setWorkspaceHistory((current) => ({ ...current, [restoredWorkspace.id]: restoredWorkspace }))
    setLeftPanel('workspace-explorer')
    setIsLeftPanelVisible(true)
    triggerBackgroundIndexing(restoredWorkspace)
    return restoredWorkspace
  }
  const openRecent = async (recent?: RecentWorkspaceEntry) => {
    if (!recent) {
      setLeftPanel('workspace-explorer')
      setIsLeftPanelVisible(true)
      setMessage(recents.length ? 'Recent local workspaces and files are shown in Explorer.' : 'No local workspace history yet.')
      return
    }
    if (recent.kind === 'file') {
      setLeftPanel('workspace-explorer')
      setIsLeftPanelVisible(true)
      setMessage(`${recent.label} remains an Explorer-only file; Studio rendering is not enabled.`)
    }
    const rememberedWorkspace = recent.workspaceId ? workspaceHistory[recent.workspaceId] : undefined
    if (rememberedWorkspace) {
      setWorkspace(rememberedWorkspace)
      setLeftPanel('workspace-explorer')
      setIsLeftPanelVisible(true)
      setMessage(`Reopened ${rememberedWorkspace.name} from this browser session.`)
      return
    }
    if (!recent.workspaceId) {
      const selection = await chooseLocalDirectory()
      if (!selection) return
      activateWorkspace(recent.label, selection)
      setMessage(`Opened ${recent.label} from the folder picker.`)
      return
    }
    const stored = await readStoredDirectory(recent.workspaceId)
    const workspaceRecent = recents.find((entry) => entry.kind === 'workspace' && entry.workspaceId === recent.workspaceId)
    if (stored.selection) {
      const restoredWorkspace = restoreWorkspace(recent.workspaceId, workspaceRecent?.label ?? stored.selection.name, stored.selection)
      setMessage(`Reopened ${restoredWorkspace.name} from its saved local folder.`)
      return
    }
    const selection = await chooseStoredDirectory(recent.workspaceId)
    if (!selection) return
    activateWorkspace(workspaceRecent?.label ?? recent.label, selection)
    const source = stored.reason === 'permission-denied' ? 'the saved folder' : 'the folder picker'
    setMessage(`Opened ${workspaceRecent?.label ?? recent.label} from ${source}.`)
  }

  const addToolNode = useCallback((toolName: string, position: { x: number; y: number }) => {
    const tool = toolFunctions.find((candidate) => candidate.name === toolName)
    const separator = toolName.lastIndexOf('.')
    if (!tool || separator <= 0 || separator === toolName.length - 1) {
      setMessage(`Could not add ${toolName}: the registered tool is no longer available.`)
      return
    }
    const nodeId = nextNodeId(nodes)
    const functionName = toolName.slice(separator + 1)
    const toolId = toolName.slice(0, separator)
    const node: WorkflowNode = {
      id: nodeId,
      title: tool.title?.trim() || functionName,
      kind: 'tool',
      toolId,
      moduleToolId: tool.moduleToolId,
      functionName,
      output: outputName(functionName, nodes),
      x: position.x,
      y: position.y,
      arguments: {},
      annotations: { audit: false, timeoutMs: tool.annotations?.timeoutMs, scopes: tool.annotations?.scopes ?? [] },
    }
    setNodes((current) => [...current, node])
    setSelectedNodeId(nodeId)
    setMessage(`Added ${node.title}.`)
  }, [nodes, toolFunctions])

  const refreshTools = () => {
    setToolsState('refreshing')
    listMcpTools()
      .then(async (functions) => {
        setToolFunctions(functions)
        setTools(registeredTools(functions))
        setToolsState('ready')
        const modules = await listToolModules().catch(() => [])
        setToolModules(modules)
        setMessage('Refreshed tool tree and registered tools.')
      })
      .catch(() => {
        setToolsState('error')
        setMessage('Failed to refresh registered tools.')
      })
  }

  const copyOutputReference = async (output: string) => {
    const reference = `{{ ${output} }}`
    try {
      await navigator.clipboard.writeText(reference)
      setMessage(`Copied ${reference} as an input reference`)
    } catch {
      addLog('Editor', `Could not copy ${reference}`)
    }
  }

  const validate = () => {
    const names = new Set<string>()
    const errors = nodes.flatMap((node) => {
      if (!/^[A-Za-z_$][A-Za-z0-9_$]*$/.test(node.output)) return [`${node.title}: invalid output variable`]
      if (names.has(node.output)) return [`${node.title}: duplicate output variable`]
      names.add(node.output)
      return []
    })
    const unavailableNode = toolsState === 'ready' ? nodes.find((node) => node.kind === 'tool' && !toolFunctions.some((tool) => tool.name === `${node.toolId}.${node.functionName}`)) : undefined
    if (unavailableNode) {
      addLog('Validator', `${unavailableNode.id} ${unavailableNode.title}: Tool function is not available.`, 'error')
      return false
    }
    if (errors.length) {
      addLog('Validator', errors[0], 'error')
      return false
    }
    addLog('Validator', 'Workflow passed validation.')
    return true
  }

  const isSamePanel = (currentKind: PanelViewContentKind, targetKind: PanelViewContentKind) => {
    const currentPanel = getPanelForContentKind(currentKind)
    const targetPanel = getPanelForContentKind(targetKind)
    return currentPanel === targetPanel && currentPanel !== undefined
  }

  const requestLeftPanel = (content: LeftPanelViewContentKind, keepOpen: boolean = false) => {
    if (isLeftPanelVisible && isSamePanel(leftPanelView, content)) {
      if (keepOpen) {
        setLeftPanel(content)
        return
      }
      setIsLeftPanelVisible(false)
      setMessage('Left panel hidden.')
    } else {
      setLeftPanel(content)
      setIsLeftPanelVisible(true)
    }
  }
  const requestRightPanel = (content: RightPanelViewContentKind, keepOpen: boolean = false) => {
    if (isRightPanelBodyVisible && isSamePanel(rightPanelView, content)) {
      if (keepOpen) {
        setRightPanel(content)
        return
      }
      setIsRightPanelBodyVisible(false)
      setMessage('Workflow details panel hidden.')
    } else {
      setRightPanel(content)
      setIsRightPanelBodyVisible(true)
    }
  }
  const requestDrawerPanel = (content: DrawerPanelViewContentKind, keepOpen: boolean = false) => {
    if (isDrawerVisible && isSamePanel(drawerView, content)) {
      if (keepOpen) {
        setDrawer(content)
        return
      }
      setIsDrawerVisible(false)
      setMessage('Workflow drawer hidden.')
    } else {
      setDrawer(content)
      setIsDrawerVisible(true)
    }
  }
  const openPanel = (panel: StudioPanel, keepOpen: boolean = false, targetKind?: PanelViewContentKind) => {
    const kind = targetKind ?? getPanelContentKind(panel)
    if (panel.placement.surface === 'left') requestLeftPanel(kind as LeftPanelViewContentKind, keepOpen)
    else if (panel.placement.surface === 'right') requestRightPanel(kind as RightPanelViewContentKind, keepOpen)
    else requestDrawerPanel(kind as DrawerPanelViewContentKind, keepOpen)
  }
  const requestStudioPanel = (content: PanelViewContentKind, keepOpen: boolean = false) => {
    const panel = getPanelForContentKind(content) as StudioPanel | undefined
    if (!panel) {
      return
    }
    openPanel(panel, keepOpen, content)
  }

  const describeOutcome = (outcome: WorkflowNodeOutcome) => outcome.failed
    ? `Failed after ${outcome.attempts} attempt(s): ${outcome.message ?? 'unknown error'}`
    : `Completed through ${outcome.port} on attempt ${outcome.attempts}.`

  const finishRun = (result: WorkflowRunResult, includeOutcomeLogs: boolean) => {
    setLastRun(result)
    const completedAt = Date.now()
    setRuntimeTrace((current) => current && {
      ...current,
      completedAt,
      runId: result.runId ?? current.runId,
      entries: current.entries.map((entry) => {
        const outcome = result.nodeOutcomes?.find((candidate) => candidate.requestId === entry.requestId)
        const value = result.results?.[entry.variable]
        return outcome
          ? {
            ...entry,
            status: outcome.failed ? 'error' : 'success',
            attempts: outcome.attempts,
            port: outcome.port,
            message: outcome.message,
            ...(outcome.failed || value === undefined ? {} : valueDetails(value)),
          }
          : entry
      }),
    })
    const nextStates: Record<string, NodeRunState> = {}
    result.nodeOutcomes?.forEach((outcome) => {
      nextStates[outcome.requestId] = outcome.failed ? 'error' : 'success'
      if (includeOutcomeLogs) {
        const node = nodes.find((candidate) => candidate.id === outcome.requestId)
        addLog(node?.title ?? outcome.requestId, describeOutcome(outcome), outcome.failed ? 'error' : undefined)
      }
    })
    setRunStates(nextStates)
  }
  const run = async () => {
    if (running || !validate()) return
    const definition = workflowDefinition(nodes, edges)
    setRunning(true)
    setLogs([])
    setLastRun(null)
    requestStudioPanel('workflow-runtime', true)
    setRuntimeTrace(newRuntimeTrace(nodes))
    setRunStates(Object.fromEntries(nodes.map((node) => [node.id, 'idle'])))
    try {
      addLog('Runtime', 'Opening the live workflow channel to Meshingress.')
      try {
        const result = await runWorkflowLive(definition, {
          onRunStarted: (runId) => {
            setRuntimeTrace((current) => current ? { ...current, runId } : current)
            addLog('Runtime', `Live workflow run ${runId} started.`)
          },
          onNodeStarted: (requestId) => {
            const node = nodes.find((candidate) => candidate.id === requestId)
            const startedAt = Date.now()
            setRuntimeTrace((current) => current ? {
              ...current,
              entries: current.entries.map((entry) => entry.requestId === requestId
                ? { ...entry, status: 'running', startedAt, lane: firstOpenLane(current) }
                : entry),
            } : current)
            setRunStates((current) => ({ ...current, [requestId]: 'running' }))
            addLog(node?.title ?? requestId, 'Running.')
          },
          onNodeCompleted: (outcome) => {
            const node = nodes.find((candidate) => candidate.id === outcome.requestId)
            const completedAt = Date.now()
            setRuntimeTrace((current) => current ? {
              ...current,
              entries: current.entries.map((entry) => entry.requestId === outcome.requestId
                ? { ...entry, status: outcome.failed ? 'error' : 'success', completedAt, attempts: outcome.attempts, port: outcome.port, message: outcome.message }
                : entry),
            } : current)
            setRunStates((current) => ({ ...current, [outcome.requestId]: outcome.failed ? 'error' : 'success' }))
            addLog(node?.title ?? outcome.requestId, describeOutcome(outcome), outcome.failed ? 'error' : undefined)
          },
        })
        finishRun(result, false)
      } catch (error) {
        if (!(error instanceof WorkflowLiveTransportError) || !error.canFallbackToHttp) throw error
        addLog('Runtime', 'Live channel unavailable before the run started; using the HTTP fallback.')
        finishRun(await runWorkflowHttp(definition), true)
      }
    } catch (error) {
      setRunStates((current) => Object.fromEntries(nodes.map((node) => [node.id, current[node.id] === 'success' ? 'success' : 'error'])))
      const message = error instanceof Error ? error.message : 'Unknown runtime error'
      addLog('Runtime', message, 'error')
      setRuntimeTrace((current) => current ? {
        ...current,
        completedAt: Date.now(),
        entries: current.entries.map((entry) => entry.status === 'running' ? { ...entry, status: 'error', completedAt: Date.now(), message } : entry),
      } : current)
    } finally {
      setRunning(false)
    }
  }
  const resize = (panel: keyof typeof widths, event: React.PointerEvent<HTMLDivElement>) => {
    const target = event.currentTarget
    const start = event.clientX
    const original = widths[panel]
    const constraint = DEFAULT_STUDIO_PROPERTIES.panels[panel]
    target.setPointerCapture(event.pointerId)
    const move = (moveEvent: PointerEvent) => setWidths((current) => ({
      ...current,
      [panel]: Math.max(constraint.minWidth, Math.min(constraint.maxWidth, original + (panel === 'leftPanel' ? 1 : -1) * (moveEvent.clientX - start)))
    }))
    const end = () => {
      target.removeEventListener('pointermove', move)
      target.removeEventListener('pointerup', end)
      target.removeEventListener('pointercancel', end)
    }
    target.addEventListener('pointermove', move)
    target.addEventListener('pointerup', end)
    target.addEventListener('pointercancel', end)
  }
  const resizeDrawer = (event: React.PointerEvent<HTMLDivElement>) => {
    const target = event.currentTarget
    const center = target.parentElement
    if (!center) return
    const start = event.clientY
    const original = drawerHeight
    const centerHeight = center.getBoundingClientRect().height
    const { minHeight, maxHeight } = DEFAULT_STUDIO_PROPERTIES.drawer
    target.setPointerCapture(event.pointerId)
    const move = (moveEvent: PointerEvent) => setDrawerHeight(Math.max(minHeight, Math.min(maxHeight, original + ((start - moveEvent.clientY) / centerHeight) * 100)))
    const end = () => {
      target.removeEventListener('pointermove', move)
      target.removeEventListener('pointerup', end)
      target.removeEventListener('pointercancel', end)
    }
    target.addEventListener('pointermove', move)
    target.addEventListener('pointerup', end)
    target.addEventListener('pointercancel', end)
  }

  function studioPanelParams(): PanelViewSetContentParams {
    return {
      'tool-catalog': { tools, toolsState, presentations, onToolAdd: (toolName) => setPendingToolNode({ toolName, requestId: crypto.randomUUID() }), onRefreshTools: refreshTools },
      'workspace-explorer': { workspace, recents, recentItemsWithoutHandle, onFileSelect: selectWorkspaceFile, onOpenWorkspace: () => void openWorkspace(), onOpenRecent: (recent) => void openRecent(recent) },
      'workflow-files': {},
      'starred-tools': { bookmarkedTools, toolsState, presentations, onToolAdd: (toolName) => setPendingToolNode({ toolName, requestId: crypto.randomUUID() }) },
      'starred-workflows': {},
      'node-editor': { node: selectedNode!, toolFunctions, onNodeChange: (id, changes) => setNodes((current) => current.map((node) => node.id === id ? { ...node, ...changes } : node)) },
      'node-payload': { node: selectedNode! },
      'workflow-payload': { nodes, edges },
      'workflow-tools': { nodes, onSelectNode: setSelectedNodeId },
      'workflow-variables': { lastRun, nodes, onGetAs: (output) => void copyOutputReference(output), presentations },
      'workflow-activity': { logs },
      'workflow-runtime': { nodes, onClear: () => setRuntimeTrace(null), running, trace: runtimeTrace },
    }
  }

  function getStudioPanelSetParams(surface: 'left'): LeftPanelViewContentParams
  function getStudioPanelSetParams(surface: 'right'): RightPanelViewContentParams
  function getStudioPanelSetParams(surface: 'drawer'): DrawerPanelViewContentParams
  function getStudioPanelSetParams(surface: PanelSurface): LeftPanelViewContentParams | RightPanelViewContentParams | DrawerPanelViewContentParams {
    const allParams = studioPanelParams()
    const surfaceKinds = getSurfaceContentKinds(surface)
    const result = {} as Record<string, unknown>
    for (const kind of surfaceKinds) {
      if (kind in allParams) {
        result[kind] = allParams[kind]
      }
    }
    return result as any
  }


  return (
    <div className="studio-shell" style={{ '--left-panel-width': isLeftPanelVisible ? `${widths.leftPanel}px` : '0px', '--right-panel-body-width': isRightPanelBodyVisible ? `${widths.rightPanelBody}px` : '0px' } as CSSProperties}>
      <TopBar
        googleClientId={RuntimeConfiguration.current.googleClientId}
        onGoogleCredential={acceptGoogleCredential}
        onGoogleError={(message) => addLog('Authentication', message, 'error')}
        onNativeValidate={async (username, password) => {
          const result = await validateNativeLogin(username, password)
          addLog('Authentication', result.accepted ? 'Native login development validation succeeded.' : result.message, result.accepted ? undefined : 'error')
          return result
        }}
        nativeValidationAvailable={nativeValidation.available}
        nativeValidationNotice={nativeValidation.notice}
        onNewWorkspace={() => setIsWorkspaceDialogOpen(true)}
        onOpenRecent={() => openRecent()}
        onOpenWorkspace={() => void openWorkspace()}
        onRun={run}
        onSearchPanels={() => setIsPanelSearchOpen(true)}
        onStatus={setMessage}
        onSignOut={signOut}
        onValidate={validate}
        profile={profile}
        workflowName={workflowName}
      />
      <div className="layout">
        <ActivityRail
          drawer={drawerView}
          iconSize={studioProperties.railIconSize}
          isDrawerVisible={isDrawerVisible}
          isLeftPanelVisible={isLeftPanelVisible}
          onDrawerChange={requestStudioPanel}
          onViewChange={requestStudioPanel}
          showTitles={studioProperties.nav.rail.showTitles}
          showToolBadges={studioProperties.nav.rail.showToolBadges}
          showToolNames={studioProperties.nav.rail.showToolNames}
          view={leftPanelView}
        />

        {isLeftPanelVisible && (
          <StudioLeftPanel
            onChange={setLeftPanel}
            onHide={() => { setIsLeftPanelVisible(false); setMessage('Left panel hidden.') }}
            params={getStudioPanelSetParams('left')}
            view={leftPanelView}
          />
        )}
        {isLeftPanelVisible &&
          <div
            aria-label="Resize left panel"
            aria-orientation="vertical"
            className="panel-resizer"
            onPointerDown={(event) => resize('leftPanel', event)}
            role="separator"
          />
        }

        <main className={`center${isDrawerVisible ? '' : ' drawer-hidden'}`} style={{ '--drawer-height': `${drawerHeight}%` } as CSSProperties}>
          <Workbench
            edges={edges}
            nodes={nodes}
            onEdgeCreate={(sourceId, targetId) => setEdges((current) => [...current.filter((edge) => edge.source !== sourceId), { source: sourceId, target: targetId }])}
            onEdgeDelete={(edgeToDelete) => setEdges((current) => current.filter((edge) => !(edge.source === edgeToDelete.source && edge.target === edgeToDelete.target)))}
            onEdgeReconnect={(edge, targetId) => setEdges((current) => { const index = current.findIndex((candidate) => candidate.source === edge.source && candidate.target === edge.target); if (index < 0) return current; return current.map((candidate, candidateIndex) => candidateIndex === index ? { ...candidate, target: targetId } : candidate) })}
            onNodesChange={setNodes}
            onPendingToolNodeHandled={(requestId) => setPendingToolNode((current) => current?.requestId === requestId ? null : current)}
            onRun={run}
            onSelectNode={setSelectedNodeId}
            onStatus={setMessage}
            onToggleReattachOnEmptyRelease={toggleReattachOnEmptyRelease}
            onToolInsert={addToolNode}
            onValidate={validate}
            pendingToolNode={pendingToolNode}
            presentations={presentations}
            reattachOnEmptyRelease={studioProperties.canvas.reattachOnEmptyRelease}
            runStates={runStates}
            running={running}
            selectedNodeId={selectedNodeId}
            toolFunctions={toolFunctions}
            workflowFileName={workflowFileName}
          />

          {isDrawerVisible &&
            <div
              aria-label="Resize workflow drawer"
              aria-orientation="horizontal"
              aria-valuemax={DEFAULT_STUDIO_PROPERTIES.drawer.maxHeight}
              aria-valuemin={DEFAULT_STUDIO_PROPERTIES.drawer.minHeight}
              aria-valuenow={Math.round(drawerHeight)}
              className="pane-resizer"
              onPointerDown={resizeDrawer}
              role="separator"
            />
          }
          {isDrawerVisible && (
            <StudioDrawerPanel
              drawer={drawerView}
              onChange={setDrawer}
              onHide={() => { setIsDrawerVisible(false); setMessage('Workflow drawer hidden.') }}
              params={getStudioPanelSetParams('drawer')}
            />
          )}
        </main>

        {isRightPanelBodyVisible &&
          <div
            aria-label="Resize workflow details panel"
            aria-orientation="vertical"
            className="panel-resizer"
            onPointerDown={(event) => resize('rightPanelBody', event)}
            role="separator"
          />
        }
        {isRightPanelBodyVisible && (
          <StudioRightPanel
            onHide={() => { setIsRightPanelBodyVisible(false); setMessage('Workflow details panel hidden.') }}
            onNodeChange={(id, changes) => setNodes((current) => current.map((node) => node.id === id ? { ...node, ...changes } : node))}
            onRightPanelBodyChange={setRightPanel}
            params={getStudioPanelSetParams('right')}
            presentations={presentations}
            rightPanelBody={rightPanelView}
            selectedNode={selectedNode}
          />
        )}

        <InteractiveRail
          iconSize={studioProperties.railIconSize}
          isRightPanelBodyVisible={isRightPanelBodyVisible}
          onChange={requestStudioPanel}
          rightPanelBody={rightPanelView}
          showTitles={studioProperties.nav.rail.showTitles}
          showToolNames={studioProperties.nav.rail.showToolNames}
        />
      </div>
      <footer className="studio-footer">
        <div className="studio-footer-breadcrumbs">
          <span className="muted">Workflows › </span>{workflowName}
        </div>
      </footer>
      {isPanelSearchOpen && <PanelSearchDialog onClose={() => setIsPanelSearchOpen(false)} onSelect={(panel) => { openPanel(panel, true); setIsPanelSearchOpen(false) }} />}
      {isWorkspaceDialogOpen && <WorkspaceDialog onChooseDirectory={chooseLocalDirectory} onClose={() => setIsWorkspaceDialogOpen(false)} onCreate={createWorkspace} />}
    </div>
  )
}

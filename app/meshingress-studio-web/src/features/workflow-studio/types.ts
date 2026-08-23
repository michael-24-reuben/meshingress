export const GRID_SIZE = 20
export const NODE_STAGE_INSET = GRID_SIZE * 2
export const NODE_HEIGHT = GRID_SIZE * 6
export const NODE_WIDTH = GRID_SIZE * 11
export const CANVAS_SIZE = { width: 1500, height: 1000 }
export const TOOL_NODE_DRAG_TYPE = 'application/x-meshingress-tool-node'
export const RAIL_ICON_SIZE = 24

export interface PanelConstraint {
  defaultWidth: number
  minWidth: number
  maxWidth: number
}

export interface DrawerConstraint {
  defaultHeight: number
  minHeight: number
  maxHeight: number
  runtimeMinHeight: number
}

export interface CanvasProperties {
  gridSize: number
  nodeStageInset: number
  nodeHeight: number
  nodeWidth: number
  canvasSize: { width: number; height: number }
  /**
   * Runtime flag controlling edge reconnection behavior when released over empty space (without attached ports).
   * Default: false (remove that edge path on unattached release)
   * Alternative: true (reattach edge back to previous connection)
   */
  reattachOnEmptyRelease?: boolean
}

export interface RailNavProperties {
  showToolNames: boolean
  showTitles: boolean
  showToolBadges: boolean
}

export interface NavProperties {
  rail: RailNavProperties
}

export interface StudioRuntimeProperties {
  panels: {
    leftPanel: PanelConstraint
    rightPanelBody: PanelConstraint
  }
  drawer: DrawerConstraint
  canvas: CanvasProperties
  railIconSize: number
  nav: NavProperties
}

export const DEFAULT_STUDIO_PROPERTIES: StudioRuntimeProperties = {
  panels: {
    leftPanel: {
      defaultWidth: 255,
      minWidth: 250,
      maxWidth: 460,
    },
    rightPanelBody: {
      defaultWidth: 315,
      minWidth: 260,
      maxWidth: 520,
    },
  },
  drawer: {
    defaultHeight: 15,
    minHeight: 15,
    maxHeight: 75,
    runtimeMinHeight: 42,
  },
  canvas: {
    gridSize: GRID_SIZE,
    nodeStageInset: NODE_STAGE_INSET,
    nodeHeight: NODE_HEIGHT,
    nodeWidth: NODE_WIDTH,
    canvasSize: CANVAS_SIZE,
    reattachOnEmptyRelease: false,
  },
  railIconSize: RAIL_ICON_SIZE,
  nav: {
    rail: {
      showToolNames: true,
      showTitles: true,
      showToolBadges: true,
    },
  },
}

export type {
  SurfaceEntry,
  LeftPanelViewContentKind,
  DrawerPanelViewContentKind,
  RightPanelViewContentKind,
  LeftPanelViewContentParams,
  DrawerPanelViewContentParams,
  RightPanelViewContentParams,
} from './components/panel-catalog'

export type SortOption = 'name-asc' | 'name-desc' | 'namespace' | 'count-desc'
export type NodeRunState = 'idle' | 'running' | 'success' | 'error'
export type WorkflowArgumentValue = string | string[]
export type WorkflowNodeIconKind = 'workflow-manual-trigger' | 'meshingress-api' | 'tool'

export interface WorkflowNode {
  id: string
  title: string
  kind: 'trigger' | 'tool'
  /** Presentation is separate from the executable node kind so future native nodes do not become tool-ID heuristics. */
  iconKind?: WorkflowNodeIconKind
  toolId: string
  /** Opaque catalog module identity. It does not replace the callable toolId. */
  moduleToolId?: string
  functionName: string
  output: string
  x: number
  y: number
  arguments: Record<string, WorkflowArgumentValue>
  annotations: { audit: boolean; timeoutMs?: number; scopes: string[] }
  isFavorite?: boolean
}

export interface WorkflowEdge {
  /** One route; multiple edges may share a target input. */
  source: string
  target: string
}

export interface LogEntry {
  id?: string
  time: string
  source: string
  message: string
  severity?: 'error'
}

export const createLogEntryId = (source: string, time: string, uuid: string = crypto.randomUUID()): string =>
  `${uuid}-${time}-${source}`

export const createLogEntry = (
  source: string,
  message: string,
  severity?: LogEntry['severity'],
  time: string = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
  uuid: string = crypto.randomUUID()
): LogEntry => ({
  id: createLogEntryId(source, time, uuid),
  time,
  source,
  message,
  severity,
})

export interface RegisteredTool {
  id: string
  moduleToolId?: string
  title: string
  description?: string
}

export interface WorkflowRunResult {
  runId?: string
  status?: string
  /** Compiled workflow payload values used for bindings; tool envelopes live in nodeResults. */
  results?: Record<string, unknown>
  nodeResults?: WorkflowNodeResult[]
  failureMessage?: string
}

export interface WorkflowNodeOutcome {
  requestId: string
  failed: boolean
  attempts: number
  port: string
  message?: string
}

/** The authoritative persisted record for one workflow node execution. */
export interface WorkflowNodeResult {
  nodeId: string
  nodePath?: string | null
  variable: string
  outcome: WorkflowNodeOutcome
  /** Epoch milliseconds captured by the workflow server around execution, not by browser event receipt. */
  startedAt: number
  completedAt: number
  result?: unknown
  /** Generated from the concrete typed StructuredContent contract, never from result data. */
  outputSchema?: Record<string, unknown> | null
  diagnostics?: WorkflowNodeDiagnostic[]
}

/** Non-fatal execution observation supplied by the workflow server. */
export interface WorkflowNodeDiagnostic {
  type: string
  severity: string
  message: string
  details?: Record<string, unknown> | null
}

export interface WorkflowNodeStarted {
  requestId: string
  startedAt: number
  sequence?: number
}

export interface RuntimeTraceEntry {
  requestId: string
  variable: string
  initiator: string
  kind: WorkflowNode['kind']
  status: NodeRunState
  startedAt?: number
  completedAt?: number
  attempts?: number
  port?: string
  message?: string
  type?: string
  size?: number
}

export interface RuntimeTrace {
  runId?: string
  /** Server-time origin for every node span. The initial browser value is replaced on the first lifecycle event. */
  startedAt: number
  completedAt?: number
  /** Latest server clock value paired with the browser receipt time for live-duration display. */
  serverTimeAnchor?: { at: number; receivedAt: number }
  entries: RuntimeTraceEntry[]
}

export type ElementAttributesNormalized = Record<string, string | number | boolean>

export type ElementAttributeInput = Record<string, string[] | string | number | boolean>

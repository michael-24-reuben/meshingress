import { generateUuid } from '../../utils/uuid'

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

export interface WorkflowNodeCapabilities {
  acceptsInput: boolean
  acceptsOutput: boolean
}

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
  /** Connection affordances supplied by the node/tool contract, not renderer-local state. */
  capabilities: WorkflowNodeCapabilities
  isFavorite?: boolean
}

export interface WorkflowNodeValidationResult {
  valid: boolean
  errors: string[]
}

/**
 * Validates an input candidate against the WorkflowNode data policy.
 * Checks structural integrity, required fields, variable identifier formatting for `output`,
 * coordinate finiteness, argument shape, and annotation constraints.
 */
export function validateWorkflowNodeDataPolicy(node: unknown): WorkflowNodeValidationResult {
  const errors: string[] = []

  if (!node || typeof node !== 'object' || Array.isArray(node)) {
    return { valid: false, errors: ['WorkflowNode must be a non-null object.'] }
  }

  const candidate = node as Partial<WorkflowNode>

  if (typeof candidate.id !== 'string' || candidate.id.trim().length === 0) {
    errors.push('id must be a non-empty string.')
  }

  if (typeof candidate.title !== 'string' || candidate.title.trim().length === 0) {
    errors.push('title must be a non-empty string.')
  }

  if (candidate.kind !== 'trigger' && candidate.kind !== 'tool') {
    errors.push(`kind must be either 'trigger' or 'tool' (received: ${String(candidate.kind)}).`)
  }

  if (candidate.iconKind !== undefined && candidate.iconKind !== 'workflow-manual-trigger' && candidate.iconKind !== 'meshingress-api' && candidate.iconKind !== 'tool') {
    errors.push("iconKind must be 'workflow-manual-trigger', 'meshingress-api', 'tool', or undefined.")
  }

  if (typeof candidate.toolId !== 'string') {
    errors.push('toolId must be a string.')
  }

  if (candidate.moduleToolId !== undefined && typeof candidate.moduleToolId !== 'string') {
    errors.push('moduleToolId must be a string if provided.')
  }

  if (typeof candidate.functionName !== 'string') {
    errors.push('functionName must be a string.')
  }

  if (typeof candidate.output !== 'string' || !/^[A-Za-z_$][A-Za-z0-9_$]*$/.test(candidate.output)) {
    errors.push(`output must be a valid variable identifier matching /^[A-Za-z_$][A-Za-z0-9_$]*$/ (received: "${String(candidate.output)}").`)
  }

  if (typeof candidate.x !== 'number' || !Number.isFinite(candidate.x)) {
    errors.push('x coordinate must be a finite number.')
  }

  if (typeof candidate.y !== 'number' || !Number.isFinite(candidate.y)) {
    errors.push('y coordinate must be a finite number.')
  }

  if (!candidate.arguments || typeof candidate.arguments !== 'object' || Array.isArray(candidate.arguments)) {
    errors.push('arguments must be a Record<string, string | string[]>.')
  } else {
    for (const [argKey, argValue] of Object.entries(candidate.arguments)) {
      if (typeof argValue === 'string') {
        continue
      }
      if (Array.isArray(argValue) && argValue.every((item) => typeof item === 'string')) {
        continue
      }
      errors.push(`arguments["${argKey}"] must be a string or array of strings.`)
    }
  }

  if (!candidate.annotations || typeof candidate.annotations !== 'object' || Array.isArray(candidate.annotations)) {
    errors.push('annotations must be an object.')
  } else {
    if (typeof candidate.annotations.audit !== 'boolean') {
      errors.push('annotations.audit must be a boolean.')
    }
    if (candidate.annotations.timeoutMs !== undefined && (typeof candidate.annotations.timeoutMs !== 'number' || !Number.isFinite(candidate.annotations.timeoutMs) || candidate.annotations.timeoutMs < 0)) {
      errors.push('annotations.timeoutMs must be a positive finite number if provided.')
    }
    if (!Array.isArray(candidate.annotations.scopes) || !candidate.annotations.scopes.every((scope) => typeof scope === 'string')) {
      errors.push('annotations.scopes must be an array of string scopes.')
    }
  }

  if (!candidate.capabilities || typeof candidate.capabilities !== 'object' || Array.isArray(candidate.capabilities)) {
    errors.push('capabilities must be an object.')
  } else {
    if (typeof candidate.capabilities.acceptsInput !== 'boolean') {
      errors.push('capabilities.acceptsInput must be a boolean.')
    }
    if (typeof candidate.capabilities.acceptsOutput !== 'boolean') {
      errors.push('capabilities.acceptsOutput must be a boolean.')
    }
  }

  if (candidate.isFavorite !== undefined && typeof candidate.isFavorite !== 'boolean') {
    errors.push('isFavorite must be a boolean if provided.')
  }

  return {
    valid: errors.length === 0,
    errors,
  }
}

/**
 * Type guard to check if an unknown candidate satisfies the WorkflowNode data policy.
 */
export function isWorkflowNode(node: unknown): node is WorkflowNode {
  return validateWorkflowNodeDataPolicy(node).valid
}

/**
 * Asserts that an unknown value satisfies the WorkflowNode data policy, throwing an error if invalid.
 */
export function assertWorkflowNodeDataPolicy(node: unknown): asserts node is WorkflowNode {
  const result = validateWorkflowNodeDataPolicy(node)
  if (!result.valid) {
    throw new Error(`WorkflowNode data policy violation: ${result.errors.join('; ')}`)
  }
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

export const createLogEntryId = (source: string, time: string, uuid: string = generateUuid()): string =>
  `${uuid}-${time}-${source}`

export const createLogEntry = (
  source: string,
  message: string,
  severity?: LogEntry['severity'],
  time: string = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
  uuid: string = generateUuid()
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

import { ensureDirectoryPermission, type FileSystemDirectoryHandleLike } from './local-workspace'
import {
  jsonToSchema,
  inferObservedOutputSchema,
  stableJson,
  type JsonSchema,
} from '../cache/schema-inference'
import {
  CacheType,
  safeToolPath,
  getCacheDirectoryPath,
  schemaFingerprint,
  cacheToolOutputSchema,
  cache,
  resolveDirectory,
  type CachedToolInfo,
  type ToolOutputSchemaCacheEntry,
  type ToolOutputSchemaCacheWrite,
  type CacheSchemaParams,
} from '../cache/tool-output-schema-cache'

export {
  CacheType,
  jsonToSchema,
  inferObservedOutputSchema,
  stableJson,
  type JsonSchema,
  type CachedToolInfo,
  type ToolOutputSchemaCacheEntry,
  type ToolOutputSchemaCacheWrite,
  type CacheSchemaParams,
}

export type ToolResultStoreWriteStatus = 'stored' | 'unavailable' | 'error'

export interface ToolResultStoreOptions {
  workspaceRoot?: FileSystemDirectoryHandleLike
  workflowName?: string
  dateTime?: string | Date
}

export interface StoreToolResultParams {
  workflowName?: string
  toolId?: string
  nodeId?: string
  toolPath?: string
  nodePath?: string
  contents: unknown
  dateTime?: string | Date
  workspaceRoot?: FileSystemDirectoryHandleLike
}

export interface StoredToolResultRecord {
  workflowName: string
  workflowFileName: string
  dateTime: string
  nodeId: string
  nodePath: string
  fileName: string
  relativePath: string
  fullPath: string
  contents: unknown
  storedAt: string
  status: ToolResultStoreWriteStatus
  error?: string
}

/**
 * Format a Date or timestamp string into the compact ISO run timestamp format `YYYYMMDDTHHmmss`.
 * Example: `20260821T185605`
 */
export function formatRunTimestamp(date: Date | string = new Date()): string {
  if (typeof date === 'string') {
    if (/^\d{8}T\d{6}$/.test(date)) {
      return date
    }
    const parsed = new Date(date)
    if (!isNaN(parsed.getTime())) {
      date = parsed
    } else {
      const cleaned = date.replace(/[^0-9T]/g, '')
      if (cleaned.length >= 15) return cleaned.slice(0, 15)
      date = new Date()
    }
  }
  const pad = (n: number) => n.toString().padStart(2, '0')
  const year = date.getFullYear()
  const month = pad(date.getMonth() + 1)
  const day = pad(date.getDate())
  const hours = pad(date.getHours())
  const minutes = pad(date.getMinutes())
  const seconds = pad(date.getSeconds())
  return `${year}${month}${day}T${hours}${minutes}${seconds}`
}

/**
 * Sanitize or normalize a workflow name to a file-system directory name,
 * stripping `.json` / `.yaml` extensions and normalizing unsafe characters.
 */
export function safeWorkflowFileName(name: string): string {
  if (!name || !name.trim()) return 'workflow'
  const stripped = name.trim().replace(/\.(json|ya?ml)$/i, '')
  if (/^[a-zA-Z0-9._-]+$/.test(stripped)) {
    return stripped
  }
  return stripped
    .toLowerCase()
    .replace(/[^a-z0-9._-]+/g, '-')
    .replace(/^-+|-+$/g, '') || 'workflow'
}

/**
 * Handles storing tool execution results under `var/runs/` and managing run execution history.
 *
 * Runs Output Path:
 * `var/runs/<workflow_file_name>/<date_time>/<nodeId>_<nodePath>.out.json`
 * Sample: `var/runs/desktop-volume-greeting-solo-leveling/20260821T185605/r-002_powershell.cli.execute.out.json`
 */
export class ToolResultStore {
  private workspaceRoot?: FileSystemDirectoryHandleLike
  private workflowName: string
  private dateTime: string
  private readonly history: StoredToolResultRecord[] = []

  constructor(options?: ToolResultStoreOptions | string, dateTime?: string | Date, workspaceRoot?: FileSystemDirectoryHandleLike) {
    if (typeof options === 'string') {
      this.workflowName = options
      this.dateTime = formatRunTimestamp(dateTime)
      this.workspaceRoot = workspaceRoot
    } else {
      this.workflowName = options?.workflowName ?? 'workflow'
      this.dateTime = formatRunTimestamp(options?.dateTime)
      this.workspaceRoot = options?.workspaceRoot
    }
  }

  // =========================================================================
  // Static Run Helpers
  // =========================================================================

  static formatDateTime(date?: Date | string): string {
    return formatRunTimestamp(date)
  }

  static safeWorkflowFileName(workflowName: string): string {
    return safeWorkflowFileName(workflowName)
  }

  static buildFileName(nodeId: string, nodePath: string): string {
    const safeNodeId = nodeId.trim().replace(/[^a-zA-Z0-9._-]+/g, '_') || 'node'
    const safeNodePath = nodePath.trim().replace(/[^a-zA-Z0-9._-]+/g, '_') || 'tool'
    return `${safeNodeId}_${safeNodePath}.out.json`
  }

  static buildPath(workflowName: string, dateTime: string | Date, nodeId: string, nodePath: string): string {
    const workflowFolder = safeWorkflowFileName(workflowName)
    const formattedDate = formatRunTimestamp(dateTime)
    const fileName = ToolResultStore.buildFileName(nodeId, nodePath)
    return `var/runs/${workflowFolder}/${formattedDate}/${fileName}`
  }

  static async store(params: {
    workspaceRoot?: FileSystemDirectoryHandleLike
    workflowName: string
    dateTime?: string | Date
    toolId: string
    toolPath: string
    contents: unknown
  }): Promise<StoredToolResultRecord> {
    const store = new ToolResultStore({
      workspaceRoot: params.workspaceRoot,
      workflowName: params.workflowName,
      dateTime: params.dateTime,
    })
    return store.store(params.toolId, params.toolPath, params.contents)
  }

  // =========================================================================
  // Static Cache & Schema Mechanics (Delegated for Backwards Compatibility)
  // =========================================================================

  static jsonToSchema(value: unknown): JsonSchema {
    return jsonToSchema(value)
  }

  static stableJson(value: unknown): string {
    return stableJson(value)
  }

  static async schemaFingerprint(schema: JsonSchema): Promise<string> {
    return schemaFingerprint(schema)
  }

  static safeToolPath(toolName: string): string {
    return safeToolPath(toolName)
  }

  static getCacheDirectoryPath(cacheType: CacheType): string {
    return getCacheDirectoryPath(cacheType)
  }

  static async cacheToolOutputSchema(
    workspaceRoot: FileSystemDirectoryHandleLike | undefined,
    tool: CachedToolInfo | { name: string; moduleToolId?: string; outputSchema?: JsonSchema },
    schema?: JsonSchema
  ): Promise<ToolOutputSchemaCacheWrite> {
    return cacheToolOutputSchema(workspaceRoot, tool, schema)
  }

  static async cache(cacheType: typeof CacheType.TOOL_OUTPUT_SCHEMA, params: CacheSchemaParams): Promise<ToolOutputSchemaCacheWrite>
  static async cache(cacheType: CacheType, params: CacheSchemaParams): Promise<ToolOutputSchemaCacheWrite> {
    return cache(cacheType, params)
  }

  // =========================================================================
  // Instance Run Operations
  // =========================================================================

  setWorkspaceRoot(root?: FileSystemDirectoryHandleLike): this {
    this.workspaceRoot = root
    return this
  }

  setWorkflowName(name: string): this {
    this.workflowName = name
    return this
  }

  setDateTime(dateOrTimestamp: string | Date): this {
    this.dateTime = formatRunTimestamp(dateOrTimestamp)
    return this
  }

  resetRun(dateOrTimestamp?: string | Date): this {
    this.dateTime = formatRunTimestamp(dateOrTimestamp)
    return this
  }

  getRunDateTime(): string {
    return this.dateTime
  }

  getWorkflowName(): string {
    return this.workflowName
  }

  getWorkflowFileName(): string {
    return safeWorkflowFileName(this.workflowName)
  }

  getRunDirectoryPath(): string {
    return `var/runs/${this.getWorkflowFileName()}/${this.dateTime}`
  }

  buildFileName(nodeId: string, nodePath: string): string {
    return ToolResultStore.buildFileName(nodeId, nodePath)
  }

  buildPath(nodeId: string, nodePath: string): string {
    return ToolResultStore.buildPath(this.workflowName, this.dateTime, nodeId, nodePath)
  }

  /**
   * Stores tool result contents for the given (toolId, toolPath) / (nodeId, nodePath).
   * Appends the result to the instance run history and persists to the workspace filesystem if available.
   */
  async store(toolId: string, toolPath: string, contents: unknown): Promise<StoredToolResultRecord> {
    return this.storeResult({ toolId, toolPath, contents })
  }

  /**
   * Alias for store() to append a tool result.
   */
  async append(toolId: string, toolPath: string, contents: unknown): Promise<StoredToolResultRecord> {
    return this.store(toolId, toolPath, contents)
  }

  /**
   * Flexible method to store a tool result with either positional or object arguments.
   */
  async storeResult(
    paramOrWorkflowName: string | StoreToolResultParams,
    toolIdParam?: string,
    toolPathParam?: string,
    contentsParam?: unknown
  ): Promise<StoredToolResultRecord> {
    let workflowName = this.workflowName
    let toolId = ''
    let toolPath = ''
    let contents: unknown
    let dateTime = this.dateTime
    let workspaceRoot = this.workspaceRoot

    if (typeof paramOrWorkflowName === 'object' && paramOrWorkflowName !== null) {
      workflowName = paramOrWorkflowName.workflowName ?? this.workflowName
      toolId = paramOrWorkflowName.toolId ?? paramOrWorkflowName.nodeId ?? ''
      toolPath = paramOrWorkflowName.toolPath ?? paramOrWorkflowName.nodePath ?? ''
      contents = paramOrWorkflowName.contents
      dateTime = paramOrWorkflowName.dateTime ? formatRunTimestamp(paramOrWorkflowName.dateTime) : this.dateTime
      workspaceRoot = paramOrWorkflowName.workspaceRoot ?? this.workspaceRoot
    } else if (typeof paramOrWorkflowName === 'string') {
      if (contentsParam !== undefined) {
        workflowName = paramOrWorkflowName
        toolId = toolIdParam ?? ''
        toolPath = toolPathParam ?? ''
        contents = contentsParam
      } else {
        toolId = paramOrWorkflowName
        toolPath = toolIdParam ?? ''
        contents = toolPathParam
      }
    }

    const workflowFileName = safeWorkflowFileName(workflowName)
    const nodeId = toolId
    const nodePath = toolPath
    const fileName = ToolResultStore.buildFileName(nodeId, nodePath)
    const relativePath = `var/runs/${workflowFileName}/${dateTime}/${fileName}`
    const fullPath = relativePath

    const record: StoredToolResultRecord = {
      workflowName,
      workflowFileName,
      dateTime,
      nodeId,
      nodePath,
      fileName,
      relativePath,
      fullPath,
      contents,
      storedAt: new Date().toISOString(),
      status: 'unavailable',
    }

    if (!workspaceRoot?.getDirectoryHandle || !workspaceRoot.getFileHandle) {
      record.status = 'unavailable'
      record.error = 'Workspace directory handle is not available for writing'
      this.history.push(record)
      return record
    }

    try {
      const hasPermission = await ensureDirectoryPermission(workspaceRoot, 'readwrite')
      if (!hasPermission) {
        record.status = 'unavailable'
        record.error = 'Permission denied to write to workspace root'
        this.history.push(record)
        return record
      }

      const dirPath = `var/runs/${workflowFileName}/${dateTime}`
      const directory = await resolveDirectory(workspaceRoot, dirPath)
      const file = await directory.getFileHandle!(fileName, { create: true })

      if (!file.createWritable) {
        record.status = 'unavailable'
        record.error = 'File handle does not support createWritable'
        this.history.push(record)
        return record
      }

      const writable = await file.createWritable()
      const jsonContent = formatJsonOutput(contents)
      await writable.write(jsonContent)
      await writable.close()

      record.status = 'stored'
    } catch (error) {
      record.status = 'error'
      record.error = error instanceof Error ? error.message : 'Unknown storage error'
    }

    this.history.push(record)
    return record
  }

  getHistory(): readonly StoredToolResultRecord[] {
    return this.history
  }

  clearHistory(): void {
    this.history.length = 0
  }
}

export const WorkflowToolResultStore = ToolResultStore

function formatJsonOutput(contents: unknown): string {
  if (typeof contents === 'string') {
    try {
      const parsed = JSON.parse(contents)
      return `${JSON.stringify(parsed, null, 2)}\n`
    } catch {
      return contents.endsWith('\n') ? contents : `${contents}\n`
    }
  }
  return `${JSON.stringify(contents ?? null, null, 2)}\n`
}

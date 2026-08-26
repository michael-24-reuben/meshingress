import { ensureDirectoryPermission, type FileSystemDirectoryHandleLike } from '../storage/local-workspace'
import {
  isRecord,
  jsonToSchema,
  inferObservedOutputSchema,
  stableJson,
  type JsonSchema,
} from './schema-inference'

export {
  jsonToSchema,
  inferObservedOutputSchema,
  stableJson,
  type JsonSchema,
}

export const CacheType = {
  TOOL_OUTPUT_SCHEMA: 'TOOL_OUTPUT_SCHEMA',
} as const

export type CacheType = typeof CacheType[keyof typeof CacheType]

export interface CachedToolInfo {
  name: string
  title?: string
  description?: string
  moduleToolId?: string
  inputSchema?: JsonSchema
  outputSchema?: JsonSchema
  uiSchema?: unknown // UI schema is not used, nor defined currently in the tool model, but in the future it may be used
  annotations?: {
    scopes?: unknown[]
    availability?: unknown
  }
}

/** A timestamped local observation, not a stable tool output contract. */
export interface ToolOutputSchemaCacheEntry {
  schemaVersion: 1
  tool: CachedToolInfo
  schemaFingerprint: string
  capturedAt: string
}

export type ToolOutputSchemaCacheWrite = 'stored' | 'already-exists' | 'unavailable'

export interface CacheSchemaParams {
  workspaceRoot: FileSystemDirectoryHandleLike | undefined
  tool: CachedToolInfo
  schema?: JsonSchema
}

const CACHE_ROOT_TOOLS = 'var/cache/tools'
const TOOL_OBSERVATIONS_DIRECTORY = 'observations'
const TOOL_OBSERVATION_FILE_SUFFIX = '.json'

export function formatSchemaTimestamp(date: Date): string {
  return date.toISOString().replace(/[-:.]/g, '')
}

export function safeToolPath(toolName: string): string {
  return toolName.trim().replace(/[^a-zA-Z0-9._-]+/g, '_').replace(/^[_.]+|[_.]+$/g, '') || 'tool'
}

export function getCacheDirectoryPath(cacheType: CacheType): string {
  switch (cacheType) {
    case CacheType.TOOL_OUTPUT_SCHEMA:
      return CACHE_ROOT_TOOLS
    default:
      return 'var/cache'
  }
}

export async function schemaFingerprint(schema: JsonSchema): Promise<string> {
  const content = new TextEncoder().encode(stableJson(schema))
  if (globalThis.crypto?.subtle?.digest) {
    try {
      const digest = await globalThis.crypto.subtle.digest('SHA-256', content)
      return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, '0')).join('')
    } catch {
      // Fallback if subtle digest fails
    }
  }
  // Deterministic fallback hash for non-secure HTTP contexts (e.g. --host on LAN)
  let h1 = 0xdeadbeef ^ 0, h2 = 0x41c6ce57 ^ 0
  const str = stableJson(schema)
  for (let i = 0; i < str.length; i++) {
    const ch = str.charCodeAt(i)
    h1 = Math.imul(h1 ^ ch, 2654435761)
    h2 = Math.imul(h2 ^ ch, 1597334677)
  }
  h1 = Math.imul(h1 ^ (h1 >>> 16), 2246822507) ^ Math.imul(h2 ^ (h2 >>> 13), 3266489909)
  h2 = Math.imul(h2 ^ (h2 >>> 16), 2246822507) ^ Math.imul(h1 ^ (h1 >>> 13), 3266489909)
  return (4294967296 * (2097151 & h2) + (h1 >>> 0)).toString(16).padStart(16, '0')
}

export async function resolveDirectory(root: FileSystemDirectoryHandleLike, relativePath: string): Promise<FileSystemDirectoryHandleLike> {
  let current = root
  for (const segment of relativePath.split('/').filter(Boolean)) {
    if (!current.getDirectoryHandle) {
      throw new Error('Directory handle does not support getDirectoryHandle')
    }
    current = await current.getDirectoryHandle(segment, { create: true })
  }
  return current
}

export async function readLatestObservedOutputSchema(directory: FileSystemDirectoryHandleLike): Promise<JsonSchema | null> {
  const snapshots: Array<{ name: string; getFile?: () => Promise<{ text: () => Promise<string> }> }> = []
  for await (const handle of directory.values()) {
    if (handle.kind === 'file' && handle.name.endsWith(TOOL_OBSERVATION_FILE_SUFFIX)) snapshots.push(handle)
  }
  const latest = snapshots.toSorted((left, right) => right.name.localeCompare(left.name))[0]
  if (!latest?.getFile) return null

  try {
    const parsed: unknown = JSON.parse(await (await latest.getFile()).text())
    const observedSchema = isRecord(parsed) && isRecord(parsed.tool) ? parsed.tool.outputSchema : null
    return isRecord(observedSchema) ? observedSchema : null
  } catch {
    return null
  }
}

/**
 * Persists a changed tool output observation as an immutable, chronologically named snapshot.
 * The latest snapshot is compared before writing so repeated identical observations are not stored.
 */
export async function cacheToolOutputSchema(
  workspaceRoot: FileSystemDirectoryHandleLike | undefined,
  tool: CachedToolInfo | { name: string; moduleToolId?: string; outputSchema?: JsonSchema },
  schema?: JsonSchema
): Promise<ToolOutputSchemaCacheWrite> {
  if (!workspaceRoot?.getDirectoryHandle || !workspaceRoot.getFileHandle) return 'unavailable'
  if (!await ensureDirectoryPermission(workspaceRoot, 'readwrite')) return 'unavailable'

  const outputSchema = schema ?? tool.outputSchema ?? {}
  const cacheDirPath = `${getCacheDirectoryPath(CacheType.TOOL_OUTPUT_SCHEMA)}/${safeToolPath(tool.name)}/${TOOL_OBSERVATIONS_DIRECTORY}`
  const directory = await resolveDirectory(workspaceRoot, cacheDirPath)
  const latestSchema = await readLatestObservedOutputSchema(directory)
  if (latestSchema && stableJson(latestSchema) === stableJson(outputSchema)) {
    return 'already-exists'
  }

  const capturedAt = formatSchemaTimestamp(new Date())
  const fingerprint = await schemaFingerprint(outputSchema)
  const fileName = `${capturedAt}--sha256-${fingerprint}${TOOL_OBSERVATION_FILE_SUFFIX}`
  const entry: ToolOutputSchemaCacheEntry = {
    schemaVersion: 1,
    tool: {
      ...tool,
      outputSchema,
    },
    schemaFingerprint: fingerprint,
    capturedAt: new Date().toISOString(),
  }
  const file = await directory.getFileHandle!(fileName, { create: true })
  if (!file.createWritable) return 'unavailable'
  const writable = await file.createWritable()
  await writable.write(`${JSON.stringify(entry, null, 2)}\n`)
  await writable.close()
  return 'stored'
}

/**
 * Generic static cache dispatcher parameterized by CacheType.
 */
export async function cache(cacheType: typeof CacheType.TOOL_OUTPUT_SCHEMA, params: CacheSchemaParams): Promise<ToolOutputSchemaCacheWrite>
export async function cache(cacheType: CacheType, params: CacheSchemaParams): Promise<ToolOutputSchemaCacheWrite> {
  switch (cacheType) {
    case CacheType.TOOL_OUTPUT_SCHEMA:
      return cacheToolOutputSchema(
        params.workspaceRoot,
        params.tool,
        params.schema
      )
    default:
      return 'unavailable'
  }
}

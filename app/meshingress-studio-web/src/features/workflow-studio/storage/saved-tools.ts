import type { FileSystemDirectoryHandleLike } from './local-workspace'

export const SAVED_TOOLS_FILE_PATH = '.meshingress/saved-tools.json'

export type SavedToolReference = {
  toolId: string
  moduleToolId?: string
}

type SavedToolsDocument = {
  version: 2
  tools: SavedToolReference[]
}

function isMissingFile(error: unknown): boolean {
  return error instanceof DOMException && error.name === 'NotFoundError'
}

export function savedToolReferenceKey({ moduleToolId, toolId }: SavedToolReference): string {
  return `${moduleToolId ?? ''}\u0000${toolId}`
}

export function sameSavedTool(left: SavedToolReference, right: SavedToolReference): boolean {
  return savedToolReferenceKey(left) === savedToolReferenceKey(right)
}

export function matchesSavedTool(reference: SavedToolReference, candidate: SavedToolReference): boolean {
  return reference.toolId === candidate.toolId
    && (reference.moduleToolId === undefined || candidate.moduleToolId === undefined || reference.moduleToolId === candidate.moduleToolId)
}

function normalizedReference(value: unknown): SavedToolReference | null {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return null
  const reference = value as Partial<SavedToolReference>
  if (typeof reference.toolId !== 'string' || !reference.toolId.trim()) return null
  if (reference.moduleToolId !== undefined && (typeof reference.moduleToolId !== 'string' || !reference.moduleToolId.trim())) return null
  return {
    toolId: reference.toolId.trim(),
    ...(reference.moduleToolId?.trim() ? { moduleToolId: reference.moduleToolId.trim() } : {}),
  }
}

function savedToolsDocument(tools: Iterable<SavedToolReference>): SavedToolsDocument {
  const unique = new Map<string, SavedToolReference>()
  for (const tool of tools) {
    const reference = normalizedReference(tool)
    if (reference) unique.set(savedToolReferenceKey(reference), reference)
  }
  return {
    version: 2,
    tools: [...unique.values()].toSorted((left, right) => savedToolReferenceKey(left).localeCompare(savedToolReferenceKey(right))),
  }
}

function parseSavedToolsDocument(value: unknown): SavedToolReference[] {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`${SAVED_TOOLS_FILE_PATH} must contain an object.`)
  }
  const document = value as { version?: unknown; tools?: unknown; toolIds?: unknown }
  if (document.version === 1 && Array.isArray(document.toolIds) && document.toolIds.every((toolId) => typeof toolId === 'string')) {
    return savedToolsDocument(document.toolIds.map((toolId) => ({ toolId }))).tools
  }
  if (document.version !== 2 || !Array.isArray(document.tools)) {
    throw new Error(`${SAVED_TOOLS_FILE_PATH} must contain version 2 and a tools array.`)
  }
  const tools = document.tools.map(normalizedReference)
  if (tools.some((tool) => tool === null)) {
    throw new Error(`${SAVED_TOOLS_FILE_PATH} tools must each contain a toolId and optional moduleToolId.`)
  }
  return savedToolsDocument(tools.filter((tool): tool is SavedToolReference => tool !== null)).tools
}

/** Reads only tool identity; registered tool metadata remains server-catalog data. */
export async function readSavedTools(workspaceRoot: FileSystemDirectoryHandleLike): Promise<SavedToolReference[]> {
  if (!workspaceRoot.getDirectoryHandle) {
    throw new Error('This browser cannot read workspace-local Saved tools state.')
  }

  try {
    const stateDirectory = await workspaceRoot.getDirectoryHandle('.meshingress')
    const file = await stateDirectory.getFileHandle?.('saved-tools.json')
    const contents = await file?.getFile?.().then((savedFile) => savedFile.text())
    if (contents === undefined) {
      throw new Error('This browser cannot read workspace-local Saved tools state.')
    }
    return parseSavedToolsDocument(JSON.parse(contents))
  } catch (error) {
    if (isMissingFile(error)) return []
    throw error
  }
}

export async function writeSavedTools(workspaceRoot: FileSystemDirectoryHandleLike, tools: Iterable<SavedToolReference>): Promise<void> {
  if (!workspaceRoot.getDirectoryHandle) {
    throw new Error('This browser cannot write workspace-local Saved tools state.')
  }

  const stateDirectory = await workspaceRoot.getDirectoryHandle('.meshingress', { create: true })
  const file = await stateDirectory.getFileHandle?.('saved-tools.json', { create: true })
  if (!file?.createWritable) {
    throw new Error('This browser cannot write workspace-local Saved tools state.')
  }
  const writable = await file.createWritable()
  await writable.write(`${JSON.stringify(savedToolsDocument(tools), null, 2)}\n`)
  await writable.close()
}

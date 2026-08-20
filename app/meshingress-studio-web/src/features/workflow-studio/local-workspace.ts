export type LocalWorkspaceNode = {
  name: string
  path: string
  kind: 'directory' | 'file'
  children?: LocalWorkspaceNode[]
  handle?: FileSystemDirectoryHandleLike
  isIndexed?: boolean
}

export type LocalWorkspace = {
  id: string
  name: string
  pathLabel: string
  root: LocalWorkspaceNode
}

export type RecentWorkspaceEntry = {
  id: string
  kind: 'workspace' | 'file'
  label: string
  path: string
  workspaceId?: string
}

export type DirectorySelection = {
  name: string
  root: LocalWorkspaceNode
  handle?: FileSystemDirectoryHandleLike
}

export interface FileSystemDirectoryHandleLike {
  kind: 'directory'
  name: string
  values(): AsyncIterable<FileSystemHandleLike>
  queryPermission?: (descriptor?: { mode?: 'read' | 'readwrite' }) => Promise<PermissionState>
  requestPermission?: (descriptor?: { mode?: 'read' | 'readwrite' }) => Promise<PermissionState>
}

interface FileSystemFileHandleLike {
  kind: 'file'
  name: string
}

type FileSystemHandleLike = FileSystemDirectoryHandleLike | FileSystemFileHandleLike

type DirectoryPickerWindow = Window & {
  showDirectoryPicker?: (options?: { startIn?: FileSystemDirectoryHandleLike }) => Promise<FileSystemDirectoryHandleLike>
}

const RECENT_WORKSPACES_KEY = 'meshingress.studio.local-workspace-recents.v1'
const MAX_RECENTS = 16
const DIRECTORY_HANDLES_DATABASE = 'meshingress.studio.local-workspaces.v1'
const DIRECTORY_HANDLES_STORE = 'directory-handles'

type StoredDirectoryHandle = {
  id: string
  handle: FileSystemDirectoryHandleLike
}

export type StoredDirectorySelection =
  | { selection: DirectorySelection }
  | { selection: null; reason: 'missing' | 'permission-denied' }

const sortNodes = (nodes: LocalWorkspaceNode[]) => nodes.toSorted((left, right) => {
  if (left.kind !== right.kind) return left.kind === 'directory' ? -1 : 1
  return left.name.localeCompare(right.name)
})

export async function readDirectory(
  handle: FileSystemDirectoryHandleLike,
  path = handle.name,
  maxDepth = 1,
  currentDepth = 0
): Promise<LocalWorkspaceNode> {
  const children: LocalWorkspaceNode[] = []
  for await (const entry of handle.values()) {
    const entryPath = `${path}/${entry.name}`
    if (entry.kind === 'directory') {
      if (currentDepth + 1 < maxDepth) {
        children.push(await readDirectory(entry, entryPath, maxDepth, currentDepth + 1))
      } else {
        children.push({
          name: entry.name,
          path: entryPath,
          kind: 'directory',
          children: [],
          handle: entry,
          isIndexed: false,
        })
      }
    } else {
      children.push({ name: entry.name, path: entryPath, kind: 'file' })
    }
  }
  return {
    name: handle.name,
    path,
    kind: 'directory',
    children: sortNodes(children),
    handle,
    isIndexed: currentDepth + 1 < maxDepth,
  }
}

export async function ensureDirectoryChildrenIndexed(node: LocalWorkspaceNode): Promise<boolean> {
  if (node.kind !== 'directory' || !node.handle || node.isIndexed) {
    return false
  }
  const children: LocalWorkspaceNode[] = []
  for await (const entry of node.handle.values()) {
    const entryPath = `${node.path}/${entry.name}`
    if (entry.kind === 'directory') {
      const existing = node.children?.find((c) => c.name === entry.name && c.kind === 'directory')
      children.push(
        existing ?? {
          name: entry.name,
          path: entryPath,
          kind: 'directory',
          children: [],
          handle: entry,
          isIndexed: false,
        }
      )
    } else {
      children.push({ name: entry.name, path: entryPath, kind: 'file' })
    }
  }
  node.children = sortNodes(children)
  node.isIndexed = true
  return true
}

export async function indexWorkspaceBackground(
  root: LocalWorkspaceNode,
  onUpdate?: (updatedRoot: LocalWorkspaceNode) => void,
  signal?: AbortSignal
): Promise<void> {
  const queue: LocalWorkspaceNode[] = [root]
  let hasUpdates = false

  while (queue.length > 0) {
    if (signal?.aborted) break

    const current = queue.shift()!
    if (current.kind === 'directory') {
      if (!current.isIndexed && current.handle) {
        const updated = await ensureDirectoryChildrenIndexed(current)
        if (updated) {
          hasUpdates = true
        }
      }

      if (current.children) {
        for (const child of current.children) {
          if (child.kind === 'directory' && !child.isIndexed && child.handle) {
            queue.push(child)
          }
        }
      }

      if (hasUpdates) {
        onUpdate?.({ ...root })
        hasUpdates = false
        await new Promise((resolve) => setTimeout(resolve, 10))
      }
    }
  }

  if (hasUpdates) {
    onUpdate?.({ ...root })
  }
}

function treeFromFiles(files: FileList): DirectorySelection | null {
  const firstFile = files.item(0)
  if (!firstFile) return null

  const rootName = (firstFile.webkitRelativePath || firstFile.name).split('/')[0] || firstFile.name
  const root: LocalWorkspaceNode = { name: rootName, path: rootName, kind: 'directory', children: [] }

  for (const file of Array.from(files)) {
    const segments = (file.webkitRelativePath || `${rootName}/${file.name}`).split('/').filter(Boolean)
    const relativeSegments = segments[0] === rootName ? segments.slice(1) : segments
    let directory = root
    for (const segment of relativeSegments.slice(0, -1)) {
      const children = directory.children ?? (directory.children = [])
      let child = children.find((candidate) => candidate.kind === 'directory' && candidate.name === segment)
      if (!child) {
        child = { name: segment, path: `${directory.path}/${segment}`, kind: 'directory', children: [] }
        children.push(child)
      }
      directory = child
    }
    const fileName = relativeSegments.at(-1) ?? file.name
    const children = directory.children ?? (directory.children = [])
    children.push({ name: fileName, path: `${directory.path}/${fileName}`, kind: 'file' })
  }

  const sortTree = (node: LocalWorkspaceNode) => {
    if (node.children) {
      node.children = sortNodes(node.children)
      node.children.forEach(sortTree)
    }
  }
  sortTree(root)
  return { name: rootName, root }
}

function chooseDirectoryWithInput(): Promise<DirectorySelection | null> {
  return new Promise((resolve) => {
    const input = document.createElement('input')
    input.type = 'file'
    input.multiple = true
    input.setAttribute('webkitdirectory', '')
    input.addEventListener('change', () => resolve(input.files ? treeFromFiles(input.files) : null), { once: true })
    input.addEventListener('cancel', () => resolve(null), { once: true })
    input.click()
  })
}

export async function chooseLocalDirectory(startIn?: FileSystemDirectoryHandleLike): Promise<DirectorySelection | null> {
  const pickerWindow = window as DirectoryPickerWindow
  try {
    if (pickerWindow.showDirectoryPicker) {
      const handle = await pickerWindow.showDirectoryPicker(startIn ? { startIn } : undefined)
      return { name: handle.name, root: await readDirectory(handle), handle }
    }
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') return null
    throw error
  }
  return chooseDirectoryWithInput()
}

function openDirectoryHandlesDatabase(): Promise<IDBDatabase | null> {
  if (!('indexedDB' in window)) return Promise.resolve(null)

  return new Promise((resolve) => {
    const request = window.indexedDB.open(DIRECTORY_HANDLES_DATABASE, 1)
    request.addEventListener('upgradeneeded', () => {
      if (!request.result.objectStoreNames.contains(DIRECTORY_HANDLES_STORE)) {
        request.result.createObjectStore(DIRECTORY_HANDLES_STORE, { keyPath: 'id' })
      }
    })
    request.addEventListener('success', () => resolve(request.result), { once: true })
    request.addEventListener('error', () => resolve(null), { once: true })
  })
}

export async function storeDirectoryHandle(id: string, handle: FileSystemDirectoryHandleLike): Promise<boolean> {
  const database = await openDirectoryHandlesDatabase()
  if (!database) return false

  return new Promise((resolve) => {
    const transaction = database.transaction(DIRECTORY_HANDLES_STORE, 'readwrite')
    transaction.objectStore(DIRECTORY_HANDLES_STORE).put({ id, handle } satisfies StoredDirectoryHandle)
    transaction.addEventListener('complete', () => { database.close(); resolve(true) }, { once: true })
    transaction.addEventListener('abort', () => { database.close(); resolve(false) }, { once: true })
    transaction.addEventListener('error', () => { database.close(); resolve(false) }, { once: true })
  })
}

async function readDirectoryHandle(id: string): Promise<FileSystemDirectoryHandleLike | null> {
  const database = await openDirectoryHandlesDatabase()
  if (!database) return null

  return new Promise((resolve) => {
    const transaction = database.transaction(DIRECTORY_HANDLES_STORE, 'readonly')
    const request = transaction.objectStore(DIRECTORY_HANDLES_STORE).get(id)
    request.addEventListener('success', () => {
      const stored = request.result as StoredDirectoryHandle | undefined
      resolve(stored?.handle?.kind === 'directory' ? stored.handle : null)
    }, { once: true })
    request.addEventListener('error', () => resolve(null), { once: true })
    transaction.addEventListener('complete', () => database.close(), { once: true })
    transaction.addEventListener('abort', () => database.close(), { once: true })
  })
}

export async function hasStoredDirectoryHandle(id: string): Promise<boolean> {
  return (await readDirectoryHandle(id)) !== null
}

async function canReadDirectory(handle: FileSystemDirectoryHandleLike): Promise<boolean> {
  if (!handle.queryPermission) return true
  const permission = await handle.queryPermission({ mode: 'read' })
  if (permission === 'granted') return true
  if (permission === 'denied' || !handle.requestPermission) return false
  return (await handle.requestPermission({ mode: 'read' })) === 'granted'
}

export async function readStoredDirectory(id: string): Promise<StoredDirectorySelection> {
  const handle = await readDirectoryHandle(id)
  if (!handle) return { selection: null, reason: 'missing' }
  if (!await canReadDirectory(handle)) return { selection: null, reason: 'permission-denied' }

  try {
    return { selection: { name: handle.name, root: await readDirectory(handle), handle } }
  } catch {
    return { selection: null, reason: 'permission-denied' }
  }
}

export async function chooseStoredDirectory(id: string): Promise<DirectorySelection | null> {
  return chooseLocalDirectory(await readDirectoryHandle(id) ?? undefined)
}

export function readRecentWorkspaceEntries(): RecentWorkspaceEntry[] {
  try {
    const value: unknown = JSON.parse(window.localStorage.getItem(RECENT_WORKSPACES_KEY) ?? '[]')
    if (!Array.isArray(value)) return []
    const entries = value.filter((entry): entry is RecentWorkspaceEntry => typeof entry === 'object' && entry !== null
      && typeof entry.id === 'string'
      && (entry.kind === 'workspace' || entry.kind === 'file')
      && typeof entry.label === 'string'
      && typeof entry.path === 'string')
    const uniqueEntries = deduplicateRecentWorkspaceEntries(entries)
    if (uniqueEntries.length !== entries.length) writeRecentWorkspaceEntries(uniqueEntries)
    return uniqueEntries
  } catch {
    return []
  }
}

function recentEntryKey(entry: RecentWorkspaceEntry) {
  return `${entry.kind}\u0000${entry.label}\u0000${entry.path}`
}

export function deduplicateRecentWorkspaceEntries(entries: RecentWorkspaceEntry[]): RecentWorkspaceEntry[] {
  const retainedKeys = new Set<string>()
  const earliestFirst = [...entries].reverse().filter((entry) => {
    const key = recentEntryKey(entry)
    if (retainedKeys.has(key)) return false
    retainedKeys.add(key)
    return true
  })
  return earliestFirst.reverse()
}

export function writeRecentWorkspaceEntries(entries: RecentWorkspaceEntry[]) {
  try {
    window.localStorage.setItem(RECENT_WORKSPACES_KEY, JSON.stringify(deduplicateRecentWorkspaceEntries(entries).slice(0, MAX_RECENTS)))
  } catch {
    // Browsing stays available when the user has disabled browser storage.
  }
}

export function upsertRecentEntry(entries: RecentWorkspaceEntry[], entry: RecentWorkspaceEntry): RecentWorkspaceEntry[] {
  return deduplicateRecentWorkspaceEntries([entry, ...entries.filter((candidate) => candidate.id !== entry.id)]).slice(0, MAX_RECENTS)
}

import { WORKFLOW_PROJECT_LAYOUT_V1, type WorkspaceLayoutPackage } from './workspace-layout'

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
  getDirectoryHandle?: (name: string, options?: { create?: boolean }) => Promise<FileSystemDirectoryHandleLike>
  getFileHandle?: (name: string, options?: { create?: boolean }) => Promise<FileSystemFileHandleLike>
  queryPermission?: (descriptor?: { mode?: 'read' | 'readwrite' }) => Promise<PermissionState>
  requestPermission?: (descriptor?: { mode?: 'read' | 'readwrite' }) => Promise<PermissionState>
}

export interface FileSystemFileHandleLike {
  kind: 'file'
  name: string
  getFile?: () => Promise<{ text: () => Promise<string> }>
  createWritable?: () => Promise<FileSystemWritableFileStreamLike>
}

interface FileSystemWritableFileStreamLike {
  write(data: string): Promise<void>
  close(): Promise<void>
}

type FileSystemHandleLike = FileSystemDirectoryHandleLike | FileSystemFileHandleLike

type DirectoryPickerWindow = Window & {
  showDirectoryPicker?: (options?: { startIn?: FileSystemDirectoryHandleLike; mode?: 'read' | 'readwrite' }) => Promise<FileSystemDirectoryHandleLike>
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

export async function chooseLocalDirectory(startIn?: FileSystemDirectoryHandleLike, mode: 'read' | 'readwrite' = 'read'): Promise<DirectorySelection | null> {
  const pickerWindow = window as DirectoryPickerWindow
  try {
    if (pickerWindow.showDirectoryPicker) {
      const handle = await pickerWindow.showDirectoryPicker({ ...(startIn ? { startIn } : {}), mode })
      return { name: handle.name, root: await readDirectory(handle), handle }
    }
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') return null
    throw error
  }
  return chooseDirectoryWithInput()
}

export function chooseLocalWorkspaceParent(): Promise<DirectorySelection | null> {
  return chooseLocalDirectory(undefined, 'readwrite')
}

const WINDOWS_RESERVED_NAMES = /^(con|prn|aux|nul|com[1-9]|lpt[1-9])(\..*)?$/i
const INVALID_WORKSPACE_NAME = /[<>:"/\\|?*]/

export function validateWorkspaceName(value: string): string {
  const name = value.trim()
  const hasControlCharacter = Array.from(name).some((character) => (character.codePointAt(0) ?? 0) < 32)
  if (!name || name === '.' || name === '..' || name.endsWith('.') || name.endsWith(' ') || hasControlCharacter || INVALID_WORKSPACE_NAME.test(name) || WINDOWS_RESERVED_NAMES.test(name)) {
    throw new Error('Use a non-empty workspace name without path characters or reserved names.')
  }
  return name
}

export async function ensureDirectoryPermission(handle: FileSystemDirectoryHandleLike, mode: 'read' | 'readwrite'): Promise<boolean> {
  if (!handle.queryPermission) return true
  const permission = await handle.queryPermission({ mode })
  if (permission === 'granted') return true
  if (permission === 'denied' || !handle.requestPermission) return false
  return (await handle.requestPermission({ mode })) === 'granted'
}

function isMissingDirectory(error: unknown): boolean {
  return error instanceof DOMException && error.name === 'NotFoundError'
}

async function directoryExists(parent: FileSystemDirectoryHandleLike, name: string): Promise<boolean> {
  try {
    await parent.getDirectoryHandle!(name)
    return true
  } catch (error) {
    if (isMissingDirectory(error)) return false
    if (error instanceof DOMException && error.name === 'TypeMismatchError') return true
    throw error
  }
}

async function materializeDirectory(root: FileSystemDirectoryHandleLike, relativePath: string): Promise<FileSystemDirectoryHandleLike> {
  let current = root
  for (const segment of relativePath.split('/')) {
    current = await current.getDirectoryHandle!(segment, { create: true })
  }
  return current
}

async function writeProjectFile(root: FileSystemDirectoryHandleLike, relativePath: string, contents: string): Promise<void> {
  const segments = relativePath.split('/')
  const fileName = segments.pop()
  if (!fileName) throw new Error(`Invalid generated project path: ${relativePath}`)
  const parent = segments.length ? await materializeDirectory(root, segments.join('/')) : root
  const file = await parent.getFileHandle!(fileName, { create: true })
  if (!file.createWritable) throw new Error('This browser cannot write project files through the selected folder.')
  const writable = await file.createWritable()
  await writable.write(contents)
  await writable.close()
}

async function validateGeneratedLayout(root: FileSystemDirectoryHandleLike, layout: WorkspaceLayoutPackage): Promise<void> {
  for (const path of layout.scaffold.directories) {
    let current = root
    for (const segment of path.split('/')) current = await current.getDirectoryHandle!(segment)
  }
  for (const path of Object.keys(layout.scaffold.files)) {
    const segments = path.split('/')
    const fileName = segments.pop()!
    let current = root
    for (const segment of segments) current = await current.getDirectoryHandle!(segment)
    await current.getFileHandle!(fileName)
  }
}

export async function createLocalWorkspaceProject(nameInput: string, parentSelection: DirectorySelection): Promise<DirectorySelection> {
  const name = validateWorkspaceName(nameInput)
  const parent = parentSelection.handle
  if (!parent?.getDirectoryHandle || !parent.getFileHandle) {
    throw new Error('New workspace creation requires a browser with writable File System Access support.')
  }
  if (!await ensureDirectoryPermission(parent, 'readwrite')) {
    throw new Error('Write permission is required for the selected parent folder.')
  }
  if (await directoryExists(parent, name)) {
    throw new Error(`A folder named ${name} already exists in the selected parent folder.`)
  }

  const projectRoot = await parent.getDirectoryHandle(name, { create: true })
  for (const path of WORKFLOW_PROJECT_LAYOUT_V1.scaffold.directories) {
    await materializeDirectory(projectRoot, path)
  }
  for (const [path, content] of Object.entries(WORKFLOW_PROJECT_LAYOUT_V1.scaffold.files)) {
    await writeProjectFile(projectRoot, path, content(name))
  }
  await validateGeneratedLayout(projectRoot, WORKFLOW_PROJECT_LAYOUT_V1)
  return { name: projectRoot.name, root: await readDirectory(projectRoot), handle: projectRoot }
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
  return ensureDirectoryPermission(handle, 'read')
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

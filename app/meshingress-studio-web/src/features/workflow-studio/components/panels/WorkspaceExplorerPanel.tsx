import { useEffect, useRef, useState } from 'react'
import {
    DragGridHorizontalIcon,
    FileObjectTypeIcon,
    FolderIcon,
    FolderOpenIcon,
    WorkflowFolderIcon,
} from '../../../../components/icons/node-icons'
import { ensureDirectoryChildrenIndexed, type LocalWorkspace, type LocalWorkspaceNode, type RecentWorkspaceEntry } from '../../local-workspace'

export interface WorkspaceExplorerPanelProps {
    workspace?: LocalWorkspace | null
    recents?: RecentWorkspaceEntry[]
    recentItemsWithoutHandle?: ReadonlySet<string>
    onFileSelect?: (file: LocalWorkspaceNode) => void
    onOpenWorkspace?: () => void
    onOpenRecent?: (recent: RecentWorkspaceEntry) => void
    expandAllState?: boolean | null
    expandKey?: number
}

function WorkspaceTree({
    node,
    depth = 0,
    onFileSelect,
    expandAllState,
    expandKey,
}: {
    node: LocalWorkspaceNode
    depth?: number
    onFileSelect?: (node: LocalWorkspaceNode) => void
    expandAllState?: boolean | null
    expandKey?: number
}) {
    const isRoot = depth === 0
    const defaultOpen = isRoot ? true : (expandAllState === true)
    const [isOpen, setIsOpen] = useState(defaultOpen)

    useEffect(() => {
        if (expandAllState !== null && expandAllState !== undefined) {
            setIsOpen(isRoot ? true : expandAllState)
        }
    }, [expandAllState, expandKey, isRoot])

    if (node.kind === 'file') {
        return (
            <button className="tree-view-item list-item workspace-tree-file" onClick={() => onFileSelect?.(node)} title={node.path} type="button">
                <FileObjectTypeIcon size={15} />
                <span>{node.name}</span>
            </button>
        )
    }

    const handleToggle = (e: React.SyntheticEvent<HTMLDetailsElement>) => {
        const targetOpen = e.currentTarget.open
        setIsOpen(targetOpen)
        if (targetOpen && node.handle && !node.isIndexed) {
            void ensureDirectoryChildrenIndexed(node)
        }
    }

    return (
        <details className="workspace-tree-directory" open={isOpen} onToggle={handleToggle}>
            <summary className="tree-view-item list-item" title={node.path}>
                <FolderIcon className="workspace-tree-folder-closed" size={16} />
                <FolderOpenIcon className="workspace-tree-folder-open" size={16} />
                <span>{node.name}</span>
            </summary>
            {node.children?.length ? (
                <div className="workspace-tree-children">
                    {node.children.map((child) => (
                        <WorkspaceTree
                            key={child.path}
                            node={child}
                            depth={depth + 1}
                            onFileSelect={onFileSelect}
                            expandAllState={expandAllState}
                            expandKey={expandKey}
                        />
                    ))}
                </div>
            ) : null}
        </details>
    )
}

const MIN_HEIGHT = 60
const DEFAULT_HEIGHT = 180
const DRAG_THRESHOLD = 5

export function WorkspaceExplorerPanel({
    workspace,
    recents = [],
    recentItemsWithoutHandle = new Set(),
    onFileSelect,
    onOpenWorkspace,
    onOpenRecent,
    expandAllState,
    expandKey,
}: WorkspaceExplorerPanelProps) {
    const [isCollapsed, setIsCollapsed] = useState(false)
    const [bodyHeight, setBodyHeight] = useState(DEFAULT_HEIGHT)
    const lastHeightRef = useRef(DEFAULT_HEIGHT)
    const draggingRef = useRef(false)
    const activePointerIdRef = useRef<number | null>(null)
    const isCollapsedRef = useRef(false)
    const bodyHeightRef = useRef(DEFAULT_HEIGHT)
    const startYRef = useRef(0)
    const startHeightRef = useRef(DEFAULT_HEIGHT)

    const handlePointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
        if (e.button !== 0 || activePointerIdRef.current !== null) return
        draggingRef.current = false
        activePointerIdRef.current = e.pointerId
        startYRef.current = e.clientY
        startHeightRef.current = isCollapsedRef.current ? 0 : bodyHeightRef.current
        e.currentTarget.setPointerCapture(e.pointerId)
    }

    const handlePointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
        if (activePointerIdRef.current !== e.pointerId) return
        const deltaY = startYRef.current - e.clientY
        if (!draggingRef.current && Math.abs(deltaY) <= DRAG_THRESHOLD) return

        draggingRef.current = true
        const newHeight = startHeightRef.current + deltaY

        if (isCollapsedRef.current) {
            if (deltaY > DRAG_THRESHOLD) {
                const restoredHeight = Math.max(MIN_HEIGHT, Math.max(lastHeightRef.current, MIN_HEIGHT) + (deltaY - DRAG_THRESHOLD))
                isCollapsedRef.current = false
                bodyHeightRef.current = restoredHeight
                setIsCollapsed(false)
                setBodyHeight(restoredHeight)
                lastHeightRef.current = restoredHeight
            }
        } else {
            if (newHeight <= MIN_HEIGHT) {
                isCollapsedRef.current = true
                setIsCollapsed(true)
            } else {
                bodyHeightRef.current = newHeight
                setIsCollapsed(false)
                setBodyHeight(newHeight)
                lastHeightRef.current = newHeight
            }
        }
    }

    const finishPointer = (e: React.PointerEvent<HTMLDivElement>) => {
        if (activePointerIdRef.current !== e.pointerId) return
        activePointerIdRef.current = null
        draggingRef.current = false
        if (e.currentTarget.hasPointerCapture(e.pointerId)) {
            e.currentTarget.releasePointerCapture(e.pointerId)
        }
    }

    const handleDoubleClick = () => {
        if (isCollapsedRef.current) {
            const restoredHeight = lastHeightRef.current || DEFAULT_HEIGHT
            isCollapsedRef.current = false
            bodyHeightRef.current = restoredHeight
            setIsCollapsed(false)
            setBodyHeight(restoredHeight)
        } else {
            isCollapsedRef.current = true
            setIsCollapsed(true)
        }
    }

    return (
        <>
            {workspace ? (
                <div className='explorer-view explorer-workspace'>
                    <div className="explorer-workspace-toolbar">
                        <button className="explorer-open-workspace" onClick={onOpenWorkspace} type="button">
                            <WorkflowFolderIcon size={16} />
                            <span>{workspace?.name ?? 'Open workspace'}</span>
                        </button>
                    </div>
                    <div aria-label={`${workspace.name} contents`} className="workspace-tree">
                        <WorkspaceTree
                            node={workspace.root}
                            depth={0}
                            onFileSelect={onFileSelect}
                            expandAllState={expandAllState}
                            expandKey={expandKey}
                        />
                    </div>
                </div>
            ) : (
                <div className='explorer-workspace' >
                    <div className="explorer-workspace-toolbar">
                        <button className="explorer-open-workspace" onClick={onOpenWorkspace} type="button">
                            <WorkflowFolderIcon size={16} />
                            <span>{'Open workspace'}</span>
                        </button>
                    </div>

                    <div className="explorer-view explorer-empty">
                        <WorkflowFolderIcon size={28} />
                        <strong>No workspace open</strong>
                        <span>Open a local folder to browse it here.</span>
                    </div>
                </div>
            )}
            {recents.length ? (
                <section className={`explorer-recents${isCollapsed ? ' is-collapsed' : ''}`} aria-label="Recent workspaces and files">
                    <div
                        className="panel-section-header"
                        onDoubleClick={handleDoubleClick}
                        onPointerDown={handlePointerDown}
                        onPointerMove={handlePointerMove}
                        onPointerCancel={finishPointer}
                        onLostPointerCapture={finishPointer}
                        onPointerUp={finishPointer}
                    >
                        <h3>Recent</h3>
                        <DragGridHorizontalIcon size={18} />
                    </div>
                    <div className="panel-section-body" style={{ height: `${bodyHeight}px` }}>
                        {recents.map((entry) => (
                            <button className={`explorer-recent-item${recentItemsWithoutHandle.has(entry.id) ? ' is-missing-handle' : ''}`} key={entry.id} onClick={() => onOpenRecent?.(entry)} title={entry.path} type="button">
                                <span>
                                    {entry.kind === 'workspace' ? <WorkflowFolderIcon size={15} /> : <FileObjectTypeIcon size={15} />}
                                </span>
                                <span>
                                    <strong>{entry.label}</strong>
                                    <small>{entry.path}</small>
                                </span>
                            </button>
                        ))}
                    </div>
                </section>
            ) : null}
        </>
    )
}

export default WorkspaceExplorerPanel

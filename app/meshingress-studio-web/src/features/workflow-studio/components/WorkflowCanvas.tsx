import { memo, useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { Close, CriticalIcon, CubeIcon, CubeFilledIcon, WarningIcon, WorkflowNodeIcon, PathIcon, CopyIcon, FavoriteIcon, DeleteIcon, type NodeIconDescriptor } from '../../../components/icons/node-icons'
import { hasStructuredOutput, type McpToolFunction } from '../../../api/mcp'
import { CanvasActions } from './CanvasActions'
import { clamp } from '../canvas/grid'
import { constrainNodePosition, findNodeSpawnPosition } from '../canvas/node-spawner'
import { reconnectPreviewPath, roundedPath, routeEdges } from '../canvas/router'
import { CANVAS_SIZE, GRID_SIZE, NODE_HEIGHT, NODE_WIDTH, NODE_STAGE_INSET, TOOL_NODE_DRAG_TYPE, type ElementAttributeInput, type NodeRunState, type WorkflowEdge, type WorkflowNode } from '../types'
import { formatElementAttributes } from '../utilities'
import type { ToolPresentationIndex } from '../node-presentation'

interface WorkflowCanvasProps {
    attributes?: ElementAttributeInput
    overlayHost?: HTMLElement | null
    nodes: WorkflowNode[];
    edges: WorkflowEdge[];
    selectedNodeId: string;
    nodeRunStates: Record<string, NodeRunState>;
    onSelectLayoutNodeChange?: (selectNode: ((nodeId: string) => void) | null) => void;
    onSelect: (id: string) => void;
    onNodesChange: (change: (nodes: WorkflowNode[]) => WorkflowNode[]) => void;
    onToolFavorite: (node: WorkflowNode) => void;
    onEdgeReconnect: (edge: WorkflowEdge, targetId: string) => void
    onEdgeCreate: (sourceId: string, targetId: string) => void
    onEdgeDelete?: (edge: WorkflowEdge) => void
    reattachOnEmptyRelease?: boolean
    onToggleReattachOnEmptyRelease?: () => void
    toolFunctions: McpToolFunction[]
    presentations: ToolPresentationIndex
    running: boolean
    onRun: () => void
    onValidate: () => void
    onStatus: (message: string) => void
    pendingToolNode: { requestId: string; toolName: string } | null
    onToolInsert: (toolName: string, position: { x: number; y: number }) => void
    onPendingToolNodeHandled: (requestId: string) => void
}

const INPUT_PORT_SNAP_RADIUS = GRID_SIZE
const LAYOUT_REVEAL_DURATION_MS = 280
const LAYOUT_FOCUS_RESET_DELAY_MS = 450
type LayoutPoint = { x: number; y: number }
type WorkflowNodeCommand = 'copy' | 'cut' | 'paste' | 'duplicate' | 'delete'

function workflowLayoutNodes(nodes: WorkflowNode[], edges: WorkflowEdge[]) {
    const nodesById = new Map(nodes.map((node) => [node.id, node]))
    const included = new Set<string>()
    const ordered: WorkflowNode[] = []
    const include = (id: string) => {
        const node = nodesById.get(id)
        if (!node || included.has(id)) return
        included.add(id)
        ordered.push(node)
    }

    edges.forEach((edge) => {
        include(edge.source)
        include(edge.target)
    })
    nodes.forEach((node) => include(node.id))
    return ordered
}

function reachableWorkflowNodeIds(nodes: WorkflowNode[], edges: WorkflowEdge[]) {
    const trigger = nodes.find((node) => node.kind === 'trigger')
    const reachable = new Set<string>()
    const pending = trigger ? [trigger.id] : []
    while (pending.length) {
        const source = pending.shift()!
        if (reachable.has(source)) continue
        reachable.add(source)
        edges.filter((edge) => edge.source === source).forEach((edge) => pending.push(edge.target))
    }
    return reachable
}

function layoutRoadPath(start: LayoutPoint, end: LayoutPoint, laneX: number) {
    const points = [start, { x: laneX, y: start.y }, { x: laneX, y: end.y }, end].filter((point, index, all) => index === 0 || point.x !== all[index - 1].x || point.y !== all[index - 1].y)
    return roundedPath(points, 4)
}

export function WorkflowCanvas({ attributes, overlayHost, nodes, edges, selectedNodeId, nodeRunStates, onSelectLayoutNodeChange, onSelect, onNodesChange, onToolFavorite, onEdgeReconnect, onEdgeCreate, onEdgeDelete, reattachOnEmptyRelease = false, onToggleReattachOnEmptyRelease, toolFunctions, presentations, running, onRun, onValidate, onStatus, pendingToolNode, onToolInsert, onPendingToolNodeHandled }: WorkflowCanvasProps) {
    const [transform, setTransform] = useState({ x: 36, y: 24, zoom: 0.86 })
    const [worldSize, setWorldSize] = useState(CANVAS_SIZE)
    const [reconnecting, setReconnecting] = useState<{ sourceId: string; edge?: WorkflowEdge; head: { x: number; y: number }; targetId: string | null } | null>(null)
    const [documentationOpen, setDocumentationOpen] = useState(false)
    const [layoutFocusNodeId, setLayoutFocusNodeId] = useState<string | null>(null)
    const [layoutRevealing, setLayoutRevealing] = useState(false)
    const canvasRef = useRef<HTMLDivElement>(null)
    const canvasWidgetPanelRef = useRef<HTMLDivElement>(null)
    const transformRef = useRef(transform)
    const layoutRevealTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
    const activePointer = useRef<number | null>(null)
    const pan = useRef<{ pointerId: number; startX: number; startY: number; originX: number; originY: number } | null>(null)
    const edgeDrag = useRef<{ pointerId: number; sourceId: string; edge?: WorkflowEdge } | null>(null)
    const routes = useMemo(() => routeEdges(nodes, edges, worldSize.width, worldSize.height), [nodes, edges, worldSize])
    const layoutNodes = useMemo(() => workflowLayoutNodes(nodes, edges), [nodes, edges])
    const selectedNode = useMemo(() => nodes.find((node) => node.id === selectedNodeId), [nodes, selectedNodeId])
    const selectedTool = useMemo(() => {
        return selectedNode ? toolFunctions.find((tool) => tool.name === `${selectedNode.toolId}.${selectedNode.functionName}`) : undefined
    }, [selectedNode, toolFunctions])
    transformRef.current = transform
    useLayoutEffect(() => {
        const canvas = canvasRef.current
        if (!canvas) return undefined
        const updateWorldSize = () => {
            const bounds = canvas.getBoundingClientRect()
            setWorldSize({
                width: Math.max(CANVAS_SIZE.width, Math.ceil(bounds.width / transform.zoom / GRID_SIZE) * GRID_SIZE),
                height: Math.max(CANVAS_SIZE.height, Math.ceil(bounds.height / transform.zoom / GRID_SIZE) * GRID_SIZE)
            })
        }
        updateWorldSize()
        const observer = new ResizeObserver(updateWorldSize)
        observer.observe(canvas)
        return () => observer.disconnect()
    }, [transform.zoom])
    useEffect(() => () => {
        if (layoutRevealTimer.current !== null) clearTimeout(layoutRevealTimer.current)
    }, [])
    const nodePosition = useCallback((rawX: number, rawY: number) => constrainNodePosition(rawX, rawY, worldSize), [worldSize])
    const updatePosition = useCallback((id: string, rawX: number, rawY: number) => onNodesChange((current) => {
        const node = current.find((candidate) => candidate.id === id)
        if (!node) return current
        const target = nodePosition(rawX, rawY)
        const obstacles = current.filter((other) => other.id !== id)
        const collides = (position: { x: number; y: number }, other: WorkflowNode) => (
            position.x - NODE_STAGE_INSET < other.x + NODE_WIDTH
            && position.x + NODE_WIDTH + NODE_STAGE_INSET > other.x
            && position.y - NODE_STAGE_INSET < other.y + NODE_HEIGHT
            && position.y + NODE_HEIGHT + NODE_STAGE_INSET > other.y
        )
        const isOpen = (position: { x: number; y: number }) => !obstacles.some((other) => collides(position, other))
        const resolved = (() => {
            if (isOpen(target)) return target

            const distanceToTarget = (position: { x: number; y: number }) => (
                (position.x - target.x) ** 2 + (position.y - target.y) ** 2
            )
            const currentDistance = distanceToTarget(node)
            const candidates = obstacles.flatMap((obstacle) => {
                const left = obstacle.x - NODE_WIDTH - NODE_STAGE_INSET
                const right = obstacle.x + NODE_WIDTH + NODE_STAGE_INSET
                const top = obstacle.y - NODE_HEIGHT - NODE_STAGE_INSET
                const bottom = obstacle.y + NODE_HEIGHT + NODE_STAGE_INSET

                // Only stay on an edge that the node is already able to reach. This keeps
                // the effect as a slide around the contacted edge rather than pathfinding.
                return [
                    ...(node.x <= left ? [nodePosition(left, target.y)] : []),
                    ...(node.x >= right ? [nodePosition(right, target.y)] : []),
                    ...(node.y <= top ? [nodePosition(target.x, top)] : []),
                    ...(node.y >= bottom ? [nodePosition(target.x, bottom)] : []),
                ]
            }).filter(isOpen)

            return candidates.reduce<{ x: number; y: number } | null>((closest, candidate) => {
                if (distanceToTarget(candidate) >= currentDistance) return closest
                return !closest || distanceToTarget(candidate) < distanceToTarget(closest) ? candidate : closest
            }, null) ?? { x: node.x, y: node.y }
        })()

        if (resolved.x === node.x && resolved.y === node.y) return current
        return current.map((candidate) => candidate.id === id ? { ...candidate, ...resolved } : candidate)
    }), [nodePosition, onNodesChange])
    const beginLayoutReveal = useCallback(() => {
        if (layoutRevealTimer.current !== null) clearTimeout(layoutRevealTimer.current)
        setLayoutRevealing(true)
        layoutRevealTimer.current = setTimeout(() => {
            layoutRevealTimer.current = null
            setLayoutRevealing(false)
        }, LAYOUT_REVEAL_DURATION_MS)
    }, [])
    const selectLayoutNode = useCallback((nodeId: string) => {
        onSelect(nodeId)
        const node = nodes.find((candidate) => candidate.id === nodeId)
        const canvas = canvasRef.current
        if (!node || !canvas) return

        const bounds = canvas.getBoundingClientRect()
        const widgetBounds = canvasWidgetPanelRef.current?.getBoundingClientRect()
        const visibleHeight = widgetBounds
            ? Math.max(0, Math.min(bounds.height, widgetBounds.bottom - bounds.top))
            : bounds.height
        const current = transformRef.current
        const viewport = {
            left: -current.x / current.zoom,
            top: -current.y / current.zoom,
            right: (bounds.width - current.x) / current.zoom,
            bottom: (visibleHeight - current.y) / current.zoom,
        }
        const horizontalPlacement = node.x < viewport.left ? 0.25 : node.x + NODE_WIDTH > viewport.right ? 0.75 : null
        const verticalPlacement = node.y < viewport.top ? 0.25 : node.y + NODE_HEIGHT > viewport.bottom ? 0.75 : null
        if (horizontalPlacement === null && verticalPlacement === null) return

        beginLayoutReveal()
        setTransform({
            ...current,
            x: horizontalPlacement === null ? current.x : bounds.width * horizontalPlacement - (node.x + NODE_WIDTH / 2) * current.zoom,
            y: verticalPlacement === null ? current.y : visibleHeight * verticalPlacement - (node.y + NODE_HEIGHT / 2) * current.zoom,
        })
    }, [beginLayoutReveal, nodes, onSelect])
    useEffect(() => {
        onSelectLayoutNodeChange?.(selectLayoutNode)
        return () => onSelectLayoutNodeChange?.(null)
    }, [onSelectLayoutNodeChange, selectLayoutNode])
    const claimPointer = useCallback((event: React.PointerEvent<Element>): boolean => {
        if (!event.isPrimary || activePointer.current !== null) return false
        activePointer.current = event.pointerId
        return true
    }, [])
    const releasePointer = useCallback((pointerId: number) => {
        if (activePointer.current === pointerId) activePointer.current = null
    }, [])
    const capturePointer = useCallback((element: HTMLElement, pointerId: number): boolean => {
        try {
            element.setPointerCapture(pointerId)
            return true
        } catch {
            return false
        }
    }, [])
    const releaseCapturedPointer = useCallback((element: HTMLElement, pointerId: number) => {
        if (element.hasPointerCapture(pointerId)) element.releasePointerCapture(pointerId)
    }, [])
    const onPointerDown = (event: React.PointerEvent<HTMLDivElement>) => {
        if (event.button !== 0 || (event.target as Element).closest('.node, .edge, .tool-reference, .workflow-layout-map, .selected-node-data') || !claimPointer(event)) return
        pan.current = { pointerId: event.pointerId, startX: event.clientX, startY: event.clientY, originX: transform.x, originY: transform.y }
        if (!capturePointer(event.currentTarget, event.pointerId)) {
            pan.current = null
            releasePointer(event.pointerId)
        }
    }
    const inputPortAt = useCallback((event: React.PointerEvent<HTMLDivElement>) => {
        const element = document.elementFromPoint(event.clientX, event.clientY)
        const node = element?.closest<HTMLElement>('article.node')
        const port = node?.querySelector<HTMLElement>('[data-input-port]')
        const nodeId = port?.dataset.nodeId
        if (!port || !nodeId) return null

        const portBounds = port.getBoundingClientRect()
        const distance = Math.hypot(
            event.clientX - (portBounds.left + portBounds.width / 2),
            event.clientY - (portBounds.top + portBounds.height / 2),
        )
        return distance <= INPUT_PORT_SNAP_RADIUS * transformRef.current.zoom ? nodeId : null
    }, [])
    const inputPortWorldPosition = useCallback((nodeId: string) => {
        const node = nodes.find((candidate) => candidate.id === nodeId)
        return node ? { x: node.x, y: node.y + GRID_SIZE } : null
    }, [nodes])
    const pointerWorldPosition = useCallback((event: React.PointerEvent<Element>) => {
        const bounds = canvasRef.current?.getBoundingClientRect()
        if (!bounds) return { x: 0, y: 0 }
        const currentTransform = transformRef.current
        return {
            x: clamp((event.clientX - bounds.left - currentTransform.x) / currentTransform.zoom, 0, worldSize.width),
            y: clamp((event.clientY - bounds.top - currentTransform.y) / currentTransform.zoom, 0, worldSize.height),
        }
    }, [worldSize])
    const insertToolNode = useCallback((toolName: string, target: { x: number; y: number }) => {
        const position = findNodeSpawnPosition(target, nodes, worldSize)
        if (!position) {
            onStatus(`Could not add ${toolName}: the canvas has no open node position.`)
            return
        }
        onToolInsert(toolName, position)
    }, [nodes, onStatus, onToolInsert, worldSize])
    const insertPendingToolNode = useCallback(() => {
        if (!pendingToolNode) return
        const canvas = canvasRef.current
        if (!canvas) return
        const bounds = canvas.getBoundingClientRect()
        const current = transformRef.current
        insertToolNode(pendingToolNode.toolName, {
            x: (bounds.width / 2 - current.x) / current.zoom - NODE_WIDTH / 2,
            y: (bounds.height / 2 - current.y) / current.zoom - NODE_HEIGHT / 2,
        })
        onPendingToolNodeHandled(pendingToolNode.requestId)
    }, [insertToolNode, onPendingToolNodeHandled, pendingToolNode])
    useEffect(() => {
        insertPendingToolNode()
    }, [insertPendingToolNode])
    const onDragOver = (event: React.DragEvent<HTMLDivElement>) => {
        if (event.dataTransfer.types.includes(TOOL_NODE_DRAG_TYPE)) event.preventDefault()
    }
    const onDrop = (event: React.DragEvent<HTMLDivElement>) => {
        const toolName = event.dataTransfer.getData(TOOL_NODE_DRAG_TYPE)
        if (!toolName) return
        event.preventDefault()
        const bounds = event.currentTarget.getBoundingClientRect()
        const current = transformRef.current
        insertToolNode(toolName, {
            x: (event.clientX - bounds.left - current.x) / current.zoom - NODE_WIDTH / 2,
            y: (event.clientY - bounds.top - current.y) / current.zoom - NODE_HEIGHT / 2,
        })
    }
    const beginEdgeDrag = useCallback((sourceId: string, event: React.PointerEvent, edge?: WorkflowEdge) => {
        if (event.button !== 0 || !claimPointer(event)) return
        event.preventDefault()
        event.stopPropagation()
        edgeDrag.current = { pointerId: event.pointerId, sourceId, edge }
        setReconnecting({ sourceId, edge, head: pointerWorldPosition(event), targetId: null })
        const canvas = canvasRef.current
        if (!canvas || !capturePointer(canvas, event.pointerId)) {
            edgeDrag.current = null
            setReconnecting(null)
            releasePointer(event.pointerId)
        }
    }, [capturePointer, claimPointer, pointerWorldPosition, releasePointer])
    const beginReconnect = useCallback((edge: WorkflowEdge, event: React.PointerEvent) => beginEdgeDrag(edge.source, event, edge), [beginEdgeDrag])
    const beginConnection = useCallback((sourceId: string, event: React.PointerEvent<HTMLElement>) => beginEdgeDrag(sourceId, event), [beginEdgeDrag])
    const onPointerMove = (event: React.PointerEvent<HTMLDivElement>) => {
        if (edgeDrag.current?.pointerId === event.pointerId) {
            const targetId = inputPortAt(event)
            const validTarget = targetId && targetId !== edgeDrag.current.sourceId ? targetId : null
            const head = validTarget ? inputPortWorldPosition(validTarget) ?? pointerWorldPosition(event) : pointerWorldPosition(event)
            setReconnecting((current) => current && (current.targetId !== validTarget || current.head.x !== head.x || current.head.y !== head.y)
                ? { ...current, head, targetId: validTarget }
                : current)
            return
        }
        const gesture = pan.current
        if (gesture?.pointerId === event.pointerId) {
            const { originX, originY, startX, startY } = gesture
            const { clientX, clientY } = event
            setTransform((current) => ({
                ...current,
                x: originX + clientX - startX,
                y: originY + clientY - startY
            }))
        }
    }
    const endPan = (event: React.PointerEvent<HTMLDivElement>) => {
        if (edgeDrag.current?.pointerId === event.pointerId) {
            const { edge, sourceId } = edgeDrag.current
            edgeDrag.current = null
            setReconnecting(null)
            const targetId = event.type === 'pointercancel' ? null : inputPortAt(event)
            if (targetId && targetId !== sourceId && (!edge || targetId !== edge.target)) {
                if (edge) onEdgeReconnect(edge, targetId)
                else onEdgeCreate(sourceId, targetId)
                onSelect(targetId)
            } else if (edge && (!targetId || targetId === sourceId)) {
                if (!reattachOnEmptyRelease) {
                    if (onEdgeDelete) {
                        onEdgeDelete(edge)
                    }
                    onStatus(`Removed path edge from ${sourceId} to ${edge.target}`)
                } else {
                    onStatus(`Reattached path edge back to ${edge.target}`)
                }
            }
            releasePointer(event.pointerId)
            releaseCapturedPointer(event.currentTarget, event.pointerId)
            return
        }
        if (pan.current?.pointerId === event.pointerId) {
            pan.current = null;
            releasePointer(event.pointerId)
            releaseCapturedPointer(event.currentTarget, event.pointerId)
        }
    }
    const onLostPointerCapture = (event: React.PointerEvent<HTMLDivElement>) => {
        if (edgeDrag.current?.pointerId === event.pointerId) {
            edgeDrag.current = null
            setReconnecting(null)
        }
        if (pan.current?.pointerId === event.pointerId) pan.current = null
        releasePointer(event.pointerId)
    }
    const onWheel = (event: React.WheelEvent<HTMLDivElement>) => {
        event.preventDefault();
        const bounds = event.currentTarget.getBoundingClientRect();
        const pointerX = event.clientX - bounds.left;
        const pointerY = event.clientY - bounds.top;
        setTransform((current) => {
            const worldX = (pointerX - current.x) / current.zoom;
            const worldY = (pointerY - current.y) / current.zoom;
            const zoom = clamp(current.zoom * (event.deltaY < 0 ? 1.1 : 0.9), 0.35, 1.9);
            return { zoom, x: pointerX - worldX * zoom, y: pointerY - worldY * zoom }
        })
    }

    const clipboardNodeRef = useRef<WorkflowNode | null>(null)

    const handleCopyNode = useCallback((nodeToCopy: WorkflowNode) => {
        clipboardNodeRef.current = nodeToCopy
        onStatus(`Copied ${nodeToCopy.title} to clipboard`)
        if (navigator.clipboard) {
            navigator.clipboard.writeText(JSON.stringify(nodeToCopy, null, 2)).catch(() => { })
        }
    }, [onStatus])

    const handleDuplicateNode = useCallback((nodeToDuplicate: WorkflowNode) => {
        const newId = `node-${Date.now().toString(36).slice(-4)}`
        let copyOutput = `${nodeToDuplicate.output}_copy`
        let suffix = 1
        while (nodes.some((n) => n.output === copyOutput)) {
            suffix++
            copyOutput = `${nodeToDuplicate.output}_copy${suffix}`
        }
        const { x, y } = nodePosition(nodeToDuplicate.x + GRID_SIZE, nodeToDuplicate.y + GRID_SIZE)
        const newNode: WorkflowNode = {
            ...nodeToDuplicate,
            id: newId,
            title: `${nodeToDuplicate.title} (Copy)`,
            output: copyOutput,
            x,
            y,
        }
        onNodesChange((current) => [...current, newNode])
        onSelect(newNode.id)
        onStatus(`Duplicated ${nodeToDuplicate.title} as ${newNode.id}`)
    }, [nodePosition, nodes, onNodesChange, onSelect, onStatus])

    const handleDeleteNode = useCallback((nodeToDelete: WorkflowNode) => {
        const connectedEdges = edges.filter((edge) => edge.source === nodeToDelete.id || edge.target === nodeToDelete.id)
        if (onEdgeDelete) {
            connectedEdges.forEach((edge) => onEdgeDelete(edge))
        }
        onNodesChange((current) => current.filter((n) => n.id !== nodeToDelete.id))
        onStatus(`Deleted node ${nodeToDelete.title}`)
    }, [edges, onEdgeDelete, onNodesChange, onStatus])

    const handleCutNode = useCallback((nodeToCut: WorkflowNode) => {
        handleCopyNode(nodeToCut)
        handleDeleteNode(nodeToCut)
        onStatus(`Cut ${nodeToCut.title}`)
    }, [handleCopyNode, handleDeleteNode, onStatus])

    const handlePasteNode = useCallback(async () => {
        let sourceNode = clipboardNodeRef.current
        if (navigator.clipboard) {
            try {
                const text = await navigator.clipboard.readText()
                if (text) {
                    const parsed = JSON.parse(text)
                    if (parsed && typeof parsed === 'object' && parsed.id && parsed.title && parsed.output) {
                        sourceNode = parsed as WorkflowNode
                    }
                }
            } catch {
                // Clipboard read fallback
            }
        }
        if (!sourceNode) return
        handleDuplicateNode(sourceNode)
    }, [handleDuplicateNode])

    const handleFavoriteNode = useCallback((nodeToFav: WorkflowNode) => {
        onToolFavorite(nodeToFav)
    }, [onToolFavorite])

    const handleNodeCommand = useCallback((command: WorkflowNodeCommand) => {
        if (command === 'paste') {
            void handlePasteNode()
            return
        }
        if (!selectedNode) return
        if (command === 'copy') handleCopyNode(selectedNode)
        else if (command === 'cut') handleCutNode(selectedNode)
        else if (command === 'duplicate') handleDuplicateNode(selectedNode)
        else handleDeleteNode(selectedNode)
    }, [handleCopyNode, handleCutNode, handleDeleteNode, handleDuplicateNode, handlePasteNode, selectedNode])

    useEffect(() => {
        const handleKeyDown = (event: KeyboardEvent) => {
            const target = event.target as HTMLElement | null
            const isEditingText = target && (
                target.tagName === 'INPUT' ||
                target.tagName === 'TEXTAREA' ||
                target.isContentEditable
            )
            if (isEditingText) return

            const focusedNode = document.activeElement?.closest<HTMLElement>('article.node')
            const selection = window.getSelection()
            if (!selectedNode || focusedNode?.dataset.nodeId !== selectedNode.id || !selection?.isCollapsed) return

            const isMod = event.ctrlKey || event.metaKey
            const key = event.key.toLowerCase()

            if (event.key === 'Delete' || event.key === 'Backspace') {
                event.preventDefault()
                handleNodeCommand('delete')
                return
            }

            if (isMod && key === 'c') {
                event.preventDefault()
                handleNodeCommand('copy')
                return
            }

            if (isMod && key === 'x') {
                event.preventDefault()
                handleNodeCommand('cut')
                return
            }

            if (isMod && key === 'd') {
                event.preventDefault()
                handleNodeCommand('duplicate')
                return
            }

            if (isMod && key === 'v') {
                event.preventDefault()
                handleNodeCommand('paste')
                return
            }
        }

        const handleMenuCommand = (event: Event) => {
            const command = (event as CustomEvent<WorkflowNodeCommand>).detail
            if (command === 'copy' || command === 'cut' || command === 'paste' || command === 'duplicate' || command === 'delete') handleNodeCommand(command)
        }

        window.addEventListener('keydown', handleKeyDown)
        window.addEventListener('workflow-node-command', handleMenuCommand)
        return () => {
            window.removeEventListener('keydown', handleKeyDown)
            window.removeEventListener('workflow-node-command', handleMenuCommand)
        }
    }, [handleNodeCommand, selectedNode])

    const canvasProps = formatElementAttributes({
        'aria-label': 'Workflow canvas. Drag empty space to pan and use the mouse wheel to zoom.',
        className: `canvas${pan.current ? ' panning' : ''}${layoutRevealing ? ' layout-revealing' : ''}`,
        tabIndex: 0,
    }, attributes)

    const canvasWidgets = <div className="canvas-widget-panel" ref={canvasWidgetPanelRef}>
        <WorkflowLayoutMap edges={edges} focusedNodeId={layoutFocusNodeId ?? selectedNodeId} nodes={layoutNodes} onFocusChange={setLayoutFocusNodeId} onSelect={selectLayoutNode} toolFunctions={toolFunctions} />
        <CanvasActions onRun={onRun} onStatus={onStatus} onToggleReattachOnEmptyRelease={onToggleReattachOnEmptyRelease} onValidate={onValidate} reattachOnEmptyRelease={reattachOnEmptyRelease} running={running} />
        <SelectedNodeData node={selectedNode} tool={selectedTool} />
        {documentationOpen && <ToolReference tool={selectedTool} onClose={() => setDocumentationOpen(false)} />}
    </div>

    return <div {...canvasProps} onDragOver={onDragOver} onDrop={onDrop} onLostPointerCapture={onLostPointerCapture} onPointerCancel={endPan} onPointerDown={onPointerDown}
        onPointerMove={onPointerMove} onPointerUp={endPan} onWheel={onWheel} ref={canvasRef}
        style={{ backgroundSize: `${GRID_SIZE * transform.zoom}px ${GRID_SIZE * transform.zoom}px`, backgroundPosition: `${transform.x}px ${transform.y}px` }}>
        <div
            className="canvas-stage"
            style={{
                width: worldSize.width,
                height: worldSize.height,
                transform: `translate(${transform.x}px, ${transform.y}px) scale(${transform.zoom})`
            }}>
            <svg
                aria-hidden="true"
                id="edge-layer"
                style={{ width: worldSize.width, height: worldSize.height }}>
                {routes.map(({ edge, path }) => {
                    const isReconnecting = reconnecting?.edge === edge
                    return <path
                        className={`edge${edge.target === selectedNodeId ? ' active' : ''}${isReconnecting ? ' reconnecting' : ''}`}
                        d={isReconnecting ? reconnectPreviewPath(nodes, edge, reconnecting.head) : path}
                        key={`${edge.source}-${edge.target}`}
                        onPointerDown={(event) => beginReconnect(edge, event)} />
                })}
                {reconnecting && !reconnecting.edge &&
                    <path
                        className="edge reconnecting"
                        d={reconnectPreviewPath(nodes, { source: reconnecting.sourceId, target: '' }, reconnecting.head)}
                        key="connection-preview" />
                }
            </svg>
            {nodes.map((node) => (
                <WorkflowNode
                    connectedEdge={edges.find((edge) => edge.target === node.id && nodes.some((n) => n.id === edge.source))}
                    documentationAvailable={toolFunctions.some((tool) => tool.name === `${node.toolId}.${node.functionName}`)}
                    isReconnectTarget={reconnecting?.targetId === node.id}
                    key={node.id}
                    node={node}
                    presentation={presentations.forNode(node)}
                    runState={nodeRunStates[node.id] ?? 'idle'}
                    selected={node.id === selectedNodeId}
                    zoom={transform.zoom}
                    onDrag={updatePosition}
                    onConnectStart={beginConnection}
                    onCopy={handleCopyNode}
                    onDuplicate={handleDuplicateNode}
                    onDelete={handleDeleteNode}
                    onDocumentationOpen={() => setDocumentationOpen(true)}
                    onFavorite={handleFavoriteNode}
                    onInteractionEnd={releasePointer}
                    onInteractionStart={claimPointer}
                    onReconnectStart={beginReconnect}
                    outgoingEdge={edges.find((edge) => edge.source === node.id && nodes.some((n) => n.id === edge.target))}
                    onSelect={onSelect}
                />
            ))}
        </div>
        {overlayHost && createPortal(canvasWidgets, overlayHost)}
    </div>
}

function WorkflowLayoutMap({ nodes, edges, focusedNodeId, onFocusChange, onSelect, toolFunctions }: { nodes: WorkflowNode[]; edges: WorkflowEdge[]; focusedNodeId: string | null; onFocusChange: (nodeId: string | null) => void; onSelect: (nodeId: string) => void; toolFunctions: McpToolFunction[] }) {
    const gridRef = useRef<HTMLDivElement>(null)
    const hoverResetTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
    const [layoutPorts, setLayoutPorts] = useState<Record<string, LayoutPoint>>({})
    const [layoutHeight, setLayoutHeight] = useState(0)
    const reachableNodeIds = reachableWorkflowNodeIds(nodes, edges)
    const connectedNodeIds = new Set(edges.flatMap((edge) => [edge.source, edge.target]))

    const clearHoverResetTimer = useCallback(() => {
        if (hoverResetTimer.current !== null) {
            clearTimeout(hoverResetTimer.current)
            hoverResetTimer.current = null
        }
    }, [])

    useEffect(() => () => {
        if (hoverResetTimer.current !== null) {
            clearTimeout(hoverResetTimer.current)
        }
    }, [])

    const handleFocus = useCallback((nodeId: string) => {
        clearHoverResetTimer()
        onFocusChange(nodeId)
    }, [clearHoverResetTimer, onFocusChange])

    const handlePointerLeave = useCallback((event: React.PointerEvent<HTMLDivElement>) => {
        if (!event.currentTarget.contains(document.activeElement)) {
            clearHoverResetTimer()
            hoverResetTimer.current = setTimeout(() => {
                hoverResetTimer.current = null
                onFocusChange(null)
            }, LAYOUT_FOCUS_RESET_DELAY_MS)
        }
    }, [clearHoverResetTimer, onFocusChange])

    const handleBlur = useCallback(() => {
        clearHoverResetTimer()
        onFocusChange(null)
    }, [clearHoverResetTimer, onFocusChange])

    useLayoutEffect(() => {
        const grid = gridRef.current
        if (!grid) return undefined
        const measure = () => {
            const gridBounds = grid.getBoundingClientRect()
            const ports = Object.fromEntries(Array.from(grid.querySelectorAll<HTMLElement>('[data-layout-node-id]')).flatMap((item) => {
                const status = item.querySelector<HTMLElement>('.workflow-layout-status')
                const nodeId = item.dataset.layoutNodeId
                if (!status || !nodeId) return []
                const bounds = status.getBoundingClientRect()
                return [[nodeId, { x: bounds.left - gridBounds.left + bounds.width / 2, y: bounds.top - gridBounds.top + bounds.height / 2 }]]
            }))
            setLayoutPorts(ports)
            setLayoutHeight(grid.scrollHeight)
        }
        measure()
        const observer = new ResizeObserver(measure)
        observer.observe(grid)
        return () => observer.disconnect()
    }, [nodes])
    const trafficRoads = useMemo(() => {
        if (!focusedNodeId || !layoutPorts[focusedNodeId]) return []
        const farthestPort = Math.max(...Object.values(layoutPorts).map((port) => port.x))
        const laneX = { incoming: farthestPort + 10, outgoing: farthestPort + 20 }
        const focusedPort = layoutPorts[focusedNodeId]
        const incoming = edges.filter((edge) => edge.target === focusedNodeId).flatMap((edge) => {
            const source = layoutPorts[edge.source]
            return source ? [{ id: `in-${edge.source}-${edge.target}`, kind: 'incoming' as const, path: layoutRoadPath(source, focusedPort, laneX.incoming) }] : []
        })
        const outgoing = edges.filter((edge) => edge.source === focusedNodeId).flatMap((edge) => {
            const target = layoutPorts[edge.target]
            return target ? [{ id: `out-${edge.source}-${edge.target}`, kind: 'outgoing' as const, path: layoutRoadPath(focusedPort, target, laneX.outgoing) }] : []
        })
        return [...incoming, ...outgoing]
    }, [edges, focusedNodeId, layoutPorts])
    return <nav aria-label="Workflow layout map" className="workflow-layout-map" onPointerDown={(event) => event.stopPropagation()} onWheel={(event) => event.stopPropagation()}>
        <div className="workflow-layout-grid" ref={gridRef}>
            {trafficRoads.length > 0 &&
                <svg aria-hidden="true" className="workflow-layout-roads" height={layoutHeight} width="160">
                    <defs>
                        <marker id="workflow-layout-incoming-start-arrow" markerHeight="4" markerWidth="4" orient="auto" refX="1" refY="3" viewBox="0 0 6 6">
                            <path d="M 1 0.7 L 4.98 3 L 1 5.3 Z" fill="var(--success)" stroke="var(--success)" strokeLinejoin="round" strokeWidth="0.8" />
                        </marker>
                        <marker id="workflow-layout-outgoing-arrow" markerHeight="4" markerWidth="4" orient="auto" refX="4.98" refY="3" viewBox="0 0 6 6">
                            <path d="M 1 0.7 L 4.98 3 L 1 5.3 Z" fill="var(--accent)" stroke="var(--accent)" strokeLinejoin="round" strokeWidth="0.8" />
                        </marker>
                    </defs>
                    {trafficRoads.map((road) => (
                        <path className={`workflow-layout-road ${road.kind}`} d={road.path} key={road.id} markerEnd={road.kind === 'outgoing' ? 'url(#workflow-layout-outgoing-arrow)' : undefined} markerStart={road.kind === 'incoming' ? 'url(#workflow-layout-incoming-start-arrow)' : undefined} />
                    ))}
                </svg>
            }
            {nodes.map((node) => {
                const inbound = edges.filter((edge) => edge.target === node.id)
                const outbound = edges.filter((edge) => edge.source === node.id)
                const focused = focusedNodeId === node.id
                const redundant = !connectedNodeIds.has(node.id) || !reachableNodeIds.has(node.id)
                const unavailable = node.kind === 'tool' && !toolFunctions.some((tool) => tool.name === `${node.toolId}.${node.functionName}`)
                const status = unavailable ? 'critical' : redundant ? 'warning' : undefined
                const toolName = node.kind === 'trigger' ? node.title : `${node.toolId} / ${node.functionName}`
                return <div className={`workflow-layout-item${focused ? ' focused' : ''}${redundant ? ' redundant' : ''}`} data-layout-node-id={node.id} key={node.id} onPointerEnter={() => handleFocus(node.id)} onPointerLeave={handlePointerLeave}>
                    <span aria-hidden="true" className={`workflow-layout-level${status ? ` ${status}` : ''}`}>{status === 'critical' ? <CriticalIcon fill="var(--danger)" iconColor="var(--card)" size={16} /> : status === 'warning' ? <WarningIcon size={16} /> : null}</span>
                    <button aria-label={`Show connections for ${node.id}${redundant ? ', redundant' : ''}${unavailable ? ', tool function is not available' : ''}`} className="workflow-layout-cell" onBlur={handleBlur} onClick={() => {
                        clearHoverResetTimer()
                        onSelect(node.id)
                    }} onFocus={() => handleFocus(node.id)} type="button">
                        <span aria-hidden="true" className="workflow-layout-status" />
                        <code>{node.id}</code>
                    </button>
                    <div className="workflow-layout-popout">
                        <strong>{toolName}</strong>
                        {inbound.map((edge, index) => <span aria-label={`Incoming route from ${edge.source}`} className="workflow-layout-traffic incoming" key={`in-${edge.source}-${index}`}><code>{edge.source}</code><span aria-hidden="true">→</span><code>{node.id}</code></span>)}
                        {outbound.map((edge, index) => <span aria-label={`Outgoing route to ${edge.target}`} className="workflow-layout-traffic outgoing" key={`out-${edge.target}-${index}`}><code>{node.id}</code><span aria-hidden="true">→</span><code>{edge.target}</code></span>)}
                        {!inbound.length && !outbound.length && <span className="workflow-layout-empty">Unconnected node</span>}
                    </div>
                </div>
            })}
        </div>
    </nav>
}

const WorkflowNode = memo(function WorkflowNode({ node, presentation, selected, runState, zoom, connectedEdge, outgoingEdge, isReconnectTarget, documentationAvailable, onSelect, onDrag, onReconnectStart, onConnectStart, onDocumentationOpen, onInteractionStart, onInteractionEnd, onCopy, onDuplicate: _onDuplicate, onFavorite, onDelete }: {
    node: WorkflowNode;
    presentation: NodeIconDescriptor;
    selected: boolean;
    runState: NodeRunState;
    zoom: number;
    connectedEdge?: WorkflowEdge;
    outgoingEdge?: WorkflowEdge;
    isReconnectTarget: boolean;
    documentationAvailable: boolean;
    onSelect: (id: string) => void;
    onDrag: (id: string, x: number, y: number) => void
    onReconnectStart: (edge: WorkflowEdge, event: React.PointerEvent) => void
    onConnectStart: (sourceId: string, event: React.PointerEvent<HTMLElement>) => void
    onDocumentationOpen: () => void
    onInteractionStart: (event: React.PointerEvent<HTMLElement>) => boolean
    onInteractionEnd: (pointerId: number) => void
    onCopy: (node: WorkflowNode) => void
    onDuplicate: (node: WorkflowNode) => void
    onFavorite: (node: WorkflowNode) => void
    onDelete: (node: WorkflowNode) => void
}) {
    const drag = useRef<{ pointerId: number; startX: number; startY: number; nodeX: number; nodeY: number } | null>(null)
    const primary = Object.entries(node.arguments)[0]
    const value = primary?.[1] ?? ''
    const compact = value.length > 24 ? primary?.[0] : value
    const onPointerDown = (event: React.PointerEvent<HTMLDivElement>) => {
        if (event.button !== 0 || !onInteractionStart(event)) return
        event.stopPropagation();
        drag.current = { pointerId: event.pointerId, startX: event.clientX, startY: event.clientY, nodeX: node.x, nodeY: node.y };
        try {
            event.currentTarget.setPointerCapture(event.pointerId)
        } catch {
            drag.current = null
            onInteractionEnd(event.pointerId)
        }
    }
    const onPointerMove = (event: React.PointerEvent<HTMLDivElement>) => {
        if (drag.current?.pointerId === event.pointerId) onDrag(node.id, drag.current.nodeX + (event.clientX - drag.current.startX) / zoom, drag.current.nodeY + (event.clientY - drag.current.startY) / zoom)
    }
    const endDrag = (event: React.PointerEvent<HTMLDivElement>) => {
        if (drag.current?.pointerId === event.pointerId) {
            drag.current = null;
            if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId)
            onInteractionEnd(event.pointerId)
        }
    }
    const cancelDrag = (event: React.PointerEvent<HTMLDivElement>) => {
        if (drag.current?.pointerId === event.pointerId) {
            drag.current = null
            onInteractionEnd(event.pointerId)
        }
    }
    return <article className={`node${selected ? ' selected' : ''}${runState === 'idle' ? '' : ` ${runState}`}${isReconnectTarget ? ' connection-target' : ''}`} data-node-id={node.id} onClick={() => onSelect(node.id)} tabIndex={0}
        style={{ left: node.x, top: node.y, width: NODE_WIDTH }}>
        {selected && (
            <div className="node-context-options" onClick={(event) => event.stopPropagation()} onPointerDown={(event) => event.stopPropagation()}>
                <button className="node-context-option" onClick={(event) => { event.stopPropagation(); onCopy(node) }} title="Copy node (Ctrl+C)" type="button">
                    <CopyIcon size={12} />
                    <span>Copy</span>
                </button>
                <button className={`node-context-option${node.isFavorite ? ' is-favorite active' : ''}`} onClick={(event) => { event.stopPropagation(); onFavorite(node) }} title={node.isFavorite ? 'Remove saved tool' : 'Save tool'} type="button">
                    <FavoriteIcon isFavorite={node.isFavorite} size={12} />
                    <span>Saved</span>
                </button>
                <button className="node-context-option is-delete" onClick={(event) => { event.stopPropagation(); onDelete(node) }} title="Delete node (Delete)" type="button">
                    <DeleteIcon size={12} />
                    <span>Delete</span>
                </button>
            </div>
        )}
        <div className="node-head" onLostPointerCapture={cancelDrag} onPointerCancel={endDrag} onPointerDown={onPointerDown} onPointerMove={onPointerMove} onPointerUp={endDrag}>
            <span aria-label={`Input port for ${node.title}`} className={`port in${connectedEdge ? ' connected' : ''}${isReconnectTarget ? ' connection-target' : ''}`} data-input-port data-node-id={node.id}
                onPointerDown={connectedEdge ? (event) => onReconnectStart(connectedEdge, event) : undefined} title={connectedEdge ? `Drag to reconnect ${node.title}` : `Input port for ${node.title}`} />
            <span aria-label={`Output port for ${node.title}`} className={`port out${!outgoingEdge ? ' empty' : ''}`} onPointerDown={!outgoingEdge ? (event) => onConnectStart(node.id, event) : undefined} title={!outgoingEdge ? `Drag to connect ${node.title}` : `Output port for ${node.title}`} />

            <code className="node-id">{node.id}</code>
            <span className="node-icon" title={`${node.toolId} / ${node.functionName}`}>
                <WorkflowNodeIcon descriptor={presentation} size={28} />
            </span>
            <div className="node-copy">
                <div className="node-title">{node.title}</div>
                <div className="node-kind">{node.toolId} / {node.functionName}</div>
            </div>
        </div>
        <div className="node-body">
            <span className={`node-tag input${primary ? '' : ' muted'}`} title={primary ? `${primary[0]}: ${value}` : undefined}>{primary ? compact : 'No input'}</span>
            <span className="node-tag output" title={`Output: ${node.output}`}>↗ {node.output}</span>
        </div>
        <div className="node-footer">
            {documentationAvailable && <button aria-label={`Open documentation for ${node.title}`} className="node-documentation-button" onClick={(event) => {
                event.stopPropagation()
                onSelect(node.id)
                onDocumentationOpen()
            }} title="Open tool reference" type="button"><CubeIcon size={15} /></button>}
        </div>
    </article>
})

type ToolSchema = Record<string, unknown>

function SelectedNodeData({ node, tool }: { node?: WorkflowNode; tool?: McpToolFunction }) {
    if (!node) return null
    const availability = tool?.annotations?.availability
    const returnType = tool?.returnType ?? tool?.annotations?.returnType ?? tool?.outputSchema
    const structuredOutput = tool ? hasStructuredOutput(tool) : undefined
    const details = [
        ...Object.entries(node.annotations).filter(([, value]) => value !== undefined).map(([key, value]) => [formatDataKey(key), formatDataValue(value)] as const),
        ...(availability === undefined ? [] : [['Availability', formatDataValue(availability)] as const]),
        ...(structuredOutput === undefined ? [] : [['Structured output', structuredOutput ? (tool?.outputSchema ? 'Declared schema' : 'Observed schema policy') : 'Disabled'] as const]),
        ...(returnType === undefined ? [] : [['Return type', formatReturnType(returnType)] as const]),
    ]
    return <aside aria-label={`Selected node data for ${node.title}`} className="selected-node-data" onPointerDown={(event) => event.stopPropagation()} onWheel={(event) => event.stopPropagation()}>
        <header className="selected-node-data-head">
            <div className="selected-node-data-head-top">
                <code>{node.id}</code>
                <strong>{node.title}</strong>
            </div>
        </header>
        <div className="selected-node-data-details">
            <div className="selected-node-data-toolname">
                <PathIcon size={15} />
                <span>{node.toolId}.{node.functionName}</span>
            </div>
            <dl className="selected-node-data-list">
                {details.map(([label, value]) => <div key={label}><dt>{label}</dt><dd title={value}>{value}</dd></div>)}
            </dl>
        </div>
    </aside>
}

function formatDataKey(value: string) {
    const label = value.replace(/([a-z0-9])([A-Z])/g, '$1 $2').replace(/[-_]/g, ' ').trim()
    return label ? label.charAt(0).toUpperCase() + label.slice(1) : 'Value'
}

function formatDataValue(value: unknown): string {
    if (typeof value === 'boolean') return value ? 'Enabled' : 'Disabled'
    if (typeof value === 'number') return value.toLocaleString()
    if (typeof value === 'string') return value
    if (Array.isArray(value)) return value.map(formatDataValue).join(', ')
    if (isRecord(value)) return Object.entries(value).map(([key, item]) => `${formatDataKey(key)}: ${formatDataValue(item)}`).join(' · ')
    return value === null ? 'None' : String(value)
}

function formatReturnType(value: unknown) {
    if (isRecord(value) && typeof value.type === 'string') {
        return typeof value.format === 'string' ? `${value.type} (${value.format})` : value.type
    }
    return formatDataValue(value)
}

function ToolReference({ tool, onClose }: { tool?: McpToolFunction; onClose: () => void }) {
    if (!tool) return null
    const schema = tool.inputSchema
    const properties = schema && isRecord(schema.properties) ? Object.entries(schema.properties) : []
    const required = schema && Array.isArray(schema.required) ? new Set(schema.required.filter((value): value is string => typeof value === 'string')) : new Set<string>()
    const outputSchema = tool.outputSchema
    const outputDataSchema = outputSchema && isRecord(outputSchema.properties) && isRecord(outputSchema.properties.data)
        ? outputSchema.properties.data
        : undefined
    const outputProperties = outputDataSchema && isRecord(outputDataSchema.properties) ? Object.entries(outputDataSchema.properties) : []
    const outputRequired = outputDataSchema && Array.isArray(outputDataSchema.required)
        ? new Set(outputDataSchema.required.filter((value): value is string => typeof value === 'string'))
        : new Set<string>()
    const structuredOutput = hasStructuredOutput(tool)
    const scopes = tool.annotations?.scopes ?? []
    return <aside aria-label={`Tool reference for ${tool.name}`} className="tool-reference" onPointerDown={(event) => event.stopPropagation()} onWheel={(event) => event.stopPropagation()}>
        <header className="tool-reference-bar">
            <div className="tool-reference-heading"><CubeFilledIcon size={17} /><span>Tool reference</span></div>
            <button aria-label="Close tool reference" className="tool-reference-close" onClick={onClose} title="Close" type="button"><Close size={16} /></button>
        </header>
        <div className="tool-reference-body">
            <div className="tool-reference-identity">
                <h2>{tool.title?.trim() || tool.name}</h2>
                <code>{tool.name}</code>
            </div>
            {tool.description && <p className="tool-reference-description">{tool.description}</p>}
            <section className="tool-reference-section">
                <h3>Inputs</h3>
                {properties.length ? <div className="tool-reference-inputs">{properties.map(([name, value]) => <ToolInput key={name} name={name} required={required.has(name)} schema={isRecord(value) ? value : {}} />)}</div>
                    : <p className="tool-reference-empty">This tool does not accept input arguments.</p>}
                {schema?.additionalProperties === false && <p className="tool-reference-contract">Only the documented arguments are accepted.</p>}
            </section>
            {structuredOutput && <section className="tool-reference-section">
                <h3>Output</h3>
                {outputProperties.length ? <div className="tool-reference-inputs">{outputProperties.map(([name, value]) => <ToolInput key={name} name={name} required={outputRequired.has(name)} schema={isRecord(value) ? value : {}} />)}</div>
                    : outputSchema ? <p className="tool-reference-empty">The tool declares a structured output schema.</p>
                        : <p className="tool-reference-empty">Studio will establish an observed output schema from a valid result.</p>}
            </section>}
            {(scopes.length > 0 || tool.annotations?.timeoutMs !== undefined) && <section className="tool-reference-section">
                <h3>Execution policy</h3>
                {scopes.length > 0 && <div className="tool-reference-scopes">{scopes.map((scope) => <code key={scope}>{scope}</code>)}</div>}
                {tool.annotations?.timeoutMs !== undefined && <p className="tool-reference-timeout"><span>Timeout</span><strong>{tool.annotations.timeoutMs.toLocaleString()} ms</strong></p>}
            </section>}
        </div>
    </aside>
}

function ToolInput({ name, required, schema }: { name: string; required: boolean; schema: ToolSchema }) {
    const type = typeof schema.type === 'string' ? schema.type : 'value'
    const itemType = isRecord(schema.items) && typeof schema.items.type === 'string' ? ` of ${schema.items.type}` : ''
    const description = typeof schema.description === 'string' ? schema.description : undefined
    return <article className="tool-reference-input">
        <div className="tool-reference-input-head"><code>{name}</code><span>{type}{itemType}</span>{required && <b>Required</b>}</div>
        {description && <p>{description}</p>}
    </article>
}

function isRecord(value: unknown): value is ToolSchema {
    return typeof value === 'object' && value !== null && !Array.isArray(value)
}

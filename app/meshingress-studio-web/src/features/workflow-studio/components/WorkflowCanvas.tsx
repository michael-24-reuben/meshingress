import { useMemo, useRef, useState } from 'react'
import { NodeIcon } from '../../../components/icons/node-icons'
import { clamp, snapToGrid } from '../canvas/grid'
import { routeEdges } from '../canvas/router'
import { CANVAS_SIZE, GRID_SIZE, NODE_HEIGHT, NODE_STAGE_INSET, type NodeRunState, type WorkflowEdge, type WorkflowNode } from '../types'

interface WorkflowCanvasProps { nodes: WorkflowNode[]; edges: WorkflowEdge[]; selectedNodeId: string; nodeRunStates: Record<string, NodeRunState>; onSelect: (id: string) => void; onNodesChange: (change: (nodes: WorkflowNode[]) => WorkflowNode[]) => void }

export function WorkflowCanvas({ nodes, edges, selectedNodeId, nodeRunStates, onSelect, onNodesChange }: WorkflowCanvasProps) {
  const [transform, setTransform] = useState({ x: 36, y: 24, zoom: 0.86 })
  const pan = useRef<{ pointerId: number; startX: number; startY: number; originX: number; originY: number } | null>(null)
  const routes = useMemo(() => routeEdges(nodes, edges, CANVAS_SIZE.width, CANVAS_SIZE.height), [nodes, edges])
  const updatePosition = (id: string, rawX: number, rawY: number) => onNodesChange((current) => {
    const node = current.find((candidate) => candidate.id === id)
    if (!node) return current
    const x = snapToGrid(clamp(rawX, NODE_STAGE_INSET, CANVAS_SIZE.width - node.width - NODE_STAGE_INSET))
    const y = snapToGrid(clamp(rawY, NODE_STAGE_INSET, CANVAS_SIZE.height - NODE_HEIGHT - NODE_STAGE_INSET))
    const collides = current.some((other) => other.id !== id && x - NODE_STAGE_INSET < other.x + other.width && x + node.width + NODE_STAGE_INSET > other.x && y - NODE_STAGE_INSET < other.y + NODE_HEIGHT && y + NODE_HEIGHT + NODE_STAGE_INSET > other.y)
    if (collides) return current
    return current.map((candidate) => candidate.id === id ? { ...candidate, x, y } : candidate)
  })
  const onPointerDown = (event: React.PointerEvent<HTMLDivElement>) => {
    if (event.button !== 0 || (event.target as HTMLElement).closest('.node')) return
    pan.current = { pointerId: event.pointerId, startX: event.clientX, startY: event.clientY, originX: transform.x, originY: transform.y }
    event.currentTarget.setPointerCapture(event.pointerId)
  }
  const onPointerMove = (event: React.PointerEvent<HTMLDivElement>) => { if (pan.current?.pointerId === event.pointerId) setTransform((current) => ({ ...current, x: pan.current!.originX + event.clientX - pan.current!.startX, y: pan.current!.originY + event.clientY - pan.current!.startY })) }
  const endPan = (event: React.PointerEvent<HTMLDivElement>) => { if (pan.current?.pointerId === event.pointerId) { pan.current = null; event.currentTarget.releasePointerCapture(event.pointerId) } }
  const onWheel = (event: React.WheelEvent<HTMLDivElement>) => { event.preventDefault(); const bounds = event.currentTarget.getBoundingClientRect(); const pointerX = event.clientX - bounds.left; const pointerY = event.clientY - bounds.top; setTransform((current) => { const worldX = (pointerX - current.x) / current.zoom; const worldY = (pointerY - current.y) / current.zoom; const zoom = clamp(current.zoom * (event.deltaY < 0 ? 1.1 : 0.9), 0.35, 1.9); return { zoom, x: pointerX - worldX * zoom, y: pointerY - worldY * zoom } }) }
  return <div aria-label="Workflow canvas. Drag empty space to pan and use the mouse wheel to zoom." className={`canvas${pan.current ? ' panning' : ''}`} onPointerCancel={endPan} onPointerDown={onPointerDown} onPointerMove={onPointerMove} onPointerUp={endPan} onWheel={onWheel} tabIndex={0} style={{ backgroundSize: `${GRID_SIZE * transform.zoom}px ${GRID_SIZE * transform.zoom}px`, backgroundPosition: `${transform.x}px ${transform.y}px` }}><div className="canvas-stage" style={{ transform: `translate(${transform.x}px, ${transform.y}px) scale(${transform.zoom})` }}><svg aria-hidden="true" id="edge-layer">{routes.map(({ edge, path }) => <path className={`edge${edge.target === selectedNodeId ? ' active' : ''}`} d={path} key={`${edge.source}-${edge.target}`} />)}</svg>{nodes.map((node) => <WorkflowNode key={node.id} node={node} runState={nodeRunStates[node.id] ?? 'idle'} selected={node.id === selectedNodeId} zoom={transform.zoom} onDrag={updatePosition} onSelect={onSelect} />)}</div></div>
}

function WorkflowNode({ node, selected, runState, zoom, onSelect, onDrag }: { node: WorkflowNode; selected: boolean; runState: NodeRunState; zoom: number; onSelect: (id: string) => void; onDrag: (id: string, x: number, y: number) => void }) {
  const drag = useRef<{ pointerId: number; startX: number; startY: number; nodeX: number; nodeY: number } | null>(null)
  const primary = Object.entries(node.arguments)[0]
  const value = primary?.[1] ?? ''
  const compact = value.length > 24 ? primary?.[0] : value
  const onPointerDown = (event: React.PointerEvent<HTMLDivElement>) => { event.stopPropagation(); drag.current = { pointerId: event.pointerId, startX: event.clientX, startY: event.clientY, nodeX: node.x, nodeY: node.y }; event.currentTarget.setPointerCapture(event.pointerId) }
  const onPointerMove = (event: React.PointerEvent<HTMLDivElement>) => { if (drag.current?.pointerId === event.pointerId) onDrag(node.id, drag.current.nodeX + (event.clientX - drag.current.startX) / zoom, drag.current.nodeY + (event.clientY - drag.current.startY) / zoom) }
  const endDrag = (event: React.PointerEvent<HTMLDivElement>) => { if (drag.current?.pointerId === event.pointerId) { drag.current = null; event.currentTarget.releasePointerCapture(event.pointerId) } }
  return <article className={`node${selected ? ' selected' : ''}${runState === 'idle' ? '' : ` ${runState}`}`} data-node-id={node.id} onClick={() => onSelect(node.id)} style={{ left: node.x, top: node.y, width: node.width }}><div className="node-head" onPointerCancel={endDrag} onPointerDown={onPointerDown} onPointerMove={onPointerMove} onPointerUp={endDrag}><span className="port in" /><span className="port out" /><span className="node-icon" title={`${node.toolId} / ${node.functionName}`}><NodeIcon nodeKind={node.kind} toolId={node.toolId} /></span><div className="node-copy"><div className="node-title">{node.title}</div><div className="node-kind">{node.toolId} / {node.functionName}</div></div></div><div className="node-body"><span className={`node-tag input${primary ? '' : ' muted'}`} title={primary ? `${primary[0]}: ${value}` : undefined}>{primary ? compact : 'No input'}</span><span className="node-tag output" title={`Output: ${node.output}`}>↗ {node.output}</span></div><div className="node-footer"><code className="node-id">{node.id}</code></div></article>
}

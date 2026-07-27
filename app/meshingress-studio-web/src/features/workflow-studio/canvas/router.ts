import { GRID_SIZE, NODE_HEIGHT, type WorkflowEdge, type WorkflowNode } from '../types'

type Point = { x: number; y: number }
type Box = { id: string; left: number; top: number; right: number; bottom: number }
type Segment = { from: Point; to: Point }

const CLEARANCE = GRID_SIZE
const TURN_COST = GRID_SIZE * 2
const CROSSING_COST = GRID_SIZE

function inside(point: Point, box: Box): boolean {
  return point.x > box.left && point.x < box.right && point.y > box.top && point.y < box.bottom
}

function segmentBlocked(from: Point, to: Point, boxes: Box[]): boolean {
  return boxes.some((box) => {
    if (from.x === to.x) return from.x > box.left && from.x < box.right && Math.max(from.y, to.y) > box.top && Math.min(from.y, to.y) < box.bottom
    return from.y > box.top && from.y < box.bottom && Math.max(from.x, to.x) > box.left && Math.min(from.x, to.x) < box.right
  })
}

function crossingPenalty(from: Point, to: Point, reserved: Segment[]): number {
  return reserved.reduce((total, segment) => {
    const horizontal = from.y === to.y
    const otherHorizontal = segment.from.y === segment.to.y
    if (horizontal === otherHorizontal) return total
    const horizontalSegment = horizontal ? { from, to } : segment
    const verticalSegment = horizontal ? segment : { from, to }
    const crosses = verticalSegment.from.x >= Math.min(horizontalSegment.from.x, horizontalSegment.to.x)
      && verticalSegment.from.x <= Math.max(horizontalSegment.from.x, horizontalSegment.to.x)
      && horizontalSegment.from.y >= Math.min(verticalSegment.from.y, verticalSegment.to.y)
      && horizontalSegment.from.y <= Math.max(verticalSegment.from.y, verticalSegment.to.y)
    return total + (crosses ? CROSSING_COST : 0)
  }, 0)
}

function compact(points: Point[]): Point[] {
  return points.filter((point, index, all) => {
    if (index === 0 || index === all.length - 1) return true
    const previous = all[index - 1]
    const next = all[index + 1]
    return !((previous.x === point.x && point.x === next.x) || (previous.y === point.y && point.y === next.y))
  })
}

function findRoute(start: Point, end: Point, boxes: Box[], width: number, height: number, reserved: Segment[]): Point[] | null {
  const keyOf = (point: Point, direction: string) => `${point.x},${point.y}:${direction}`
  const pending: Array<{ point: Point; direction: string; score: number }> = [{ point: start, direction: 'start', score: 0 }]
  const scores = new Map([[keyOf(start, 'start'), 0]])
  const previous = new Map<string, string>()

  while (pending.length) {
    pending.sort((a, b) => a.score - b.score)
    const current = pending.shift()
    if (!current || current.score !== scores.get(keyOf(current.point, current.direction))) continue
    if (current.point.x === end.x && current.point.y === end.y) {
      const route: Point[] = []
      let key = keyOf(current.point, current.direction)
      while (key) {
        const [coordinate] = key.split(':')
        const [x, y] = coordinate.split(',').map(Number)
        route.unshift({ x, y })
        key = previous.get(key) ?? ''
      }
      return route
    }
    const candidates = [
      { point: { x: current.point.x + GRID_SIZE, y: current.point.y }, direction: 'horizontal' },
      { point: { x: current.point.x - GRID_SIZE, y: current.point.y }, direction: 'horizontal' },
      { point: { x: current.point.x, y: current.point.y + GRID_SIZE }, direction: 'vertical' },
      { point: { x: current.point.x, y: current.point.y - GRID_SIZE }, direction: 'vertical' },
    ] as const
    candidates.forEach((next) => {
      if (next.point.x < GRID_SIZE || next.point.x > width - GRID_SIZE || next.point.y < GRID_SIZE || next.point.y > height - GRID_SIZE) return
      if (boxes.some((box) => inside(next.point, box)) || segmentBlocked(current.point, next.point, boxes)) return
      const score = current.score + GRID_SIZE + (current.direction !== 'start' && current.direction !== next.direction ? TURN_COST : 0) + crossingPenalty(current.point, next.point, reserved)
      const key = keyOf(next.point, next.direction)
      if (score >= (scores.get(key) ?? Infinity)) return
      scores.set(key, score)
      previous.set(key, keyOf(current.point, current.direction))
      pending.push({ ...next, score })
    })
  }
  return null
}

export function roundedPath(points: Point[], radius = 10): string {
  if (points.length < 2) return ''
  let path = `M ${points[0].x} ${points[0].y}`
  for (let index = 1; index < points.length - 1; index += 1) {
    const previous = points[index - 1]
    const current = points[index]
    const next = points[index + 1]
    const firstLength = Math.hypot(current.x - previous.x, current.y - previous.y)
    const secondLength = Math.hypot(next.x - current.x, next.y - current.y)
    const r = Math.min(radius, firstLength * 0.45, secondLength * 0.45)
    const before = { x: current.x - Math.sign(current.x - previous.x) * r, y: current.y - Math.sign(current.y - previous.y) * r }
    const after = { x: current.x + Math.sign(next.x - current.x) * r, y: current.y + Math.sign(next.y - current.y) * r }
    path += ` L ${before.x} ${before.y} Q ${current.x} ${current.y}, ${after.x} ${after.y}`
  }
  const last = points.at(-1)!
  return `${path} L ${last.x} ${last.y}`
}

export function routeEdges(nodes: WorkflowNode[], edges: WorkflowEdge[], width: number, height: number): Array<{ edge: WorkflowEdge; path: string }> {
  const boxes = nodes.map((node) => ({ id: node.id, left: node.x, top: node.y, right: node.x + node.width, bottom: node.y + NODE_HEIGHT }))
  const padded = boxes.map((box) => ({ ...box, left: box.left - CLEARANCE, top: box.top - CLEARANCE, right: box.right + CLEARANCE, bottom: box.bottom + CLEARANCE }))
  const reserved: Segment[] = []
  return edges.flatMap((edge) => {
    const source = nodes.find((node) => node.id === edge.source)
    const target = nodes.find((node) => node.id === edge.target)
    if (!source || !target) return []
    const sourcePort = { x: source.x + source.width, y: source.y + GRID_SIZE }
    const targetPort = { x: target.x, y: target.y + GRID_SIZE }
    const start = { x: sourcePort.x + CLEARANCE, y: sourcePort.y }
    const end = { x: targetPort.x - CLEARANCE, y: targetPort.y }
    const route = findRoute(start, end, padded, width, height, reserved)
    if (!route) return []
    const points = compact([sourcePort, ...route, targetPort])
    for (let index = 1; index < points.length; index += 1) reserved.push({ from: points[index - 1], to: points[index] })
    return [{ edge, path: roundedPath(points) }]
  })
}

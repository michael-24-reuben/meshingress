import {
  flatTopHexPortCorners,
  flatTopHexPosition,
  flatTopHexCornerPortPoint,
  flatTopHexInnerTurnPortAllocation,
  flatTopHexJunctionCoordinate,
  flatTopHexSlotId,
  flatTopHexSlotPortIdentity,
  type HoneycombPoint,
} from '../honeycomb-grid'
import type { HoneycombRoutePlan, HoneycombRoutePortHook, HoneycombRouteRequest, HoneycombRouteVariant, PortTrafficRole } from './honeycomb-route-variant'

interface TraversalGraphEdge {
  cost: number
  to: string
}

interface TraversalPortState {
  portId: string
  role: PortTrafficRole
}

function distanceBetween(first: HoneycombPoint, second: HoneycombPoint): number {
  return Math.hypot(second.x - first.x, second.y - first.y)
}

function distanceToSegment(point: HoneycombPoint, segmentStart: HoneycombPoint, segmentEnd: HoneycombPoint): number {
  const segmentX = segmentEnd.x - segmentStart.x
  const segmentY = segmentEnd.y - segmentStart.y
  const segmentLengthSquared = segmentX ** 2 + segmentY ** 2
  if (segmentLengthSquared === 0) return distanceBetween(point, segmentStart)
  const projection = Math.max(0, Math.min(1, ((point.x - segmentStart.x) * segmentX + (point.y - segmentStart.y) * segmentY) / segmentLengthSquared))
  return distanceBetween(point, { x: segmentStart.x + segmentX * projection, y: segmentStart.y + segmentY * projection })
}

function portStateId(portId: string, role: PortTrafficRole): string {
  return `${portId}#${role}`
}

function incomingPortStateId(portId: string, incomingPortId: string): string {
  return `${portStateId(portId, 'input')}/from/${incomingPortId}`
}

function addTraversalEdge(graph: Map<string, TraversalGraphEdge[]>, from: string, to: string, cost: number) {
  if (!graph.has(from) || !graph.has(to)) return
  graph.get(from)?.push({ cost, to })
}

function straightAheadPenalty(incomingPort: HoneycombRoutePortHook & { slotCenter: HoneycombPoint }, currentPort: HoneycombRoutePortHook & { slotCenter: HoneycombPoint }, nextPort: HoneycombRoutePortHook & { slotCenter: HoneycombPoint }, slotSide: number): number {
  const incomingAxis = {
    x: incomingPort.slotCenter.x - incomingPort.point.x,
    y: incomingPort.slotCenter.y - incomingPort.point.y,
  }
  const continuation = {
    x: nextPort.point.x - currentPort.point.x,
    y: nextPort.point.y - currentPort.point.y,
  }
  const incomingLength = Math.hypot(incomingAxis.x, incomingAxis.y)
  const continuationLength = Math.hypot(continuation.x, continuation.y)
  if (incomingLength === 0 || continuationLength === 0) return 0
  const alignment = Math.abs((incomingAxis.x * continuation.x + incomingAxis.y * continuation.y) / (incomingLength * continuationLength))
  return (1 - alignment) * slotSide * 4
}

/**
 * The existing graph policy, extracted without changing its selection rules.
 * Its returned hooks preserve the established rendering semantics: the first
 * hook is the output origin and the last hook is the input destination.
 */
export const graphRouteVariant: HoneycombRouteVariant = {
  id: 'graph',
  plan({ assignedRoles, edge, geometry, slots, sourceSlot, targetSlot }: HoneycombRouteRequest): HoneycombRoutePlan | null {
    type TraversalPort = HoneycombRoutePortHook & { slotCenter: HoneycombPoint }

    const graph = new Map<string, TraversalGraphEdge[]>()
    const ports = new Map<string, TraversalPort>()
    const portsByJunction = new Map<string, string[]>()
    const portStates = new Map<string, TraversalPortState>()
    const slotPortIdsBySlotId = new Map<string, string[]>()
    const sourceCenter = flatTopHexPosition(sourceSlot, geometry)
    const targetCenter = flatTopHexPosition(targetSlot, geometry)

    const addPortState = (portId: string, role: PortTrafficRole, stateId = portStateId(portId, role)) => {
      const assignedRole = assignedRoles.get(portId)
      if (assignedRole && assignedRole !== role) return
      graph.set(stateId, [])
      portStates.set(stateId, { portId, role })
    }

    slots.forEach((slot) => {
      const slotPosition = flatTopHexPosition(slot, geometry)
      const slotPortIds = flatTopHexPortCorners.map((corner) => {
        const offset = flatTopHexCornerPortPoint(corner, geometry)
        const identity = flatTopHexSlotPortIdentity(slot, corner)
        const port: TraversalPort = {
          corner,
          id: identity.slotPortId,
          junction: flatTopHexJunctionCoordinate(slot, corner),
          junctionId: identity.junctionId,
          point: { x: slotPosition.x + offset.x, y: slotPosition.y + offset.y },
          slotCenter: slotPosition,
          slotId: identity.slotId,
        }
        ports.set(port.id, port)
        addPortState(port.id, 'output')
        const junctionPorts = portsByJunction.get(port.junctionId) ?? []
        junctionPorts.push(port.id)
        portsByJunction.set(port.junctionId, junctionPorts)
        return port.id
      })
      slotPortIdsBySlotId.set(flatTopHexSlotId(slot), slotPortIds)
    })

    portsByJunction.forEach((portIds) => {
      const sortedPortIds = [...portIds].sort()
      sortedPortIds.forEach((portId, index) => {
        sortedPortIds.slice(index + 1).forEach((otherPortId) => {
          addPortState(otherPortId, 'input', incomingPortStateId(otherPortId, portId))
          addPortState(portId, 'input', incomingPortStateId(portId, otherPortId))
          addTraversalEdge(graph, portStateId(portId, 'output'), incomingPortStateId(otherPortId, portId), 0)
          addTraversalEdge(graph, portStateId(otherPortId, 'output'), incomingPortStateId(portId, otherPortId), 0)
        })
      })
    })

    slots.forEach((slot) => {
      const slotPortIds = slotPortIdsBySlotId.get(flatTopHexSlotId(slot))!
      slotPortIds.forEach((portId, index) => {
        const nextPortId = slotPortIds[(index + 1) % slotPortIds.length]
        const first = ports.get(portId)!
        const second = ports.get(nextPortId)!
        const midpoint = { x: (first.point.x + second.point.x) / 2, y: (first.point.y + second.point.y) / 2 }
        const baseCost = distanceBetween(first.point, second.point) + distanceToSegment(midpoint, sourceCenter, targetCenter) * 0.05
        const firstIncomingPortIds = portsByJunction.get(first.junctionId)!.filter((incomingPortId) => incomingPortId !== first.id)
        firstIncomingPortIds.forEach((incomingPortId) => {
          const incomingPort = ports.get(incomingPortId)!
          addTraversalEdge(graph, incomingPortStateId(first.id, incomingPortId), portStateId(second.id, 'output'), baseCost + straightAheadPenalty(incomingPort, first, second, geometry.slotSide))
        })
        const secondIncomingPortIds = portsByJunction.get(second.junctionId)!.filter((incomingPortId) => incomingPortId !== second.id)
        secondIncomingPortIds.forEach((incomingPortId) => {
          const incomingPort = ports.get(incomingPortId)!
          addTraversalEdge(graph, incomingPortStateId(second.id, incomingPortId), portStateId(first.id, 'output'), baseCost + straightAheadPenalty(incomingPort, second, first, geometry.slotSide))
        })
      })
    })

    const sourcePortStateIds = flatTopHexPortCorners
      .map((corner) => portStateId(flatTopHexSlotPortIdentity(sourceSlot, corner).slotPortId, 'output'))
      .filter((stateId) => graph.has(stateId))
    const targetPortStateIds = new Set(flatTopHexPortCorners
      .flatMap((corner) => {
        const targetPortId = flatTopHexSlotPortIdentity(targetSlot, corner).slotPortId
        const targetPort = ports.get(targetPortId)
        if (!targetPort) return []
        return portsByJunction.get(targetPort.junctionId)!
          .filter((incomingPortId) => incomingPortId !== targetPortId)
          .map((incomingPortId) => incomingPortStateId(targetPortId, incomingPortId))
      })
      .filter((stateId) => graph.has(stateId)))
    const distances = new Map(sourcePortStateIds.map((stateId) => [stateId, 0]))
    const predecessors = new Map<string, string>()
    const routeKeys = new Map(sourcePortStateIds.map((stateId) => [stateId, stateId]))
    const pending = [...sourcePortStateIds]
    const settled = new Set<string>()
    let destinationPortStateId: string | null = null

    while (pending.length > 0) {
      pending.sort((first, second) => (distances.get(first)! - distances.get(second)!) || routeKeys.get(first)!.localeCompare(routeKeys.get(second)!))
      const currentPortStateId = pending.shift()!
      if (settled.has(currentPortStateId)) continue
      settled.add(currentPortStateId)
      if (targetPortStateIds.has(currentPortStateId)) {
        destinationPortStateId = currentPortStateId
        break
      }

      graph.get(currentPortStateId)!.sort((first, second) => first.to.localeCompare(second.to)).forEach((graphEdge) => {
        if (settled.has(graphEdge.to)) return
        const candidateDistance = distances.get(currentPortStateId)! + graphEdge.cost
        const candidateKey = `${routeKeys.get(currentPortStateId)!}>${graphEdge.to}`
        const knownDistance = distances.get(graphEdge.to)
        const knownKey = routeKeys.get(graphEdge.to)
        if (knownDistance === undefined || candidateDistance < knownDistance - 0.000001 || (Math.abs(candidateDistance - knownDistance) < 0.000001 && candidateKey < knownKey!)) {
          distances.set(graphEdge.to, candidateDistance)
          predecessors.set(graphEdge.to, currentPortStateId)
          routeKeys.set(graphEdge.to, candidateKey)
          pending.push(graphEdge.to)
        }
      })
    }

    if (!destinationPortStateId) return null
    const routePortStateIds: string[] = []
    for (let currentPortStateId: string | undefined = destinationPortStateId; currentPortStateId; currentPortStateId = predecessors.get(currentPortStateId)) {
      routePortStateIds.unshift(currentPortStateId)
    }
    const routePortStates = routePortStateIds.map((stateId) => portStates.get(stateId)!)
    const routePorts = routePortStates.map((state) => ports.get(state.portId)!)
    const junctionSequence = routePorts.reduce<TraversalPort[]>((result, port) => {
      if (result.at(-1)?.junctionId !== port.junctionId) result.push(port)
      return result
    }, [])
    const turnAllocations = junctionSequence.flatMap((port, index) => {
      if (index === 0 || index === junctionSequence.length - 1) return []
      const allocation = flatTopHexInnerTurnPortAllocation(junctionSequence[index - 1].junction, port.junction, junctionSequence[index + 1].junction, slots)
      return allocation ? [allocation] : []
    })
    const sourcePortId = routePortStates[0].portId
    const targetPortId = routePortStates.at(-1)!.portId
    const hookPorts = [
      ports.get(sourcePortId)!,
      ...turnAllocations.map((allocation) => ports.get(allocation.slotPortId)!),
      ports.get(targetPortId)!,
    ]

    return {
      id: `${edge.source}->${edge.target}`,
      ports: hookPorts,
      portStates: [
        { portId: sourcePortId, role: 'output' },
        { portId: targetPortId, role: 'input' },
      ],
      turnPortIds: turnAllocations.map((allocation) => allocation.slotPortId),
      turnSlotIds: turnAllocations.map((allocation) => allocation.slotId),
    }
  },
}

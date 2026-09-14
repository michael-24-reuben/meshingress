export interface AxialSlot {
  q: number
  r: number
}

export interface HoneycombPoint {
  x: number
  y: number
}

export interface HoneycombGeometry {
  slotHeight: number
  slotSide: number
  slotWidth: number
}

export const flatTopHexPortCorners = ['top-left', 'top-right', 'right', 'bottom-right', 'bottom-left', 'left'] as const

export type FlatTopHexPortCorner = typeof flatTopHexPortCorners[number]

/** Named frame sides, ordered clockwise from the top flat edge. */
export const flatTopHexFrameSides = ['top', 'upper-right', 'lower-right', 'bottom', 'lower-left', 'upper-left'] as const

export type FlatTopHexFrameSide = typeof flatTopHexFrameSides[number]

export interface FlatTopHexSharedFrameSides {
  first: FlatTopHexFrameSide
  second: FlatTopHexFrameSide
}

export interface HoneycombJunctionCoordinate {
  u: number
  v: number
}

export interface HoneycombSlotPortIdentity {
  corner: FlatTopHexPortCorner
  junctionId: string
  slotId: string
  slotPortId: string
}

export interface HoneycombTurnPortAllocation extends HoneycombSlotPortIdentity {
  slot: AxialSlot
}

const flatTopHexCornerJunctionOffsets: Record<FlatTopHexPortCorner, HoneycombJunctionCoordinate> = {
  'top-left': { u: 1, v: 0 },
  'top-right': { u: 3, v: 0 },
  right: { u: 4, v: 1 },
  'bottom-right': { u: 3, v: 2 },
  'bottom-left': { u: 1, v: 2 },
  left: { u: 0, v: 1 },
}

export function flatTopHexSlotId({ q, r }: AxialSlot): string {
  return `slot/q${q}-r${r}`
}

export function flatTopHexJunctionCoordinate({ q, r }: AxialSlot, corner: FlatTopHexPortCorner): HoneycombJunctionCoordinate {
  const offset = flatTopHexCornerJunctionOffsets[corner]
  return {
    u: q * 3 + offset.u,
    v: r * 2 + q + offset.v,
  }
}

export function flatTopHexJunctionId(slot: AxialSlot, corner: FlatTopHexPortCorner): string {
  const { u, v } = flatTopHexJunctionCoordinate(slot, corner)
  return `junction=u${u}&v${v}`
}

export function flatTopHexSlotPortIdentity(slot: AxialSlot, corner: FlatTopHexPortCorner): HoneycombSlotPortIdentity {
  const junctionId = flatTopHexJunctionId(slot, corner)
  const slotId = flatTopHexSlotId(slot)
  return {
    corner,
    junctionId,
    slotId,
    slotPortId: `port/${junctionId}/${slotId}`,
  }
}

function sameJunction(first: HoneycombJunctionCoordinate, second: HoneycombJunctionCoordinate): boolean {
  return first.u === second.u && first.v === second.v
}

/**
 * Allocates a junction turn to the slot inside its angle. The selected slot is
 * the only logical hex that owns the previous, current, and next junctions.
 */
export function flatTopHexInnerTurnPortAllocation(previous: HoneycombJunctionCoordinate, current: HoneycombJunctionCoordinate, next: HoneycombJunctionCoordinate, slots: readonly AxialSlot[]): HoneycombTurnPortAllocation | null {
  const incomingU = previous.u - current.u
  const incomingV = previous.v - current.v
  const outgoingU = next.u - current.u
  const outgoingV = next.v - current.v
  if (incomingU * outgoingV === incomingV * outgoingU) return null

  for (const slot of slots) {
    const corners = flatTopHexPortCorners.map((corner) => ({ corner, junction: flatTopHexJunctionCoordinate(slot, corner) }))
    const currentCorner = corners.find(({ junction }) => sameJunction(junction, current))
    if (!currentCorner) continue
    if (!corners.some(({ junction }) => sameJunction(junction, previous))) continue
    if (!corners.some(({ junction }) => sameJunction(junction, next))) continue
    return { ...flatTopHexSlotPortIdentity(slot, currentCorner.corner), slot: { q: slot.q, r: slot.r } }
  }

  return null
}

/** Converts axial coordinates to the top-left position of a flat-topped hex. */
export function flatTopHexPosition({ q, r }: AxialSlot, { slotHeight, slotSide }: HoneycombGeometry): HoneycombPoint {
  const horizontalStep = slotSide * 1.5
  const verticalStep = slotHeight
  return {
    x: q * horizontalStep,
    y: (r + q / 2) * verticalStep,
  }
}

/**
 * Finds the two coincident perimeter sides for directly adjacent axial slots.
 * `first` and `second` are deliberately kept distinct so callers can hide or
 * decorate the same physical seam on both owning frames.
 */
export function flatTopHexSharedFrameSides(first: AxialSlot, second: AxialSlot): FlatTopHexSharedFrameSides | null {
  const delta = `${second.q - first.q},${second.r - first.r}`
  const sides: Record<string, FlatTopHexSharedFrameSides> = {
    '0,-1': { first: 'top', second: 'bottom' },
    '1,-1': { first: 'upper-right', second: 'lower-left' },
    '1,0': { first: 'lower-right', second: 'upper-left' },
    '0,1': { first: 'bottom', second: 'top' },
    '-1,1': { first: 'lower-left', second: 'upper-right' },
    '-1,0': { first: 'upper-left', second: 'lower-right' },
  }
  return sides[delta] ?? null
}

/** Returns a nominal, non-inset frame-side midpoint for controls and paths. */
export function flatTopHexFrameSideMidpoint(side: FlatTopHexFrameSide, { slotHeight, slotWidth }: HoneycombGeometry): HoneycombPoint {
  const midpoints: Record<FlatTopHexFrameSide, HoneycombPoint> = {
    top: { x: slotWidth / 2, y: 0 },
    'upper-right': { x: slotWidth * 0.875, y: slotHeight * 0.25 },
    'lower-right': { x: slotWidth * 0.875, y: slotHeight * 0.75 },
    bottom: { x: slotWidth / 2, y: slotHeight },
    'lower-left': { x: slotWidth * 0.125, y: slotHeight * 0.75 },
    'upper-left': { x: slotWidth * 0.125, y: slotHeight * 0.25 },
  }
  return midpoints[side]
}

/**
 * Returns a slot-owned port coordinate at a nominal frame vertex, where three
 * logical honeycomb frames meet. It intentionally ignores visual inset/shrink.
 */
export function flatTopHexCornerPortPoint(corner: FlatTopHexPortCorner, { slotHeight, slotWidth }: HoneycombGeometry): HoneycombPoint {
  const positions: Record<FlatTopHexPortCorner, HoneycombPoint> = {
    'top-left': { x: slotWidth * 0.25, y: 0 },
    'top-right': { x: slotWidth * 0.75, y: 0 },
    right: { x: slotWidth, y: slotHeight / 2 },
    'bottom-right': { x: slotWidth * 0.75, y: slotHeight },
    'bottom-left': { x: slotWidth * 0.25, y: slotHeight },
    left: { x: 0, y: slotHeight / 2 },
  }
  return positions[corner]
}

/** Returns the inward angle in degrees from a corner vertex towards the slot center. */
export function flatTopHexCornerInwardAngle(corner: FlatTopHexPortCorner): number {
  const angles: Record<FlatTopHexPortCorner, number> = {
    left: 0,
    'top-left': 60,
    'top-right': 120,
    right: 180,
    'bottom-right': 240,
    'bottom-left': 300,
  }
  return angles[corner]
}


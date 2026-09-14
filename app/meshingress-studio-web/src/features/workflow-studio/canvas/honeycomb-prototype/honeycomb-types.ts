import type { AxialSlot } from './honeycomb-grid'
import type { WorkflowCanvasPlacement } from '../../states/workflow-canvas-scene'

export interface HoneycombSlot extends AxialSlot {
  id: string
  kind: 'canvas-end' | 'workflow'
}

export interface HoneycombNodePlacement extends AxialSlot, WorkflowCanvasPlacement { }

export interface HoveredNodeSlot {
  clientX: number
  clientY: number
  label: string
  slotId: string
}

export interface ImmediateRouteSeam {
  destinationAngle: number
  id: string
  point: { x: number; y: number }
  sourceNodeId: string
  sourceSlotId: string
  targetSlotId: string
}

export function readableSlotLabel({ q, r }: AxialSlot): string {
  return `q${q}-r${r}`
}

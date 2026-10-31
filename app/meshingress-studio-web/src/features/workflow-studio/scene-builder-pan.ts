export interface CanvasPanPosition {
  x: number
  y: number
}

export const canvasPanActivationDistance = 4

export function hasCanvasPanStarted(start: CanvasPanPosition, current: CanvasPanPosition): boolean {
  return Math.hypot(current.x - start.x, current.y - start.y) >= canvasPanActivationDistance
}

export function panCanvas(position: CanvasPanPosition, from: CanvasPanPosition, to: CanvasPanPosition): CanvasPanPosition {
  return {
    x: position.x + to.x - from.x,
    y: position.y + to.y - from.y,
  }
}

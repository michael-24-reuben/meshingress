import { describe, expect, it } from 'vitest'
import { clampSceneBuilderControlsWidth, resizeSceneBuilderControls, sceneBuilderControlsMaxWidth, sceneBuilderControlsMinWidth } from '../features/workflow-studio/scene-builder-controls-resize'
import { canvasPanActivationDistance, hasCanvasPanStarted, panCanvas } from '../features/workflow-studio/scene-builder-pan'

describe('Scene Builder canvas pan', () => {
  it('keeps clicks below the pan threshold and translates after a drag starts', () => {
    expect(hasCanvasPanStarted({ x: 10, y: 10 }, { x: 10 + canvasPanActivationDistance - 1, y: 10 })).toBe(false)
    expect(hasCanvasPanStarted({ x: 10, y: 10 }, { x: 10 + canvasPanActivationDistance, y: 10 })).toBe(true)
    expect(panCanvas({ x: 12, y: -8 }, { x: 100, y: 80 }, { x: 128, y: 92 })).toEqual({ x: 40, y: 4 })
  })

  it('resizes the right control panel within its supported bounds', () => {
    expect(resizeSceneBuilderControls(320, 600, 540)).toBe(380)
    expect(resizeSceneBuilderControls(320, 600, 1200)).toBe(sceneBuilderControlsMinWidth)
    expect(clampSceneBuilderControlsWidth(999)).toBe(sceneBuilderControlsMaxWidth)
  })
})

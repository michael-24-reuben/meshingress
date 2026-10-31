export const sceneBuilderControlsMinWidth = 248
export const sceneBuilderControlsMaxWidth = 560

export function clampSceneBuilderControlsWidth(
  width: number,
  maxWidth = sceneBuilderControlsMaxWidth,
): number {
  return Math.max(sceneBuilderControlsMinWidth, Math.min(maxWidth, width))
}

export function resizeSceneBuilderControls(
  startWidth: number,
  startClientX: number,
  currentClientX: number,
  maxWidth = sceneBuilderControlsMaxWidth,
): number {
  return clampSceneBuilderControlsWidth(startWidth + startClientX - currentClientX, maxWidth)
}

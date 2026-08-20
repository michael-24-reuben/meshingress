/**
 * RightPanelBodyRail renders the right panel navigation rail using StudioRailGroup.
 *
 * NOTE: Architectural, styling, or prop signature updates made to this rail component
 * should be reflected in ActivityRail.tsx to preserve visual and functional parity.
 */
import type { RightPanelViewContentKind } from '../../types'
import { RAIL_ICON_SIZE } from '../../types'
import { rightRailItems } from '../panel-catalog'
import { StudioRailGroup } from './StudioRailGroup'

export function InteractiveRail({
  rightPanelBody: rightPanel,
  isRightPanelBodyVisible = true,
  onChange,
  iconSize = RAIL_ICON_SIZE,
  showToolNames = true,
  showTitles = true,
}: {
  rightPanelBody: RightPanelViewContentKind
  isRightPanelBodyVisible?: boolean
  onChange: (view: RightPanelViewContentKind) => void
  iconSize?: number
  showToolNames?: boolean
  showTitles?: boolean
}) {
  return (
    <nav aria-label="Right panel navigation" className="rail right-rail">
      <StudioRailGroup
        activeContent={rightPanel}
        groupClassName="rail-group-top"
        iconSize={iconSize}
        isVisible={isRightPanelBodyVisible}
        items={rightRailItems}
        onChange={onChange}
        showTitles={showTitles}
        showToolNames={showToolNames}
      />
    </nav>
  )
}

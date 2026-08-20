/**
 * ActivityRail renders the left studio activity bar using StudioRailGroup.
 *
 * NOTE: Architectural, styling, or prop signature updates made to this rail component
 * should be reflected in RightPanelBodyRail.tsx to preserve visual and functional parity.
 */
import type { DrawerPanelViewContentKind, LeftPanelViewContentKind } from '../../../types'
import { RAIL_ICON_SIZE } from '../../../types'
import { drawerRailItems, leftRailItems } from '../../panel-catalog'
import { StudioRailGroup } from '../StudioRailGroup'

export function ActivityRail({
  view,
  drawer,
  isLeftPanelVisible = true,
  isDrawerVisible = true,
  onViewChange,
  onDrawerChange,
  iconSize = RAIL_ICON_SIZE,
  showToolNames = true,
  showTitles = true,
  showToolBadges = true,
}: {
  view: LeftPanelViewContentKind
  drawer: DrawerPanelViewContentKind
  isLeftPanelVisible?: boolean
  isDrawerVisible?: boolean
  onViewChange: (view: LeftPanelViewContentKind) => void
  onDrawerChange: (view: DrawerPanelViewContentKind) => void
  iconSize?: number
  showToolNames?: boolean
  showTitles?: boolean
  showToolBadges?: boolean
}) {
  return (
    <nav aria-label="Activity bar" className="rail left-rail">
      <StudioRailGroup
        activeContent={view}
        groupClassName="rail-group-top"
        iconSize={iconSize}
        isVisible={isLeftPanelVisible}
        items={leftRailItems}
        onChange={onViewChange}
        showTitles={showTitles}
        showToolBadges={showToolBadges}
        showToolNames={showToolNames}
      />
      <StudioRailGroup
        activeContent={drawer}
        groupClassName="rail-group-bottom"
        iconSize={iconSize}
        isVisible={isDrawerVisible}
        items={drawerRailItems}
        onChange={onDrawerChange}
        showTitles={showTitles}
        showToolBadges={showToolBadges}
        showToolNames={showToolNames}
      />
    </nav>
  )
}

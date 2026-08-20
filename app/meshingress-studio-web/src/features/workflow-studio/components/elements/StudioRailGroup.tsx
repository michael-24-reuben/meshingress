import type { StudioRailItem } from '../panel-catalog'
import { getPanelContentKind } from '../panel-catalog'
import { RAIL_ICON_SIZE } from '../../types'

export function StudioRailGroup<T extends string>({
    items,
    activeContent,
    isVisible = true,
    groupClassName,
    onChange,
    iconSize = RAIL_ICON_SIZE,
    showToolNames = true,
    showTitles = true,
    showToolBadges = true,
}: {
    items: readonly StudioRailItem[]
    activeContent: T
    isVisible?: boolean
    groupClassName?: string
    onChange: (content: T) => void
    iconSize?: number
    showToolNames?: boolean
    showTitles?: boolean
    showToolBadges?: boolean
}) {
    return (
        <div className={`rail-group${groupClassName ? ` ${groupClassName}` : ''}`}>
            {items.map((item, index) => {
                if (item.type === 'separator') {
                    return <div aria-orientation="horizontal" className="rail-separator" key={`sep-${index}`} role="separator" />
                }
                if (item.type === 'label') {
                    return showTitles ? <span className="rail-title" key={`label-${index}`}>{item.label}</span> : null
                }
                const panel = item.panel
                const contentKind = getPanelContentKind(panel)
                const isActive = isVisible && (
                    activeContent === contentKind ||
                    (panel.surfaceEntry?.body && Array.isArray(panel.surfaceEntry.body) && panel.surfaceEntry.body.some((b) => b.kind === activeContent))
                )
                const Icon = panel.icon
                const labelText = panel.railLabel ?? panel.label
                const titleText = panel.railTitle ?? panel.label
                return (
                    <button
                        aria-pressed={isActive}
                        className={`rail-button${isActive ? ' active' : ''}`}
                        key={panel.railKey}
                        onClick={() => onChange(contentKind as T)}
                        title={titleText}
                        type="button"
                    >
                        <span className="rail-icon"><Icon size={iconSize} /></span>
                        {showToolNames && <span className="rail-label">{labelText}</span>}
                        {showToolBadges && panel.badge && <span className="rail-badge">{panel.badge}</span>}
                    </button>
                )
            })}
        </div>
    )
}

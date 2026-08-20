import { useEffect, useRef, useState } from 'react'
import {
    ChevronRightIcon,
    CollapseAllIcon,
    EllipsisVerticalIcon,
    ExpandAllIcon,
    ReloadIcon,
    SortIcon,
    CrossIcon,
} from '../../../../components/icons/node-icons'

export type SortOption = 'name-asc' | 'name-desc' | 'namespace' | 'count-desc'

export const getSortLabel = (sort: SortOption) => {
    switch (sort) {
        case 'name-asc': return 'Name (A to Z)'
        case 'name-desc': return 'Name (Z to A)'
        case 'namespace': return 'Namespace'
        case 'count-desc': return 'Function Count'
        default: return ''
    }
}

export interface LeftPanelActionsProps {
    onCollapseAll: () => void
    onExpandAll: () => void
    onRefreshTools?: () => void
    onStatus: (message: string) => void
    sortBy: SortOption
    onSortChange: (sort: SortOption) => void
    query?: string
    onClearQuery?: () => void
}

export function LeftPanelActions({
    onCollapseAll,
    onExpandAll,
    onRefreshTools,
    onStatus,
    sortBy,
    onSortChange,
    query,
    onClearQuery,
}: LeftPanelActionsProps) {
    const [isOptionsOpen, setIsOptionsOpen] = useState(false)
    const [isSortSubmenuOpen, setIsSortSubmenuOpen] = useState(false)
    const menuRef = useRef<HTMLDivElement>(null)
    const submenuTimerRef = useRef<number | null>(null)

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
                setIsOptionsOpen(false)
                setIsSortSubmenuOpen(false)
            }
        }
        document.addEventListener('mousedown', handleClickOutside)
        return () => document.removeEventListener('mousedown', handleClickOutside)
    }, [])

    const handleSubmenuMouseEnter = () => {
        if (submenuTimerRef.current) clearTimeout(submenuTimerRef.current)
        setIsSortSubmenuOpen(true)
    }

    const handleSubmenuMouseLeave = () => {
        submenuTimerRef.current = window.setTimeout(() => {
            setIsSortSubmenuOpen(false)
        }, 150)
    }

    return (
        <>
            <button
                aria-label="Collapse"
                className="panel-action-btn button small"
                onClick={onCollapseAll}
                title="Collapse"
                type="button"
            >
                <CollapseAllIcon size={16} />
            </button>
            <button
                aria-label="Refresh"
                className="panel-action-btn button small"
                onClick={() => {
                    onRefreshTools?.()
                    onStatus('Refilling tool tree...')
                }}
                title="Refresh tool tree"
                type="button"
            >
                <ReloadIcon size={16} />
            </button>
            <div className="panel-options-menu-wrapper" ref={menuRef}>
                <button
                    aria-expanded={isOptionsOpen}
                    aria-label="Options"
                    className={`panel-action-btn button small${isOptionsOpen ? ' is-active' : ''}`}
                    onClick={() => setIsOptionsOpen((prev) => !prev)}
                    title="Options"
                    type="button"
                >
                    <EllipsisVerticalIcon size={16} />
                </button>
                {isOptionsOpen && (
                    <div className="panel-options-menu-popover">
                        <div
                            className={`panel-options-submenu-wrapper${isSortSubmenuOpen ? ' is-open' : ''}`}
                            onMouseEnter={handleSubmenuMouseEnter}
                            onMouseLeave={handleSubmenuMouseLeave}
                        >
                            <button
                                aria-expanded={isSortSubmenuOpen}
                                aria-haspopup="true"
                                className={`panel-options-item panel-options-submenu-trigger${isSortSubmenuOpen ? ' is-active' : ''}`}
                                onClick={() => setIsSortSubmenuOpen((prev) => !prev)}
                                type="button"
                            >
                                <SortIcon size={14} />
                                <span className="panel-options-item-label">Sort by</span>
                                <span className="panel-options-submenu-value">{getSortLabel(sortBy)}</span>
                                <ChevronRightIcon className="panel-options-chevron" size={14} />
                            </button>
                            {isSortSubmenuOpen && (
                                <div className="panel-options-submenu-popover">
                                    <button
                                        className={`panel-options-item${sortBy === 'name-asc' ? ' is-active' : ''}`}
                                        onClick={() => {
                                            onSortChange('name-asc')
                                            setIsSortSubmenuOpen(false)
                                            setIsOptionsOpen(false)
                                            onStatus('Sorted by Name (A to Z)')
                                        }}
                                        type="button"
                                    >
                                        <span className="option-check">{sortBy === 'name-asc' ? '✓' : ''}</span>
                                        <span>Name (A to Z)</span>
                                    </button>
                                    <button
                                        className={`panel-options-item${sortBy === 'name-desc' ? ' is-active' : ''}`}
                                        onClick={() => {
                                            onSortChange('name-desc')
                                            setIsSortSubmenuOpen(false)
                                            setIsOptionsOpen(false)
                                            onStatus('Sorted by Name (Z to A)')
                                        }}
                                        type="button"
                                    >
                                        <span className="option-check">{sortBy === 'name-desc' ? '✓' : ''}</span>
                                        <span>Name (Z to A)</span>
                                    </button>
                                    <button
                                        className={`panel-options-item${sortBy === 'namespace' ? ' is-active' : ''}`}
                                        onClick={() => {
                                            onSortChange('namespace')
                                            setIsSortSubmenuOpen(false)
                                            setIsOptionsOpen(false)
                                            onStatus('Sorted by Namespace')
                                        }}
                                        type="button"
                                    >
                                        <span className="option-check">{sortBy === 'namespace' ? '✓' : ''}</span>
                                        <span>Namespace</span>
                                    </button>
                                    <button
                                        className={`panel-options-item${sortBy === 'count-desc' ? ' is-active' : ''}`}
                                        onClick={() => {
                                            onSortChange('count-desc')
                                            setIsSortSubmenuOpen(false)
                                            setIsOptionsOpen(false)
                                            onStatus('Sorted by Function Count')
                                        }}
                                        type="button"
                                    >
                                        <span className="option-check">{sortBy === 'count-desc' ? '✓' : ''}</span>
                                        <span>Function Count</span>
                                    </button>
                                </div>
                            )}
                        </div>
                        <div className="panel-options-divider" />
                        <button
                            className="panel-options-item"
                            onClick={() => { onExpandAll(); setIsOptionsOpen(false) }}
                            type="button"
                        >
                            <ExpandAllIcon size={14} />
                            <span>Expand All</span>
                        </button>
                        <button
                            className="panel-options-item"
                            onClick={() => { onCollapseAll(); setIsOptionsOpen(false) }}
                            type="button"
                        >
                            <CollapseAllIcon size={14} />
                            <span>Collapse All</span>
                        </button>
                        <button
                            className="panel-options-item"
                            onClick={() => { onRefreshTools?.(); setIsOptionsOpen(false) }}
                            type="button"
                        >
                            <ReloadIcon size={14} />
                            <span>Refresh Tools</span>
                        </button>
                        {query && (
                            <button
                                className="panel-options-item"
                                onClick={() => { onClearQuery?.(); setIsOptionsOpen(false) }}
                                type="button"
                            >
                                <CrossIcon size={14} />
                                <span>Clear Filter</span>
                            </button>
                        )}
                    </div>
                )}
            </div>
        </>
    )
}

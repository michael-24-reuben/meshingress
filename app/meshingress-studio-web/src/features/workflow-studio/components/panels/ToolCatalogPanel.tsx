import { useEffect, useState } from 'react'
import type { ToolPresentationIndex } from '../../node-presentation'
import { type RegisteredTool } from '../../types'
import { ToolTree } from '../elements/ToolTree'
import { Empty } from '../elements/Empty'
import type { SortOption } from '../StudioLeftPanel'

export interface ToolCatalogPanelProps {
    toolsState: 'refreshing' | 'loading' | 'ready' | 'error'
    tools: RegisteredTool[]
    presentations: ToolPresentationIndex
    sortBy?: SortOption
    expandAllState?: boolean | null
    expandKey?: number
    onToolAdd: (toolName: string) => void
    onRefreshTools?: () => void
    refreshCountdownMs?: number
}

export function ToolCatalogPanel({
    toolsState,
    tools,
    presentations,
    sortBy = 'name-asc',
    expandAllState = null,
    expandKey = 0,
    onToolAdd,
    refreshCountdownMs = 3000,
}: ToolCatalogPanelProps) {
    const [isRefreshExpired, setIsRefreshExpired] = useState(false)

    useEffect(() => {
        if (toolsState === 'refreshing') {
            setIsRefreshExpired(false)
            const timer = setTimeout(() => {
                setIsRefreshExpired(true)
            }, refreshCountdownMs)
            return () => clearTimeout(timer)
        } else {
            setIsRefreshExpired(false)
        }
    }, [toolsState, refreshCountdownMs])

    if (toolsState === 'loading') return <Empty message="Discovering attached tools..." />
    if (toolsState === 'error') return <Empty message="Failed to load tool catalog." />

    const isRefreshing = toolsState === 'refreshing'

    return (
        <div className="tool-tree-wrapper" data-refreshing={isRefreshing ? 'true' : 'false'} data-tools-state={toolsState}>
            {tools.length && !isRefreshExpired ? (
                <ToolTree expandAll={expandAllState} key={expandKey} onToolAdd={onToolAdd} presentations={presentations} sortBy={sortBy} tools={tools} />
            ) : (
                <Empty message="No matching tools." />
            )}
        </div>
    )
}

export default ToolCatalogPanel


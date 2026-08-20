import type { ToolPresentationIndex } from '../../node-presentation'
import type { RegisteredTool } from '../../types'
import { ToolTree } from '../elements/ToolTree'
import { Empty } from '../elements/Empty'
import type { SortOption } from '../StudioLeftPanel'

export interface StarredToolsPanelProps {
    toolsState: 'refreshing' | 'loading' | 'ready' | 'error'
    bookmarkedTools: RegisteredTool[]
    presentations: ToolPresentationIndex
    sortBy?: SortOption
    expandAllState?: boolean | null
    expandKey?: number
    onToolAdd: (toolName: string) => void
}

export function StarredToolsPanel({
    toolsState,
    bookmarkedTools,
    presentations,
    sortBy = 'name-asc',
    expandAllState = null,
    expandKey = 0,
    onToolAdd,
}: StarredToolsPanelProps) {
    if (toolsState === 'loading') return <Empty message="Discovering attached tools..." />
    if (toolsState === 'error') return <Empty message="Failed to load starred tools." />
    return bookmarkedTools.length ? (
        <ToolTree expandAll={expandAllState} key={expandKey} onToolAdd={onToolAdd} presentations={presentations} sortBy={sortBy} tools={bookmarkedTools} />
    ) : (
        <Empty message="No starred tools yet." />
    )
}

export default StarredToolsPanel

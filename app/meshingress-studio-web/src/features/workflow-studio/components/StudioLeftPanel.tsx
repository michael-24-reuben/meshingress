import { useState } from 'react'
import type { LeftPanelViewContentKind, LeftPanelViewContentParams, SurfaceEntry } from '../types'
import { leftStudioRailPanels, resolveSurfaceTab } from './panel-catalog'
import { SurfaceFrame } from './SurfaceFrame'
import { Empty } from './elements/Empty'
import { HideAction, LeftPanelActions, useTreeExpand, type SortOption } from './actions'

interface LeftPanelProps {
    view: LeftPanelViewContentKind
    params: LeftPanelViewContentParams
    onChange: (view: LeftPanelViewContentKind) => void
    onHide: () => void
    onStatus?: (message: string) => void
}

export function StudioLeftPanel({ view, params, onChange, onHide, onStatus = () => { } }: LeftPanelProps) {
    const [query, setQuery] = useState('')
    const [sortBy, setSortBy] = useState<SortOption>('name-asc')
    const { expandAllState, expandKey, handleExpandAll, handleCollapseAll } = useTreeExpand(onStatus)


    const panel = leftStudioRailPanels.find((candidate) => {
        const body = candidate.surfaceEntry?.body
        if (Array.isArray(body)) {
            return body.some((tab) => tab.kind === view)
        }
        return body?.kind === view
    })

    if (!panel?.surfaceEntry) {
        return <SurfaceFrame content={{
            target: 'left',
            title: 'Left panel',
            actions: <HideAction onHide={onHide} />,
            body: <Empty message="No content is available." />,
        }} />
    }

    const surfaceEntry = panel.surfaceEntry as SurfaceEntry
    const bodyEntry = surfaceEntry.body
    const isTabs = Array.isArray(bodyEntry)

    const rawProps = params[view as keyof LeftPanelViewContentParams] as any
    const mergedProps = {
        ...rawProps,
        sortBy,
        expandAllState,
        expandKey,
    }

    const tabs = isTabs ? bodyEntry.map((tab) => resolveSurfaceTab<LeftPanelViewContentKind>(tab, mergedProps)) : undefined
    const activeTabEntry = isTabs ? bodyEntry.find((tab) => tab.kind === view) : undefined

    const bodyContent = isTabs
        ? (activeTabEntry ? activeTabEntry.tabContent(mergedProps) : <Empty message="No content is available." />)
        : (bodyEntry ? (bodyEntry as any).content(mergedProps) : <Empty message="No content is available." />)

    const actions = (
        <>
            <LeftPanelActions
                onClearQuery={() => setQuery('')}
                onCollapseAll={handleCollapseAll}
                onExpandAll={handleExpandAll}
                onRefreshTools={rawProps?.onRefreshTools}
                onSortChange={setSortBy}
                onStatus={onStatus}
                query={query}
                sortBy={sortBy}
            />
            <HideAction onHide={onHide} />
        </>
    )

    const title = typeof surfaceEntry.title === 'function'
        ? surfaceEntry.title(mergedProps)
        : surfaceEntry.title

    return <SurfaceFrame content={{
        target: 'left',
        title,
        titleAttributes: surfaceEntry.titleAttributes,
        tabs,
        activeTab: isTabs ? view : undefined,
        onTabChange: isTabs ? onChange : undefined,
        actions,
        body: bodyContent,
        bodyAttributes: surfaceEntry.bodyAttributes,
        bodyKey: view,
    }} />
}

export { type SortOption }


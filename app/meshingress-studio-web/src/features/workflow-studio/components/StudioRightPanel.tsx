import type { RightPanelViewContentKind, RightPanelViewContentParams, SurfaceEntry, WorkflowNode } from '../types'
import { rightStudioRailPanels, resolveSurfaceTab } from './panel-catalog'
import { SurfaceFrame } from './SurfaceFrame'
import { MinusIcon } from '../../../components/icons/node-icons'
import { Empty } from './elements/Empty'
import type { ToolPresentationIndex } from '../node-presentation'

interface RightPanelBodyPanelProps {
    rightPanelBody: RightPanelViewContentKind
    params: RightPanelViewContentParams
    selectedNode?: WorkflowNode
    presentations: ToolPresentationIndex
    onHide: () => void
    onRightPanelBodyChange: (view: RightPanelViewContentKind) => void
    onNodeChange: (id: string, changes: Partial<WorkflowNode>) => void
}

export function StudioRightPanel({
    rightPanelBody,
    params,
    selectedNode,
    presentations,
    onHide,
    onRightPanelBodyChange,
    onNodeChange,
}: RightPanelBodyPanelProps) {
    const panel = rightStudioRailPanels.find((candidate) => {
        const body = candidate.surfaceEntry?.body
        if (Array.isArray(body)) {
            return body.some((tab) => tab.kind === rightPanelBody)
        }
        return body?.kind === rightPanelBody
    })

    if (!panel?.surfaceEntry) {
        return (
            <SurfaceFrame
                content={{
                    target: 'right',
                    title: 'Details',
                    actions: (
                        <button aria-label="Hide" className="panel-action-btn button small" onClick={onHide} title="Hide" type="button">
                            <MinusIcon size={16} />
                        </button>
                    ),
                    body: <Empty message="No panel content is available." />,
                }}
            />
        )
    }

    const surfaceEntry = panel.surfaceEntry as SurfaceEntry
    const bodyEntry = surfaceEntry.body
    const isTabs = Array.isArray(bodyEntry)
    const rawProps = params[rightPanelBody as keyof RightPanelViewContentParams] as any
    const tabs = isTabs
        ? bodyEntry.map((tab) => resolveSurfaceTab<RightPanelViewContentKind>(tab, rawProps))
        : undefined
    const activeTabEntry = isTabs ? bodyEntry.find((tab) => tab.kind === rightPanelBody) : undefined

    const isNodeDependentView = rightPanelBody === 'node-editor' || rightPanelBody === 'node-payload'
    const isNodeViewWithoutSelection = isNodeDependentView && !selectedNode

    const title = isNodeViewWithoutSelection
        ? 'No node selected'
        : typeof surfaceEntry.title === 'function'
            ? surfaceEntry.title({ selectedNode, presentations, onNodeChange })
            : surfaceEntry.title

    const body = isNodeViewWithoutSelection
        ? <Empty message="No content is available." />
        : isTabs
            ? (activeTabEntry ? activeTabEntry.tabContent(rawProps) : <Empty message="No content is available." />)
            : (bodyEntry ? (bodyEntry as any).content(rawProps) : <Empty message="No content is available." />)

    return (
        <SurfaceFrame
            content={{
                target: 'right',
                title,
                titleAttributes: surfaceEntry.titleAttributes,
                tabs,
                activeTab: rightPanelBody,
                onTabChange: onRightPanelBodyChange,
                actions: (
                    <button aria-label="Hide" className="panel-action-btn button small" onClick={onHide} title="Hide" type="button">
                        <MinusIcon size={16} />
                    </button>
                ),
                body,
                bodyAttributes: surfaceEntry.bodyAttributes ?? { className: 'right-panel-body' },
                bodyKey: rightPanelBody,
            }}
        />
    )
}

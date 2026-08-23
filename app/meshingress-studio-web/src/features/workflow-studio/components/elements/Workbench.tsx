import type { ToolPresentationIndex } from '../../node-presentation'
import type {
  ElementAttributeInput,
  NodeRunState,
  WorkflowEdge,
  WorkflowNode,
} from '../../types'
import { WorkflowCanvas } from '../WorkflowCanvas'
import type { McpToolFunction } from '../../../../api/mcp'
import { CrossIcon, WorkflowIcon } from '../../../../components/icons/node-icons'
import type { PanelIcon } from '../panel-catalog'
import { memo, useCallback, useMemo, useState } from 'react'
import { formatElementAttributes } from '../../utilities'
import DotField from '@/components/DotField'

// --- Tabs Components ---

type ContentKind = 'workflow' | 'file' | 'editor' | 'mcp'

interface TabProperties {
  tabKey: string
  icon: PanelIcon
  label: string
  isSelected: boolean
  attributes?: ElementAttributeInput
  onSelect: () => void
  onClose?: () => void
}

const Tab = memo(function Tab({
  tabKey,
  icon: Icon,
  label,
  isSelected,
  attributes,
  onSelect,
  onClose,
}: TabProperties) {
  const handleKeyDown = useCallback(
    (event: React.KeyboardEvent<HTMLDivElement>) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault()
        onSelect()
      }
    },
    [onSelect]
  )

  const props = formatElementAttributes(attributes, {
    id: `tab-${tabKey}`,
    className: `canvas-tab ${isSelected ? 'is-active' : ''}`,
    role: 'tab',
    'aria-selected': isSelected,
    'aria-controls': `tabpanel-${tabKey}`,
    'aria-label': `${isSelected ? 'Active' : 'Open'} tab: ${label}`,
    tabIndex: isSelected ? 0 : -1,
  })

  return (
    <div {...props} onClick={onSelect} onKeyDown={handleKeyDown}>
      <Icon aria-hidden="true" size={15} />
      <span className="canvas-tab-title">{label}</span>
      {onClose && (
        <button
          aria-label={`Close ${label}`}
          className="canvas-tab-close"
          onClick={(event) => {
            event.stopPropagation()
            onClose()
          }}
          title={`Close ${label}`}
          type="button"
        >
          <CrossIcon aria-hidden="true" size={16} />
        </button>
      )}
    </div>
  )
})

interface TabInfo {
  tabKey: string
  type: ContentKind
  label: string
  icon: PanelIcon
  attributes?: ElementAttributeInput
  onActiveTab?: (tabKey: string) => void
  onTabClose?: (tabKey: string) => void
  onTabRemove?: (tabKey: string) => void
  content: (attributes?: ElementAttributeInput) => React.ReactNode
}

interface TabPaneProps {
  tabInfos: readonly TabInfo[]
  activeTabKey?: string | null
  onActiveTab?: (tabKey: string | null) => void
  onTabClose?: (tabKey: string) => void
  onTabRemove?: (tabKey: string) => void
  attributes?: ElementAttributeInput
}

// --- TabPane Component ---
const TabPane = memo(function TabPane({
  tabInfos,
  activeTabKey,
  onActiveTab,
  onTabClose,
  onTabRemove,
  attributes,
}: TabPaneProps) {
  const activeTab = tabInfos.find((tabInfo) => tabInfo.tabKey === activeTabKey)

  return (
    <>
      <div className="canvas-head">
        <div aria-label="Open workflow files" className="canvas-tabs" role="tablist">
          {tabInfos.map((tabInfo) => {
            const handleSelect = () => {
              if (tabInfo.onActiveTab) {
                tabInfo.onActiveTab(tabInfo.tabKey)
              } else {
                onActiveTab?.(tabInfo.tabKey)
              }
            }

            const hasCloseHandler = Boolean(
              tabInfo.onTabRemove ||
              tabInfo.onTabClose ||
              onTabRemove ||
              onTabClose
            )

            const handleClose = hasCloseHandler
              ? () => {
                const removeHandler = tabInfo.onTabRemove ?? onTabRemove
                const closeHandler = tabInfo.onTabClose ?? onTabClose

                removeHandler?.(tabInfo.tabKey)
                closeHandler?.(tabInfo.tabKey)

                if (tabInfo.tabKey === activeTabKey) {
                  const remainingTabs = tabInfos.filter(
                    (candidate) => candidate.tabKey !== tabInfo.tabKey
                  )
                  const nextActiveTabKey =
                    remainingTabs.length > 0 ? remainingTabs[0].tabKey : null
                  onActiveTab?.(nextActiveTabKey)
                }
              }
              : undefined

            return (
              <Tab
                key={tabInfo.tabKey}
                attributes={tabInfo.attributes}
                icon={tabInfo.icon}
                isSelected={tabInfo.tabKey === activeTabKey}
                label={tabInfo.label}
                onClose={handleClose}
                onSelect={handleSelect}
                tabKey={tabInfo.tabKey}
              />
            )
          })}
        </div>
      </div>
      {activeTab ? (
        <div
          aria-label={activeTab ? undefined : 'No workflow tab is open'}
          aria-labelledby={
            activeTab ? `tab-${activeTab.tabKey}` : undefined
          }
          className="canvas-tabpanel"
          id={activeTab ? `tabpanel-${activeTab.tabKey}` : undefined}
          role="tabpanel"
        >
          {activeTab ? (
            activeTab.content(attributes)
          ) : (
            <div className="canvas-empty">
              <span>No workflow tab is open.</span>
            </div>
          )}
        </div>
      ) : (
        <div aria-label="No workflow tab is open" className="canvas-tabpanel" id="canvas-tabpanel-empty" role="tabpanel">
          <DotField
            dotRadius={2}
            dotSpacing={20}
            cursorRadius={200}
            cursorForce={0.05}
            bulgeOnly={true}
            bulgeStrength={20}
            glowRadius={50}
            sparkle={true}
            waveAmplitude={0}
            gradientFrom="rgba(168, 85, 247, 0.35)"
            gradientTo="rgba(180, 151, 207, 0.25)"
            glowColor="transparent"

          />
        </div>
      )}
    </>
  )
})
// ---

export interface WorkbenchProps {
  workflowFileName: string
  tabPaneAttributes?: ElementAttributeInput
  edges: WorkflowEdge[]
  nodes: WorkflowNode[]
  runStates: Record<string, NodeRunState>
  selectedNodeId: string | null
  pendingToolNode: { requestId: string; toolName: string } | null
  presentations: ToolPresentationIndex
  reattachOnEmptyRelease?: boolean
  running: boolean
  toolFunctions: McpToolFunction[]
  setEdges?: React.Dispatch<React.SetStateAction<WorkflowEdge[]>>
  onEdgeCreate?: (sourceId: string, targetId: string) => void
  onEdgeDelete?: (edge: WorkflowEdge) => void
  onEdgeReconnect?: (edge: WorkflowEdge, targetId: string) => void
  onNodesChange: (change: (nodes: WorkflowNode[]) => WorkflowNode[]) => void
  onToolFavorite: (node: WorkflowNode) => void
  onPendingToolNodeHandled: (requestId: string) => void
  onRun: () => void
  onSelectLayoutNodeChange: (selectNode: ((nodeId: string) => void) | null) => void
  onSelectNode: (id: string) => void
  onStatus: (message: string) => void
  onToggleReattachOnEmptyRelease: () => void
  onToolInsert: (toolName: string, position: { x: number; y: number }) => void
  onValidate: () => void
}

export function Workbench({
  workflowFileName,
  tabPaneAttributes,
  edges,
  nodes,
  runStates,
  selectedNodeId,
  pendingToolNode,
  presentations,
  reattachOnEmptyRelease,
  running,
  toolFunctions,
  setEdges,
  onEdgeCreate,
  onEdgeDelete,
  onEdgeReconnect,
  onNodesChange,
  onToolFavorite,
  onPendingToolNodeHandled,
  onRun,
  onSelectLayoutNodeChange,
  onSelectNode,
  onStatus,
  onToggleReattachOnEmptyRelease,
  onToolInsert,
  onValidate,
}: WorkbenchProps) {
  const workflowTabKey = `workflow:${workflowFileName}`
  const [selectedTabKey, setSelectedTabKey] = useState<string | null>(null)
  const [closedTabKeys, setClosedTabKeys] = useState<Set<string>>(
    () => new Set()
  )

  // Derive active tab key defaulting to current workflow file tab key unless closed
  const activeTabKey = useMemo(() => {
    const candidate = selectedTabKey ?? workflowTabKey
    return closedTabKeys.has(candidate) ? null : candidate
  }, [selectedTabKey, workflowTabKey, closedTabKeys])

  const handleActiveTab = useCallback((tabKey: string | null) => {
    setSelectedTabKey(tabKey)
  }, [])

  const handleTabRemove = useCallback(
    (tabKey: string) => {
      setClosedTabKeys((current) => {
        const updated = new Set(current)
        updated.add(tabKey)
        return updated
      })
      onStatus(`Tab "${tabKey}" removed.`)
    },
    [onStatus]
  )

  const handleEdgeCreate = useMemo(() => {
    return (
      onEdgeCreate ??
      ((sourceId: string, targetId: string) => {
        setEdges?.((current) => [
          ...current.filter((edge) => edge.source !== sourceId),
          { source: sourceId, target: targetId },
        ])
      })
    )
  }, [onEdgeCreate, setEdges])

  const handleEdgeDelete = useMemo(() => {
    return (
      onEdgeDelete ??
      ((edgeToDelete: WorkflowEdge) => {
        setEdges?.((current) =>
          current.filter(
            (edge) =>
              !(
                edge.source === edgeToDelete.source &&
                edge.target === edgeToDelete.target
              )
          )
        )
      })
    )
  }, [onEdgeDelete, setEdges])

  const handleEdgeReconnect = useMemo(() => {
    return (
      onEdgeReconnect ??
      ((edge: WorkflowEdge, targetId: string) => {
        setEdges?.((current) => {
          const index = current.findIndex(
            (candidate) =>
              candidate.source === edge.source &&
              candidate.target === edge.target
          )
          if (index < 0) return current
          return current.map((candidate, candidateIndex) =>
            candidateIndex === index
              ? { ...candidate, target: targetId }
              : candidate
          )
        })
      })
    )
  }, [onEdgeReconnect, setEdges])

  const tabInfos: readonly TabInfo[] = useMemo(
    () =>
      [
        {
          tabKey: workflowTabKey,
          type: 'workflow' as const,
          label: workflowFileName,
          icon: WorkflowIcon,
          onActiveTab: (key: string) => handleActiveTab(key),
          onTabRemove: handleTabRemove,
          content: (attributes: ElementAttributeInput | undefined) => (
            <WorkflowCanvas
              attributes={attributes}
              edges={edges}
              nodeRunStates={runStates}
              nodes={nodes}
              onEdgeCreate={handleEdgeCreate}
              onEdgeDelete={handleEdgeDelete}
              onEdgeReconnect={handleEdgeReconnect}
              onNodesChange={onNodesChange}
              onToolFavorite={onToolFavorite}
              onPendingToolNodeHandled={onPendingToolNodeHandled}
              onRun={onRun}
              onSelectLayoutNodeChange={onSelectLayoutNodeChange}
              onSelect={onSelectNode}
              onStatus={onStatus}
              onToggleReattachOnEmptyRelease={onToggleReattachOnEmptyRelease}
              onToolInsert={onToolInsert}
              onValidate={onValidate}
              pendingToolNode={pendingToolNode}
              presentations={presentations}
              reattachOnEmptyRelease={reattachOnEmptyRelease}
              running={running}
              selectedNodeId={selectedNodeId ?? ''}
              toolFunctions={toolFunctions}
            />
          ),
        },
      ].filter((tab) => !closedTabKeys.has(tab.tabKey)),
    [
      workflowTabKey,
      workflowFileName,
      handleActiveTab,
      handleTabRemove,
      closedTabKeys,
      edges,
      runStates,
      nodes,
      handleEdgeCreate,
      handleEdgeDelete,
      handleEdgeReconnect,
      onNodesChange,
      onToolFavorite,
      onPendingToolNodeHandled,
      onRun,
      onSelectLayoutNodeChange,
      onSelectNode,
      onStatus,
      onToggleReattachOnEmptyRelease,
      onToolInsert,
      onValidate,
      pendingToolNode,
      presentations,
      reattachOnEmptyRelease,
      running,
      selectedNodeId,
      toolFunctions,
    ]
  )

  return (
    <TabPane
      activeTabKey={activeTabKey}
      attributes={tabPaneAttributes}
      onActiveTab={handleActiveTab}
      onTabRemove={handleTabRemove}
      tabInfos={tabInfos}
    />
  )
}

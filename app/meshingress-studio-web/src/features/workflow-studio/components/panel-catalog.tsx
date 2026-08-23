import { useEffect, useState, type ComponentType } from 'react'
import type { CodeSquareFilledIconProps } from '../../../components/icons/node-icons'
import {
  BookmarkStarIcon,
  CubeIcon,
  FolderIcon,
  LogIcon,
  NodeEditIcon,
  PencilIcon,
  ReceiptCubeIcon,
  RuntimeIcon,
  WorkflowFolderIcon,
  WorkflowNodeIcon,
} from '../../../components/icons/node-icons'

import { ToolCatalogPanel, type ToolCatalogPanelProps } from './panels/ToolCatalogPanel'
import { WorkspaceExplorerPanel, type WorkspaceExplorerPanelProps } from './panels/WorkspaceExplorerPanel'
import { WorkflowFilesPanel } from './panels/WorkflowFilesPanel'
import { StarredToolsPanel, type StarredToolsPanelProps } from './panels/StarredToolsPanel'
import { StarredWorkflowsPanel } from './panels/StarredWorkflowsPanel'
import { WorkflowToolsPanel, type WorkflowToolsPanelProps } from './panels/WorkflowToolsPanel'
import { VariablesPanel, type VariablesPanelProps } from './panels/VariablesPanel'
import { CurrentExecutionPanel, type CurrentExecutionPanelProps } from './panels/CurrentExecutionPanel'
import { RuntimeTracePanel, type RuntimeTracePanelProps } from './panels/RuntimeTracePanel'
import { NodeToolFunctionPanel, type EditNodePanelTitleProps, type NodeToolFunctionPanelProps } from './panels/NodeToolFunctionPanel'
import { NodePayloadPanel, type NodePayloadPanelProps } from './panels/NodePayloadPanel'
import { WorkflowDefinitionPanel, type WorkflowDefinitionPanelProps } from './panels/WorkflowDefinitionPanel'
import { Empty } from './elements/Empty'
import type { ElementAttributeInput } from '../types'

export type PanelViewSetContentParams = {
  'tool-catalog': ToolCatalogPanelProps  //                ToolCatalogPanel.tsx	                <ToolCatalogPanel>
  'workspace-explorer': WorkspaceExplorerPanelProps  //    WorkspaceExplorerPanel.tsx	          <WorkspaceExplorerPanel>
  'workflow-files': Record<string, never>  //              WorkflowFilesPanel.tsx	              <WorkflowFilesPanel>
  'starred-tools': StarredToolsPanelProps  //              StarredToolsPanel.tsx	              <StarredToolsPanel>
  'starred-workflows': Record<string, never>  //           StarredWorkflowsPanel.tsx	          <StarredWorkflowsPanel>
  'workflow-tools': WorkflowToolsPanelProps  //        UsedWorkflowToolsPanel.tsx	          <UsedWorkflowToolsPanel>
  'workflow-variables': VariablesPanelProps  //            VariablesPanel.tsx	                  <VariablesPanel>
  'workflow-activity': CurrentExecutionPanelProps  //      CurrentExecutionPanel.tsx	          <CurrentExecutionPanel>
  'workflow-runtime': RuntimeTracePanelProps  //           RuntimeTracePanel.tsx	              <RuntimeTracePanel>
  'node-editor': NodeToolFunctionPanelProps  //            NodeToolFunctionPanel.tsx	          <NodeToolFunctionPanel>
  'node-payload': NodePayloadPanelProps  //                NodePayloadPanel.tsx	                <NodePayloadPanel>
  'workflow-payload': WorkflowDefinitionPanelProps  //     WorkflowDefinitionPanel.tsx	        <WorkflowDefinitionPanel>
}
export type PanelViewContentKind = keyof PanelViewSetContentParams
export type PanelViewContentParams = PanelViewSetContentParams[PanelViewContentKind]

export type PanelSurface = 'left' | 'right' | 'drawer'
export type PanelIcon = ComponentType<CodeSquareFilledIconProps>
export type RailPlacement = { surface: PanelSurface; order: number }

import type { SurfaceTab } from './SurfaceFrame'

export type SurfaceTabDescriptor<P = any> = {
  title?: React.ReactNode | ((props: P) => React.ReactNode)
  icon?: PanelIcon
  badge?: React.ReactNode | ((props: P) => React.ReactNode)
  disabled?: boolean | ((props: P) => boolean)
  tooltip?: string | ((props: P) => string | undefined)
}

export type SurfaceTabRenderer<P = any> =
  | React.ComponentType<{ props: P; active?: boolean }>
  | ((props: P, active?: boolean) => React.ReactNode)

export type SurfaceTabEntry = {
  [K in PanelViewContentKind]: {
    kind: K
    tab?: SurfaceTabDescriptor<PanelViewSetContentParams[K]> | SurfaceTabRenderer<PanelViewSetContentParams[K]>
    tabTitle?: string | React.ReactNode | ((props: PanelViewSetContentParams[K]) => React.ReactNode)
    tabIcon?: PanelIcon
    tabBadge?: React.ReactNode | ((props: PanelViewSetContentParams[K]) => React.ReactNode)
    tabDisabled?: boolean | ((props: PanelViewSetContentParams[K]) => boolean)
    tabContent: (props: PanelViewSetContentParams[K]) => React.ReactNode
  }
}[PanelViewContentKind]

export function resolveSurfaceTab<T extends string = string>(
  tabEntry: SurfaceTabEntry,
  props: any
): SurfaceTab<T> {
  const id = tabEntry.kind as T

  if (tabEntry.tab) {
    if (typeof tabEntry.tab === 'function') {
      const tabTarget = tabEntry.tab as any
      return {
        id,
        CustomHeader: (active: boolean) => {
          if (tabTarget.prototype && (tabTarget.prototype as any)?.isReactComponent) {
            const Comp = tabTarget
            return <Comp props={props} active={active} />
          }
          const res = tabTarget(props, active)
          if (res && typeof res === 'object' && ('title' in res || 'icon' in res)) {
            const Icon = res.icon
            return (
              <>
                {Icon && <span className="tab-icon"><Icon size={14} /></span>}
                {res.title !== undefined && <span>{res.title}</span>}
                {res.badge !== undefined && res.badge !== null && <span className="tab-badge">{res.badge}</span>}
              </>
            )
          }
          return res
        },
      }
    } else {
      const desc = tabEntry.tab as SurfaceTabDescriptor
      const label = typeof desc.title === 'function' ? desc.title(props) : desc.title
      const icon = desc.icon
      const badge = typeof desc.badge === 'function' ? desc.badge(props) : desc.badge
      const disabled = typeof desc.disabled === 'function' ? desc.disabled(props) : desc.disabled
      const tooltip = typeof desc.tooltip === 'function' ? desc.tooltip(props) : desc.tooltip

      return { id, label, icon, badge, disabled, tooltip }
    }
  }

  const label = typeof tabEntry.tabTitle === 'function' ? tabEntry.tabTitle(props) : tabEntry.tabTitle
  const icon = tabEntry.tabIcon
  const badge = typeof tabEntry.tabBadge === 'function' ? tabEntry.tabBadge(props) : tabEntry.tabBadge
  const disabled = typeof tabEntry.tabDisabled === 'function' ? tabEntry.tabDisabled(props) : tabEntry.tabDisabled

  return { id, label, icon, badge, disabled }
}

type SurfaceBodyEntry = {
  [K in PanelViewContentKind]: {
    kind: K
    content: (props: PanelViewSetContentParams[K]) => React.ReactNode
  }
}[PanelViewContentKind]

export type SurfaceEntry = {
  actions?: React.ReactNode
  title: React.ReactNode | ((...args: any[]) => React.ReactNode)
  titleAttributes?: ElementAttributeInput
  body?: SurfaceBodyEntry | SurfaceTabEntry[]
  bodyAttributes?: ElementAttributeInput
}

interface PanelDetails {
  label: string // Label displayed in the panel header (and search result entry)
  description: string // Description displayed in the search result entry
  keywords: readonly string[] // Keywords for searching the panel
  placement: RailPlacement // Placement of the panel in the rail
  icon: PanelIcon // Icon for the panel
  badge?: string // Badge displayed on the panel icon
  railKey: string // Unique identifier for the panel as attribute `key`
  railLabel?: string // Text next to the icon
  railTitle?: string // Text on-hover
  surfaceEntry?: SurfaceEntry
}

export type StudioPanel = PanelDetails

export type StudioRailItem =
  | { type: 'panel'; panel: StudioPanel }
  | { type: 'separator'; placement: RailPlacement }
  | { type: 'label'; label: string; placement: RailPlacement }

export const studioRailPanels = [
  {
    type: 'panel',
    panel: {
      label: 'Explorer',
      description: 'Browse the selected local workspace.',
      keywords: ['workspace', 'folder', 'local', 'files'],
      placement: { surface: 'left', order: 0 },
      railKey: 'workspace-explorer',
      railLabel: 'Explorer',
      railTitle: 'Explorer',
      icon: WorkflowFolderIcon,
      badge: undefined,
      surfaceEntry: {
        title: 'Explorer',
        body: {
          kind: 'workspace-explorer',
          content: (props) => <WorkspaceExplorerPanel {...props} />,
        },
        bodyAttributes: {
          className: ['explorer-panel-body']
        }
      },
    },
  },
  {
    type: 'panel',
    panel: {
      label: 'Tools',
      description: 'Browse attached tool functions.',
      keywords: ['catalog', 'functions', 'mcp'],
      placement: { surface: 'left', order: 1 },
      railKey: 'tool-catalog',
      railLabel: 'Tools',
      railTitle: 'Tools',
      icon: CubeIcon,
      badge: undefined,
      surfaceEntry: {
        title: 'Tools',
        body: {
          kind: 'tool-catalog',
          content: (props) => <ToolCatalogPanel {...props} />,
        },
      },
    },
  },
  {
    type: 'panel',
    panel: {
      label: 'Files',
      description: 'Browse open workflow files.',
      keywords: ['files', 'workflows'],
      placement: { surface: 'left', order: 2 },
      icon: FolderIcon,
      badge: undefined,
      railKey: 'workflow-files',
      railLabel: 'Files',
      railTitle: 'Files',
      surfaceEntry: {
        title: 'Files',
        body: {
          kind: 'workflow-files',
          content: () => <WorkflowFilesPanel />,
        },
      },
    },
  },
  {
    type: 'panel',
    panel: {
      label: 'Saved',
      description: 'Open starred tools and workflows.',
      keywords: ['bookmarks', 'favorites', 'starred'],
      placement: { surface: 'left', order: 3 },
      icon: BookmarkStarIcon,
      badge: undefined,
      railKey: 'saved-items',
      railLabel: 'Saved',
      railTitle: 'Saved',
      surfaceEntry: {
        title: 'Saved',
        body: [
          {
            kind: 'starred-tools',
            tabTitle: 'Tools',
            tabContent: (props) => <StarredToolsPanel {...props} />,
          },
          {
            kind: 'starred-workflows',
            tabTitle: 'Workflows',
            tabContent: () => <StarredWorkflowsPanel />,
          },
        ],
      },
    },
  },
  {
    type: 'panel',
    panel: {
      label: 'Tools',
      description: 'Inspect tools used by this workflow.',
      keywords: ['workflow', 'functions'],
      placement: { surface: 'drawer', order: 0 },
      icon: CubeIcon,
      badge: '4',
      railKey: 'workflow-tools',
      railLabel: 'Tools',
      railTitle: 'Tools',
      surfaceEntry: {
        title: 'Workflow data',
        body: [
          {
            kind: 'workflow-tools',
            tabTitle: 'Tools',
            tabContent: (props) => <WorkflowToolsPanel {...props} />,
          },
          {
            kind: 'workflow-variables',
            tabTitle: 'Variables',
            tabContent: (props) => <VariablesPanel {...props} />,
          },
        ],
      },
    },
  },
  {
    type: 'panel',
    panel: {
      label: 'Activities',
      description: 'Review current workflow activity.',
      keywords: ['current', 'execution', 'logs'],
      placement: { surface: 'drawer', order: 1 },
      railKey: 'workflow-activity',
      railLabel: 'Activities',
      railTitle: 'Activities',
      icon: LogIcon,
      badge: '6',
      surfaceEntry: {
        title: 'Current execution',
        body: {
          kind: 'workflow-activity',
          content: (props) => <CurrentExecutionPanel {...props} />,
        },
      },
    },
  },
  {
    type: 'panel',
    panel: {
      label: 'Runtime',
      description: 'Inspect live workflow runtime data.',
      keywords: ['execution', 'trace', 'variables'],
      placement: { surface: 'drawer', order: 2 },
      railKey: 'workflow-runtime',
      railLabel: 'Runtime',
      railTitle: 'Runtime',
      icon: RuntimeIcon,
      badge: undefined,
      surfaceEntry: {
        title: 'Runtime',
        body: {
          kind: 'workflow-runtime',
          content: (props) => <RuntimeTracePanel {...props} />,
        },
        bodyAttributes: {
          className: ['runtime-drawer-body'],
        },
      },
    },
  },
  {
    type: 'label',
    label: 'Node',
    placement: { surface: 'right', order: 0 },
  },
  {
    type: 'panel',
    panel: {
      label: 'Edit node',
      description: 'Edit the selected workflow node.',
      keywords: ['node', 'properties', 'arguments'],
      placement: { surface: 'right', order: 0 },
      railKey: 'node-editor',
      railLabel: 'Edit',
      railTitle: 'Edit node',
      icon: NodeEditIcon,
      surfaceEntry: {
        titleAttributes: {
          className: 'node-name'
        },
        title: ({ selectedNode, presentations, onNodeChange }: EditNodePanelTitleProps) => {
          const [editingTitle, setEditingTitle] = useState(false)
          const [titleDraft, setTitleDraft] = useState(selectedNode?.title ?? '')
          useEffect(() => {
            setEditingTitle(false)
            setTitleDraft(selectedNode?.title ?? '')
          }, [selectedNode?.id, selectedNode?.title])

          const commitTitle = () => {
            const title = titleDraft.trim()
            if (selectedNode && title) onNodeChange(selectedNode.id, { title })
            setEditingTitle(false)
          }

          return !selectedNode ? 'No node selected' : (<>
            <span className="node-icon panel-node-icon" title={`${selectedNode.toolId} / ${selectedNode.functionName}`}>
              <WorkflowNodeIcon descriptor={presentations.forNode(selectedNode)} size={24} />
            </span>{editingTitle ?
              <input aria-label="Node name" autoFocus className="node-name-input" onBlur={commitTitle} onChange={(event) =>
                setTitleDraft(event.target.value)} onKeyDown={(event) => {
                  if (event.key === 'Enter') event.currentTarget.blur()
                }} value={titleDraft} />
              : <button className="node-name-label" onClick={() => setEditingTitle(true)} type="button">{selectedNode.title}</button>}
            <button aria-label="Edit node name" className="node-name-edit" onClick={() => setEditingTitle(true)} title="Edit node name" type="button"><PencilIcon size={15} /></button>
          </>)
        },
        body: [
          {
            kind: 'node-editor',
            tabTitle: 'Workflow node',
            tabContent: (props) => !props.node ? <Empty message="Select a workflow node." /> : <NodeToolFunctionPanel {...props} />,
          },
          {
            kind: 'node-payload',
            tabTitle: 'Node payload',
            tabContent: (props) => !props.node ? <Empty message="Select a workflow node." /> : <NodePayloadPanel {...props} />,
          },
        ],
      },
    },
  },
  {
    type: 'separator',
    placement: { surface: 'right', order: 1 },
  },
  {
    type: 'label',
    label: 'Workflow',
    placement: { surface: 'right', order: 2 },
  },
  {
    type: 'panel',
    panel: {
      label: 'Preview workflow',
      description: 'Inspect workflow-level details.',
      keywords: ['workflow', 'preview', 'graph'],
      placement: { surface: 'right', order: 1 },
      railKey: 'workflow-preview',
      railLabel: 'Preview',
      railTitle: 'Preview workflow',
      icon: ReceiptCubeIcon,
      surfaceEntry: {
        title: 'Preview workflow',
        body: {
          kind: 'workflow-payload',
          content: (props) => <WorkflowDefinitionPanel {...props} />,
        },
      },
    },
  },
] as const satisfies readonly StudioRailItem[]

export const leftStudioRailPanels = studioRailPanels.filter(
  (item): item is Extract<typeof studioRailPanels[number], { type: 'panel'; panel: { placement: { surface: 'left' } } }> =>
    item.type === 'panel' && item.panel.placement.surface === 'left'
)
  .map((item) => item.panel)

export const drawerStudioRailPanels = studioRailPanels.filter(
  (item): item is Extract<typeof studioRailPanels[number], { type: 'panel'; panel: { placement: { surface: 'drawer' } } }> =>
    item.type === 'panel' && item.panel.placement.surface === 'drawer'
)
  .map((item) => item.panel)

export const rightStudioRailPanels = studioRailPanels.filter(
  (item): item is Extract<typeof studioRailPanels[number], { type: 'panel'; panel: { placement: { surface: 'right' } } }> =>
    item.type === 'panel' && item.panel.placement.surface === 'right'
)
  .map((item) => item.panel)

type ExtractBodyKind<P extends { surfaceEntry?: { body?: any } }> =
  NonNullable<P['surfaceEntry']>['body'] extends infer B
  ? B extends readonly any[]
  ? B[number]['kind']
  : B extends { kind: infer K }
  ? K
  : never
  : never

// Evaluates to: 'tool-catalog' | 'workspace-explorer' | 'workflow-files' | 'starred-tools' | 'starred-workflows'
export type LeftPanelViewContentKind = ExtractBodyKind<typeof leftStudioRailPanels[number]>
// Evaluates to: 'workflow-tools' | 'workflow-variables' | 'workflow-activity' | 'workflow-runtime'
export type DrawerPanelViewContentKind = ExtractBodyKind<typeof drawerStudioRailPanels[number]>
// Evaluates to: 'node-editor' | 'node-payload' | 'workflow-payload'
export type RightPanelViewContentKind = ExtractBodyKind<typeof rightStudioRailPanels[number]>

export type LeftPanelViewContentParams = Pick<PanelViewSetContentParams, LeftPanelViewContentKind>
export type DrawerPanelViewContentParams = Pick<PanelViewSetContentParams, DrawerPanelViewContentKind>
export type RightPanelViewContentParams = Pick<PanelViewSetContentParams, RightPanelViewContentKind>

export function getPanelContentKind(panel: StudioPanel): string {
  if (panel.surfaceEntry?.body) {
    if (Array.isArray(panel.surfaceEntry.body)) {
      return panel.surfaceEntry.body[0].kind
    }
    return panel.surfaceEntry.body.kind
  }
  return (panel as any).content ?? ''
}

const getItemSurface = (item: StudioRailItem): PanelSurface =>
  item.type === 'panel' ? item.panel.placement.surface : item.placement.surface

export const leftRailItems: readonly StudioRailItem[] = studioRailPanels.filter(
  (item) => getItemSurface(item) === 'left'
)

export const drawerRailItems: readonly StudioRailItem[] = studioRailPanels.filter(
  (item) => getItemSurface(item) === 'drawer'
)

export const rightRailItems: readonly StudioRailItem[] = studioRailPanels.filter(
  (item) => getItemSurface(item) === 'right'
)

export const studioPanelCatalog: readonly StudioRailItem[] = studioRailPanels

export function searchStudioPanels(query: string): StudioPanel[] {
  const panels = studioPanelCatalog
    .filter((item): item is { type: 'panel'; panel: StudioPanel } => item.type === 'panel')
    .map((item) => item.panel)
  const needle = query.trim().toLowerCase()
  if (!needle) return panels
  return panels.filter((panel) => [panel.label, panel.description, ...panel.keywords]
    .some((value) => value.toLowerCase().includes(needle)))
}

export function getSurfaceContentKinds(surface: PanelSurface): PanelViewContentKind[] {
  const panels = studioPanelCatalog
    .filter((item): item is { type: 'panel'; panel: StudioPanel } => item.type === 'panel' && item.panel.placement.surface === surface)
    .map((item) => item.panel)

  const kinds: PanelViewContentKind[] = []
  for (const panel of panels) {
    const body = panel.surfaceEntry?.body
    if (Array.isArray(body)) {
      for (const tab of body) {
        kinds.push(tab.kind as PanelViewContentKind)
      }
    } else if (body) {
      kinds.push(body.kind as PanelViewContentKind)
    }
  }
  return kinds
}

export function getPanelForContentKind(kind: PanelViewContentKind): StudioPanel | undefined {
  const panels = studioPanelCatalog
    .filter((item): item is { type: 'panel'; panel: StudioPanel } => item.type === 'panel')
    .map((item) => item.panel)

  return panels.find((candidate) => {
    const body = candidate.surfaceEntry?.body
    if (Array.isArray(body)) {
      return body.some((tab) => tab.kind === kind)
    }
    return body?.kind === kind
  })
}

export function getSurfaceForContentKind(kind: PanelViewContentKind): PanelSurface | undefined {
  const panel = getPanelForContentKind(kind)
  return panel?.placement.surface
}



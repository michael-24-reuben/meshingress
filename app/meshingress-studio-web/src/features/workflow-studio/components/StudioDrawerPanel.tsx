import type { DrawerPanelViewContentKind, DrawerPanelViewContentParams, SurfaceEntry } from '../types'
import { drawerStudioRailPanels, resolveSurfaceTab } from './panel-catalog'
import { SurfaceFrame } from './SurfaceFrame'
import { MinusIcon } from '../../../components/icons/node-icons'
import { Empty } from './elements/Empty'

interface WorkflowDrawerProps {
  drawer: DrawerPanelViewContentKind
  params: DrawerPanelViewContentParams
  onChange: (drawer: DrawerPanelViewContentKind) => void
  onHide: () => void
}

export function StudioDrawerPanel({ drawer, params, onChange, onHide }: WorkflowDrawerProps) {
  const panel = drawerStudioRailPanels.find((candidate) => {
    const body = candidate.surfaceEntry?.body
    if (Array.isArray(body)) {
      return body.some((tab) => tab.kind === drawer)
    }
    return body?.kind === drawer
  })

  if (!panel?.surfaceEntry) {
    return <SurfaceFrame content={{
      target: 'drawer',
      title: 'Drawer',
      actions: <button aria-label="Hide" className="panel-action-btn button small drawer-hide-btn" onClick={onHide} title="Hide" type="button"><MinusIcon size={16} /></button>,
      body: <Empty message="No drawer content is available." />,
    }} />
  }

  const surfaceEntry = panel.surfaceEntry as SurfaceEntry
  const bodyEntry = surfaceEntry.body
  const isTabs = Array.isArray(bodyEntry)
  const drawerParams = params[drawer as keyof DrawerPanelViewContentParams] as any
  const tabs = isTabs ? bodyEntry.map((tab) => resolveSurfaceTab<DrawerPanelViewContentKind>(tab, drawerParams)) : undefined
  const activeTabEntry = isTabs ? bodyEntry.find((tab) => tab.kind === drawer) : undefined
  const body = isTabs
    ? (activeTabEntry ? activeTabEntry.tabContent(params[drawer as keyof DrawerPanelViewContentParams] as any) : <Empty message="No drawer content is available." />)
    : (bodyEntry ? (bodyEntry as any).content(params[drawer as keyof DrawerPanelViewContentParams] as any) : <Empty message="No drawer content is available." />)

  const title = typeof surfaceEntry.title === 'function'
    ? surfaceEntry.title(params[drawer as keyof DrawerPanelViewContentParams] as any)
    : surfaceEntry.title

  return <SurfaceFrame content={{
    target: 'drawer',
    title,
    titleAttributes: surfaceEntry.titleAttributes,
    tabs,
    activeTab: isTabs ? drawer : undefined,
    onTabChange: isTabs ? onChange : undefined,
    actions: <button aria-label="Hide" className="panel-action-btn button small drawer-hide-btn" onClick={onHide} title="Hide" type="button"><MinusIcon size={16} /></button>,
    body,
    bodyAttributes: surfaceEntry.bodyAttributes,
    bodyKey: drawer,
  }} />
}

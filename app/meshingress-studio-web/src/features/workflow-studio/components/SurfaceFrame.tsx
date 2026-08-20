import type { ElementAttributeInput, ElementAttributesNormalized, SurfaceEntry } from '../types'
import { formatElementAttributes } from '../utilities'

type SurfaceTarget = 'left' | 'right' | 'drawer'

interface SurfaceTab<T extends string = string> {
  id: T
  label: string
  icon?: React.ComponentType<{ size?: number }>
}

export interface SurfaceContent<T extends string = string> {
  target: SurfaceTarget
  title: React.ReactNode
  titleAttributes?: SurfaceEntry['titleAttributes']
  tabs?: readonly SurfaceTab<T>[]
  activeTab?: T
  onTabChange?: (tab: T) => void
  actions?: React.ReactNode
  body: React.ReactNode
  bodyAttributes?: SurfaceEntry['bodyAttributes']
  bodyKey?: string
}

function SurfaceTabs<T extends string>({ content }: { content: SurfaceContent<T> }) {
  if (!content.tabs?.length) return null

  return <div className="tabs">
    {content.tabs.map((tab) => {
      const Icon = tab.icon
      return (
        <button
          className={`tab${content.activeTab === tab.id ? ' active' : ''}`}
          key={tab.id}
          onClick={() => content.onTabChange?.(tab.id)}
          type="button"
        >
          {Icon && <span className="tab-icon"><Icon size={14} /></span>}
          <span>{tab.label}</span>
        </button>
      )
    })}
  </div>
}

export function SurfaceFrame<T extends string>({ content }: { content: SurfaceContent<T> }) {
  const tabs = <SurfaceTabs content={content} />
  const defaultBodyClassName = content.target === 'drawer' ? 'drawer-body' : 'panel-body'
  const defaultTitleClassName = content.target === 'drawer' ? 'drawer-title' : 'panel-title'
  const bodyProps = formatElementAttributes({ className: defaultBodyClassName }, content.bodyAttributes)
  const titleProps = formatElementAttributes({ className: defaultTitleClassName }, content.titleAttributes)

  if (content.target === 'drawer') {
    return <section className="drawer">
      <div className="drawer-header">
        <div {...titleProps}>{content.title}</div>
        {tabs}
        {content.actions && <div className="drawer-header-actions">{content.actions}</div>}
      </div>
      <div {...bodyProps} key={content.bodyKey}>{content.body}</div>
    </section>
  }

  const panelClassName = content.target === 'left' ? 'left-panel' : 'right-panel'
  return <aside className={panelClassName}>
    <div className="panel-head">
      <div {...titleProps}>{content.title}</div>
      {content.actions && <div className="panel-head-actions hbox panel-actions">{content.actions}</div>}
    </div>
    {tabs}
    <div {...bodyProps} key={content.bodyKey}>{content.body}</div>
  </aside>
}

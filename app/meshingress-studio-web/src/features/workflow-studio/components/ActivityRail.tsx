import type { StudioView } from '../types'

const top = [{ view: 'files', icon: '▣', label: 'Files', badge: '4' }, { view: 'tools', icon: 'T', label: 'Tools', badge: '12' }]
const bottom = [{ view: 'used', icon: 'T', label: 'Used', badge: '4' }, { view: 'history', icon: 'L', label: 'Logs', badge: '6' }, { view: 'current', icon: '▶', label: 'Run' }]

export function ActivityRail({ view, onChange }: { view: StudioView; onChange: (view: StudioView) => void }) {
  const renderGroup = (items: typeof top | typeof bottom) => <div className="rail-group">{items.map((item) => <button className={`rail-button${view === item.view ? ' active' : ''}`} key={item.view} onClick={() => onChange(item.view as StudioView)} title={item.label} type="button"><span className="rail-icon">{item.icon}</span><span className="rail-label">{item.label}</span>{item.badge && <span className="rail-badge">{item.badge}</span>}</button>)}</div>
  return <nav aria-label="Activity bar" className="rail">{renderGroup(top)}{renderGroup(bottom)}</nav>
}

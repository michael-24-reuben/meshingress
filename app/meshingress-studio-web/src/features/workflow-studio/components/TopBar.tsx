import { useEffect, useRef, useState } from 'react'
import { CodeSquareFilledIcon } from '../../../components/icons/node-icons'

type Command = { label: string; separatorBefore?: boolean }

const menus: Array<{ label: string; commands: Command[] }> = [
  { label: 'File', commands: [{ label: 'New workflow' }, { label: 'Open published workflow' }, { label: 'Import definition' }, { label: 'Save draft', separatorBefore: true }, { label: 'Publish workflow' }, { label: 'Export JSON' }] },
  { label: 'Edit', commands: [{ label: 'Undo' }, { label: 'Redo' }, { label: 'Copy selection', separatorBefore: true }, { label: 'Paste' }, { label: 'Duplicate node', separatorBefore: true }, { label: 'Delete selection' }] },
  { label: 'View', commands: [{ label: 'Zoom in' }, { label: 'Zoom out' }, { label: 'Fit workflow', separatorBefore: true }, { label: 'Reset canvas' }, { label: 'Toggle grid', separatorBefore: true }, { label: 'Snap to grid' }] },
  { label: 'Run', commands: [{ label: 'Validate workflow' }, { label: 'Run workflow' }, { label: 'Debug run' }, { label: 'Stop run' }, { label: 'Run history', separatorBefore: true }, { label: 'Clear execution logs' }] },
  { label: 'Help', commands: [{ label: 'Workflow guide' }, { label: 'Tool catalog' }, { label: 'Keyboard shortcuts' }, { label: 'MCP connection status', separatorBefore: true }, { label: 'Report issue' }, { label: 'About Meshingress' }] },
]

interface TopBarProps {
  workflowName: string
  running: boolean
  onRun: () => void
  onValidate: () => void
  onStatus: (message: string) => void
}

export function TopBar({ workflowName, running, onRun, onValidate, onStatus }: TopBarProps) {
  const [openMenu, setOpenMenu] = useState<string | null>(null)
  const [openAction, setOpenAction] = useState<'run' | 'runtime' | null>(null)
  const [brandHovered, setBrandHovered] = useState(false)
  const headerRef = useRef<HTMLElement>(null)
  const brandRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const onPointerDown = (event: PointerEvent) => {
      if (!brandRef.current?.contains(event.target as Node)) setOpenMenu(null)
      if (!headerRef.current?.contains(event.target as Node)) setOpenAction(null)
    }
    document.addEventListener('pointerdown', onPointerDown)
    return () => document.removeEventListener('pointerdown', onPointerDown)
  }, [])

  const execute = (label: string) => {
    setOpenMenu(null)
    if (label === 'Save draft') onStatus('Draft saved')
    else if (label === 'Validate workflow') onValidate()
    else if (label === 'Run workflow') onRun()
    else onStatus(`${label} is ready to connect.`)
  }

  const updateBrandHover = (event: React.PointerEvent<HTMLDivElement>) => {
    const bounds = event.currentTarget.getBoundingClientRect()
    const withinVisibleBar = event.clientX >= bounds.left && event.clientX <= bounds.right
      && event.clientY >= bounds.top && event.clientY <= bounds.bottom
    setBrandHovered(withinVisibleBar)
  }

  return (
    <header className="topbar" ref={headerRef}>
      <div
        className={`brand-wrap${brandHovered ? ' is-hovered' : ''}${openMenu ? ' menu-locked' : ''}`}
        onPointerEnter={updateBrandHover}
        onPointerLeave={() => setBrandHovered(false)}
        onPointerMove={updateBrandHover}
        ref={brandRef}
      >
        <div className="brand">M</div>
        <div className="brand-switcher">
          <div className="brand-switcher-track">
            <div className="title-wrap">
              <div className="title">{workflowName}</div>
              <div className="subtitle">Meshingress workflow designer</div>
            </div>
            <nav aria-label="Meshingress menu" className="brand-menu">
              {menus.map((menu) => (
                <div className={`brand-menu-entry${openMenu === menu.label ? ' is-open' : ''}`} key={menu.label} onMouseEnter={() => { setOpenAction(null); setOpenMenu(menu.label) }}>
                  <span className="brand-menu-item">{menu.label}</span>
                  <div aria-label={`${menu.label} commands`} className="brand-menu-children">
                    {menu.commands.map((command) => (
                      <span key={command.label}>
                        {command.separatorBefore && <span className="brand-menu-separator" role="separator" />}
                        <button className="brand-menu-child" onClick={() => execute(command.label)} type="button"><CodeSquareFilledIcon size={16} /><span>{command.label}</span></button>
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </nav>
          </div>
        </div>
      </div>

      <div className="actions">
        <HeaderMenu id="run-options-menu" open={openAction === 'run'} onToggle={() => { setOpenMenu(null); setOpenAction(openAction === 'run' ? null : 'run') }} toggle={<span className="header-action-chevron">▾</span>} label="Open run options" split primary>
          <button className="header-icon-button primary" disabled={running} onClick={onRun} title={`Run ${workflowName}`} type="button"><CodeSquareFilledIcon className="header-action-icon" size={16} /></button>
          <button className="header-action-menu-item" onClick={onValidate} role="menuitem" type="button">Validate workflow</button>
          <button className="header-action-menu-item" onClick={onRun} role="menuitem" type="button">Run from Manual Trigger</button>
          <span className="header-action-menu-separator" role="separator" />
          <button className="header-action-menu-item" onClick={() => onStatus('Debug run options are ready to configure.')} role="menuitem" type="button">Debug run</button>
          <button className="header-action-menu-item" onClick={() => onStatus('Execution history is available in the Logs rail.')} role="menuitem" type="button">Execution history</button>
        </HeaderMenu>
        <HeaderMenu id="runtime-options-menu" open={openAction === 'runtime'} onToggle={() => { setOpenMenu(null); setOpenAction(openAction === 'runtime' ? null : 'runtime') }} toggle={<span className="header-action-ellipsis">…</span>} label="Open runtime configurations">
          {['Edit configurations', 'Environment variables', 'Runtime defaults', 'Permissions and scopes', 'View runtime details'].map((command, index) => (
            <span key={command}>
              {index === 4 && <span className="header-action-menu-separator" role="separator" />}
              <button className="header-action-menu-item" onClick={() => onStatus(`${command} is ready to connect.`)} role="menuitem" type="button">{command}</button>
            </span>
          ))}
        </HeaderMenu>
      </div>
    </header>
  )
}

interface HeaderMenuProps { id: string; open: boolean; onToggle: () => void; toggle: React.ReactNode; label: string; split?: boolean; primary?: boolean; children: React.ReactNode }

function HeaderMenu({ id, open, onToggle, toggle, label, split, primary, children }: HeaderMenuProps) {
  return <div className={`header-action-menu${open ? ' is-open' : ''}`}>
    <div className={split ? 'header-action-split' : undefined}>
      {split && (children as React.ReactNode[])[0]}
      <button aria-controls={id} aria-expanded={open} aria-haspopup="menu" aria-label={label} className={`header-icon-button${primary ? ' primary' : ''}`} onClick={onToggle} type="button">{toggle}</button>
    </div>
    <div aria-label={label} className="header-action-menu-popover" id={id} role="menu">{split ? (children as React.ReactNode[]).slice(1) : children}</div>
  </div>
}

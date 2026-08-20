import { useEffect, useRef, useState, type ComponentType } from 'react'
import { CodeSquareFilledIcon, CopyIcon, PanelSearchIcon, RemoveIcon, WorkflowFolderIcon, WorkflowIcon, type CodeSquareFilledIconProps } from '../../../components/icons/node-icons'
import { ProfileMenu, type StudioProfile } from './ProfileMenu'

export type Command = {
  type?: 'command'
  label: string
  shortcut?: string
  icon?: ComponentType<CodeSquareFilledIconProps>
  action?: () => void
}

export type Separator = {
  type: 'separator'
}

export type MenuItem = Command | Separator

export type Menu = {
  label: string
  items: MenuItem[]
}

const menus: Menu[] = [
  {
    label: 'File',
    items: [
      { label: 'New workspace', shortcut: 'Ctrl+W' },
      { label: 'Open workspace', shortcut: 'Ctrl+O', icon: WorkflowFolderIcon },
      { label: 'Open recent' },
      { type: 'separator' },
      { label: 'New workflow', shortcut: 'Ctrl+N', icon: WorkflowIcon },
      { label: 'Open published workflow', shortcut: 'Ctrl+Alt+O' },
      { label: 'Import definition', shortcut: 'Ctrl+Alt+I' },
      { type: 'separator' },
      { label: 'Save draft', shortcut: 'Ctrl+Shift+S' },
      { label: 'Publish workflow', shortcut: 'Ctrl+Shift+P' },
      { label: 'Export JSON', shortcut: 'Ctrl+Shift+E' },
    ],
  },
  {
    label: 'Edit',
    items: [
      { label: 'Undo', shortcut: 'Ctrl+Z' },
      { label: 'Redo', shortcut: 'Ctrl+Y' },
      { type: 'separator' },
      { label: 'Copy selection', shortcut: 'Ctrl+C', icon: CopyIcon },
      { label: 'Paste', shortcut: 'Ctrl+V' },
      { type: 'separator' },
      { label: 'Duplicate node', shortcut: 'Ctrl+D' },
      { label: 'Delete selection', shortcut: 'Delete', icon: RemoveIcon },
    ],
  },
  {
    label: 'View',
    items: [
      { label: 'Search panels', shortcut: 'Ctrl+K', icon: PanelSearchIcon },
      { type: 'separator' },
      { label: 'Zoom in' },
      { label: 'Zoom out' },
      { type: 'separator' },
      { label: 'Fit workflow' },
      { label: 'Reset canvas' },
      { type: 'separator' },
      { label: 'Toggle grid' },
      { label: 'Snap to grid' },
    ],
  },
  {
    label: 'Run',
    items: [
      { label: 'Validate workflow' },
      { label: 'Run workflow' },
      { label: 'Debug run' },
      { label: 'Stop run' },
      { type: 'separator' },
      { label: 'Run history' },
      { label: 'Clear execution logs' },
    ],
  },
  {
    label: 'Help',
    items: [
      { label: 'Workflow guide' },
      { label: 'Tool catalog' },
      { label: 'Keyboard shortcuts' },
      { type: 'separator' },
      { label: 'MCP connection status' },
      { label: 'Report issue' },
      { label: 'About Meshingress' },
    ],
  },
]

interface TopBarProps {
  workflowName: string
  running?: boolean
  onRun: () => void
  onValidate: () => void
  onStatus: (message: string) => void
  onSearchPanels: () => void
  onCopySelection?: () => void
  onCutSelection?: () => void
  onPaste?: () => void
  onDuplicateNode?: () => void
  onDeleteSelection?: () => void
  onUndo?: () => void
  onRedo?: () => void
  onNewWorkspace?: () => void
  onOpenWorkspace?: () => void
  onOpenRecent?: () => void
  googleClientId: string
  profile: StudioProfile | null
  onGoogleCredential: (credential: string) => void
  onGoogleError: (message: string) => void
  onNativeValidate: (username: string, password: string) => Promise<{ accepted: boolean; message: string; identifierFingerprint: string | null }>
  nativeValidationAvailable: boolean
  nativeValidationNotice: string
  onSignOut: () => void
}

export function TopBar({
  workflowName,
  onRun,
  onValidate,
  onStatus,
  onSearchPanels,
  onCopySelection,
  onCutSelection,
  onPaste,
  onDuplicateNode,
  onDeleteSelection,
  onUndo,
  onRedo,
  onNewWorkspace,
  onOpenWorkspace,
  onOpenRecent,
  googleClientId,
  profile,
  onGoogleCredential,
  onGoogleError,
  onNativeValidate,
  nativeValidationAvailable,
  nativeValidationNotice,
  onSignOut,
}: TopBarProps) {
  const [openMenu, setOpenMenu] = useState<string | null>(null)
  const [brandHovered, setBrandHovered] = useState(false)
  const headerRef = useRef<HTMLElement>(null)
  const brandRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const onPointerDown = (event: PointerEvent) => {
      if (!brandRef.current?.contains(event.target as Node)) setOpenMenu(null)
    }
    document.addEventListener('pointerdown', onPointerDown)
    return () => document.removeEventListener('pointerdown', onPointerDown)
  }, [])

  const execute = (item: Command) => {
    setOpenMenu(null)
    if (item.action) {
      item.action()
      return
    }
    const label = item.label
    if (label === 'New workspace') {
      onNewWorkspace?.()
    } else if (label === 'Open workspace') {
      onOpenWorkspace?.()
    } else if (label === 'Open recent') {
      onOpenRecent?.()
    } else if (label === 'Search panels') {
      onSearchPanels()
    } else if (label === 'Save draft') {
      onStatus('Draft saved')
    } else if (label === 'Validate workflow') {
      onValidate()
    } else if (label === 'Run workflow') {
      onRun()
    } else if (label === 'Copy selection') {
      if (onCopySelection) onCopySelection()
      else window.dispatchEvent(new CustomEvent('workflow-node-command', { detail: 'copy' }))
    } else if (label === 'Cut selection') {
      if (onCutSelection) onCutSelection()
      else window.dispatchEvent(new CustomEvent('workflow-node-command', { detail: 'cut' }))
    } else if (label === 'Paste') {
      if (onPaste) onPaste()
      else window.dispatchEvent(new CustomEvent('workflow-node-command', { detail: 'paste' }))
    } else if (label === 'Duplicate node') {
      if (onDuplicateNode) onDuplicateNode()
      else window.dispatchEvent(new CustomEvent('workflow-node-command', { detail: 'duplicate' }))
    } else if (label === 'Delete selection') {
      if (onDeleteSelection) onDeleteSelection()
      else window.dispatchEvent(new CustomEvent('workflow-node-command', { detail: 'delete' }))
    } else if (label === 'Undo') {
      if (onUndo) onUndo()
      else window.dispatchEvent(new KeyboardEvent('keydown', { key: 'z', ctrlKey: true, bubbles: true }))
    } else if (label === 'Redo') {
      if (onRedo) onRedo()
      else window.dispatchEvent(new KeyboardEvent('keydown', { key: 'y', ctrlKey: true, bubbles: true }))
    } else {
      onStatus(`${label} is ready to connect.`)
    }
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
                <div
                  className={`brand-menu-entry${openMenu === menu.label ? ' is-open' : ''}`}
                  key={menu.label}
                  onMouseEnter={() => {
                    if (openMenu !== null) {
                      setOpenMenu(menu.label)
                    }
                  }}
                >
                  <button
                    className="brand-menu-item"
                    onClick={() => setOpenMenu((prev) => (prev === menu.label ? null : menu.label))}
                    type="button"
                  >
                    {menu.label}
                  </button>
                  <div aria-label={`${menu.label} commands`} className="brand-menu-children">
                    {menu.items.map((item, index) => {
                      if ('type' in item && item.type === 'separator') {
                        return <span className="brand-menu-separator" key={`sep-${index}`} role="separator" />
                      }
                      const Icon = item.icon ?? CodeSquareFilledIcon
                      return (
                        <button
                          className={`brand-menu-child${item.shortcut ? ' has-shortcut' : ''}`}
                          key={item.label}
                          onClick={() => execute(item)}
                          type="button"
                        >
                          <Icon size={16} />
                          <span>{item.label}</span>
                          {item.shortcut && <span className="brand-menu-shortcut">{item.shortcut}</span>}
                        </button>
                      )
                    })}
                  </div>
                </div>
              ))}
            </nav>
          </div>
        </div>
      </div>
      <div className="topbar-right-container">
        <ProfileMenu
          googleClientId={googleClientId}
          onGoogleCredential={onGoogleCredential}
          onGoogleError={onGoogleError}
          onNativeValidate={onNativeValidate}
          onSignOut={onSignOut}
          profile={profile}
          nativeValidationAvailable={nativeValidationAvailable}
          nativeValidationNotice={nativeValidationNotice}
        />
      </div>
    </header>
  )
}

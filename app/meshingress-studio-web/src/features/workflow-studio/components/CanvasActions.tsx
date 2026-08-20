import { useEffect, useRef, useState } from 'react'
import {
  CodeSquareFilledIcon,
  DebugConsoleIcon,
  DebugRunIcon,
  EllipsisHorizontalIcon,
  InfoIcon,
  PlayIcon,
  ReloadIcon,
  SettingsIcon,
  TickIcon,
} from '../../../components/icons/node-icons'

interface CanvasActionsProps {
  running: boolean
  onRun: () => void
  onValidate: () => void
  onStatus: (message: string) => void
  reattachOnEmptyRelease?: boolean
  onToggleReattachOnEmptyRelease?: () => void
}

type RevealAction = 'run' | 'debug'

const REVEAL_COLLAPSE_DELAY_MS = 1500

export function CanvasActions({ running, onRun, onValidate, onStatus, reattachOnEmptyRelease = false, onToggleReattachOnEmptyRelease }: CanvasActionsProps) {
  const [open, setOpen] = useState(false)
  const [expandedActions, setExpandedActions] = useState<Record<RevealAction, boolean>>({ run: false, debug: false })
  const menuRef = useRef<HTMLDivElement>(null)
  const collapseTimers = useRef<Partial<Record<RevealAction, ReturnType<typeof setTimeout>>>>({})

  useEffect(() => {
    const timers = collapseTimers.current
    const onPointerDown = (event: PointerEvent) => {
      if (!menuRef.current?.contains(event.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('pointerdown', onPointerDown)
    return () => {
      document.removeEventListener('pointerdown', onPointerDown)
      Object.values(timers).forEach((timer) => {
        if (timer !== undefined) clearTimeout(timer)
      })
    }
  }, [])

  const revealAction = (action: RevealAction) => {
    const canvasEl = menuRef.current?.closest('.canvas')
    if (canvasEl && canvasEl.getBoundingClientRect().width <= 750) {
      return
    }
    const pendingTimer = collapseTimers.current[action]
    if (pendingTimer !== undefined) {
      clearTimeout(pendingTimer)
      delete collapseTimers.current[action]
    }
    setExpandedActions((current) => current[action] ? current : { ...current, [action]: true })
  }

  const scheduleActionCollapse = (action: RevealAction) => {
    const pendingTimer = collapseTimers.current[action]
    if (pendingTimer !== undefined) clearTimeout(pendingTimer)
    collapseTimers.current[action] = setTimeout(() => {
      setExpandedActions((current) => current[action] ? { ...current, [action]: false } : current)
      delete collapseTimers.current[action]
    }, REVEAL_COLLAPSE_DELAY_MS)
  }

  const options = [
    { label: reattachOnEmptyRelease ? 'Edge release: Reattach back' : 'Edge release: Remove path (Default)', icon: SettingsIcon },
    { label: 'Edit configurations', icon: SettingsIcon },
    { label: 'Environment variables', icon: CodeSquareFilledIcon },
    { label: 'Runtime defaults', icon: PlayIcon },
    { label: 'Permissions and scopes', icon: InfoIcon },
    { label: 'Validate workflow', icon: TickIcon },
    { label: 'Execution history', icon: ReloadIcon },
    { label: 'View runtime details', icon: DebugConsoleIcon },
  ]

  const handleOptionClick = (command: string) => {
    setOpen(false)
    if (command.startsWith('Edge release:')) {
      onToggleReattachOnEmptyRelease?.()
    } else if (command === 'Validate workflow') {
      onValidate()
    } else {
      onStatus(`${command} is ready to connect.`)
    }
  }

  return (
    <div className="canvas-overlay-actions" onPointerDown={(event) => event.stopPropagation()}>
      <button
        aria-label="Run workflow"
        className={`canvas-action-btn run-workflow expand-on-hover${expandedActions.run ? ' is-expanded' : ''}`}
        disabled={running}
        onClick={onRun}
        onFocus={() => revealAction('run')}
        onBlur={() => scheduleActionCollapse('run')}
        onPointerEnter={() => revealAction('run')}
        onPointerLeave={() => scheduleActionCollapse('run')}
        title="Run workflow"
        type="button"
      >
        <PlayIcon size={16} />
        <span>Run workflow</span>
      </button>
      <button
        aria-label="Debug run"
        className={`canvas-action-btn debug-run expand-on-hover${expandedActions.debug ? ' is-expanded' : ''}`}
        onClick={() => onStatus('Debug run options are ready to configure.')}
        onFocus={() => revealAction('debug')}
        onBlur={() => scheduleActionCollapse('debug')}
        onPointerEnter={() => revealAction('debug')}
        onPointerLeave={() => scheduleActionCollapse('debug')}
        title="Debug run"
        type="button"
      >
        <DebugRunIcon size={16} />
        <span>Debug run</span>
      </button>

      <div className={`header-action-menu${open ? ' is-open' : ''}`} ref={menuRef}>
        <button
          aria-controls="runtime-options-menu"
          aria-expanded={open}
          aria-haspopup="menu"
          aria-label="Open runtime configurations"
          className="header-icon-button"
          onClick={() => setOpen((current) => !current)}
          title="Open runtime configurations"
          type="button"
        >
          <EllipsisHorizontalIcon size={16} />
        </button>
        <div
          aria-label="Open runtime configurations"
          className="header-action-menu-popover"
          id="runtime-options-menu"
          role="menu"
        >
          {options.map((option, index) => {
            const Icon = option.icon
            return (
              <span key={option.label}>
                {index === 6 && <span className="header-action-menu-separator" role="separator" />}
                <button
                  className="header-action-menu-item"
                  onClick={() => handleOptionClick(option.label)}
                  role="menuitem"
                  type="button"
                >
                  <Icon size={16} />
                  <span>{option.label}</span>
                </button>
              </span>
            )
          })}
        </div>
      </div>
    </div>
  )
}

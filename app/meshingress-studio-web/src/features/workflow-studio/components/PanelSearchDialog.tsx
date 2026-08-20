import { useEffect, useMemo, useRef, useState } from 'react'
import { CrossIcon, SearchZoomIcon } from '../../../components/icons/node-icons'
import { searchStudioPanels, type StudioPanel } from './panel-catalog'

export function PanelSearchDialog({ onClose, onSelect }: { onClose: () => void; onSelect: (panel: StudioPanel) => void }) {
  const [query, setQuery] = useState('')
  const inputRef = useRef<HTMLInputElement>(null)
  const matches = useMemo(() => searchStudioPanels(query), [query])

  useEffect(() => {
    inputRef.current?.focus()
  }, [])

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [onClose])

  return <div className="panel-search-backdrop" onMouseDown={onClose}>
    <section aria-labelledby="panel-search-title" aria-modal="true" className="panel-search-dialog" onMouseDown={(event) => event.stopPropagation()} role="dialog">
      <header className="panel-search-header">
        <h2 id="panel-search-title">Search panels</h2>
        <button aria-label="Close panel search" className="panel-search-close" onClick={onClose} type="button"><CrossIcon size={16} /></button>
      </header>
      <label className="panel-search-input-wrap">
        <span className="sr-only">Search panels</span>
        <SearchZoomIcon aria-hidden="true" size={16} />
        <input autoComplete="off" onChange={(event) => setQuery(event.target.value)} placeholder="Search panels..." ref={inputRef} type="search" value={query} />
      </label>
      <div aria-live="polite" className="panel-search-results">
        {matches.length ? matches.map((panel) => <button className="panel-search-result" key={panel.railKey} onClick={() => onSelect(panel)} type="button">
          <span className="panel-search-result-copy"><strong>{panel.label}</strong><span>{panel.description}</span></span>
          <span className="panel-search-result-surface">{panel.placement.surface}</span>
        </button>) : <p className="panel-search-empty">No panels match “{query}”.</p>}
      </div>
    </section>
  </div>
}

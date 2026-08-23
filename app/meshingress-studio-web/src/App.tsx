import { useState, useEffect } from 'react'
import { WorkflowStudioPage } from './features/workflow-studio/WorkflowStudioPage'
import { SampleWorkflowTestPage } from './features/workflow-studio/SampleWorkflowTestPage'

export default function App() {
  const [view, setView] = useState<'studio' | 'test'>(() => {
    if (typeof window === 'undefined') return 'studio'
    const params = new URLSearchParams(window.location.search)
    return params.get('page') === 'test' || params.get('view') === 'test' ? 'test' : 'test' // default to test for quick testing
  })

  useEffect(() => {
    const handlePopState = () => {
      const params = new URLSearchParams(window.location.search)
      const isTest = params.get('page') === 'test' || params.get('view') === 'test'
      setView(isTest ? 'test' : 'studio')
    }
    window.addEventListener('popstate', handlePopState)
    return () => window.removeEventListener('popstate', handlePopState)
  }, [])

  const switchView = (nextView: 'studio' | 'test') => {
    setView(nextView)
    const url = new URL(window.location.href)
    if (nextView === 'test') {
      url.searchParams.set('view', 'test')
    } else {
      url.searchParams.delete('view')
      url.searchParams.delete('page')
    }
    window.history.pushState({}, '', url.toString())
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', width: '100vw', height: '100vh', overflow: 'hidden' }}>
      {/* Top Testing Switcher Bar */}
      <header
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '4px 12px',
          backgroundColor: '#18181b',
          color: '#e4e4e7',
          fontSize: '12px',
          borderBottom: '1px solid #27272a',
          zIndex: 9999,
          flexShrink: 0,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ fontWeight: 600, color: '#38bdf8' }}>Meshingress Studio</span>
          <span style={{ color: '#71717a' }}>/</span>
          <span style={{ color: '#d4d4d8' }}>
            {view === 'test' ? 'Sample Workflow Live Test Page' : 'Workflow Studio'}
          </span>
        </div>

        <div style={{ display: 'flex', gap: '6px' }}>
          <button
            onClick={() => switchView('test')}
            style={{
              padding: '3px 10px',
              borderRadius: '4px',
              fontSize: '11px',
              fontWeight: 500,
              cursor: 'pointer',
              border: view === 'test' ? '1px solid #3b82f6' : '1px solid #3f3f46',
              backgroundColor: view === 'test' ? '#1e3a8a' : '#27272a',
              color: view === 'test' ? '#ffffff' : '#a1a1aa',
            }}
          >
            ⚡ Sample Workflow Test Page
          </button>
          <button
            onClick={() => switchView('studio')}
            style={{
              padding: '3px 10px',
              borderRadius: '4px',
              fontSize: '11px',
              fontWeight: 500,
              cursor: 'pointer',
              border: view === 'studio' ? '1px solid #3b82f6' : '1px solid #3f3f46',
              backgroundColor: view === 'studio' ? '#1e3a8a' : '#27272a',
              color: view === 'studio' ? '#ffffff' : '#a1a1aa',
            }}
          >
            Studio Workbench
          </button>
        </div>
      </header>

      {/* Main Page Body */}
      <div style={{ flex: 1, minHeight: 0, position: 'relative', overflow: 'hidden' }}>
        {view === 'test' ? (
          <SampleWorkflowTestPage onBackToStudio={() => switchView('studio')} />
        ) : (
          <WorkflowStudioPage />
        )}
      </div>
    </div>
  )
}

import { useState } from 'react'

export interface InitiatorLinkProps {
  nodeId: string
  path: string
  onSelectNode: (nodeId: string) => boolean
}

function formatInitiator(nodeId: string, path: string): string {
  return `${nodeId}:/${path.replace(/^\/+/, '')}`
}

export function InitiatorLink({ nodeId, path, onSelectNode }: InitiatorLinkProps) {
  const [selectionFailed, setSelectionFailed] = useState(false)
  const initiator = formatInitiator(nodeId, path)

  return (
    <button
      aria-label={`Select initiator node ${nodeId}`}
      className={`initiator-link${selectionFailed ? ' selection-failed' : ''}`}
      onClick={() => setSelectionFailed(!onSelectNode(nodeId))}
      title={selectionFailed ? `Node ${nodeId} no longer exists in this workflow.` : initiator}
      type="button"
    >
      {initiator}
    </button>
  )
}

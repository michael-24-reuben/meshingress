import { useEffect, useRef, useState } from 'react'
import { CrossIcon, WorkflowFolderIcon } from '../../../components/icons/node-icons'
import type { DirectorySelection } from '../storage/local-workspace'

export function WorkspaceDialog({
  onClose,
  onCreate,
  onChooseDirectory,
}: {
  onClose: () => void
  onCreate: (name: string, selection: DirectorySelection) => Promise<void>
  onChooseDirectory: () => Promise<DirectorySelection | null>
}) {
  const [name, setName] = useState('')
  const [selection, setSelection] = useState<DirectorySelection | null>(null)
  const [isChoosing, setIsChoosing] = useState(false)
  const [isCreating, setIsCreating] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const nameInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    nameInputRef.current?.focus()
  }, [])

  const chooseDirectory = async () => {
    setIsChoosing(true)
    try {
      const nextSelection = await onChooseDirectory()
      if (nextSelection) {
        setSelection(nextSelection)
        setName((current) => current.trim() ? current : nextSelection.name)
        setError(null)
      }
    } finally {
      setIsChoosing(false)
    }
  }

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!selection || !name.trim()) return
    setIsCreating(true)
    setError(null)
    try {
      await onCreate(name.trim(), selection)
    } catch (creationError) {
      setError(creationError instanceof Error ? creationError.message : 'Could not create the local workspace.')
    } finally {
      setIsCreating(false)
    }
  }

  return <div aria-modal="true" className="workspace-dialog-backdrop" role="dialog" aria-labelledby="workspace-dialog-title">
    <form className="workspace-dialog" onSubmit={submit}>
      <div className="workspace-dialog-header">
        <div>
          <h2 id="workspace-dialog-title">New workspace</h2>
          <p>Choose a parent folder. Studio creates a named local project inside it.</p>
        </div>
        <button aria-label="Close new workspace dialog" className="workspace-dialog-close" onClick={onClose} type="button"><CrossIcon size={16} /></button>
      </div>
      <label className="workspace-dialog-field">
        <span>Workspace name</span>
        <input onChange={(event) => setName(event.target.value)} placeholder="My workspace" ref={nameInputRef} value={name} />
      </label>
      <label className="workspace-dialog-field">
        <span>Create in</span>
        <span className="workspace-directory-input">
          <input aria-label="Workspace parent folder" placeholder="Choose a parent folder" readOnly value={selection?.name ?? ''} />
          <button aria-label="Choose workspace parent folder" className="workspace-directory-picker" disabled={isChoosing || isCreating} onClick={() => void chooseDirectory()} title="Choose parent folder" type="button"><WorkflowFolderIcon size={17} /></button>
        </span>
        <small>Studio creates <code>&lt;folder&gt;/&lt;workspace name&gt;</code> and keeps the folder permission on this device only.</small>
      </label>
      {error && <p className="workspace-dialog-error" role="alert">{error}</p>}
      <div className="workspace-dialog-actions">
        <button className="button small" disabled={isCreating} onClick={onClose} type="button">Cancel</button>
        <button className="button primary small" disabled={!selection || !name.trim() || isCreating} type="submit">{isCreating ? 'Creating workspace…' : 'Create workspace'}</button>
      </div>
    </form>
  </div>
}

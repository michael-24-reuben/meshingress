import { useEffect, useRef, useState } from 'react'
import { CrossIcon, WorkflowFolderIcon } from '../../../components/icons/node-icons'
import type { DirectorySelection } from '../local-workspace'

export function WorkspaceDialog({
  onClose,
  onCreate,
  onChooseDirectory,
}: {
  onClose: () => void
  onCreate: (name: string, selection: DirectorySelection) => void
  onChooseDirectory: () => Promise<DirectorySelection | null>
}) {
  const [name, setName] = useState('')
  const [selection, setSelection] = useState<DirectorySelection | null>(null)
  const [isChoosing, setIsChoosing] = useState(false)
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
      }
    } finally {
      setIsChoosing(false)
    }
  }

  const submit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!selection || !name.trim()) return
    onCreate(name.trim(), selection)
  }

  return <div aria-modal="true" className="workspace-dialog-backdrop" role="dialog" aria-labelledby="workspace-dialog-title">
    <form className="workspace-dialog" onSubmit={submit}>
      <div className="workspace-dialog-header">
        <div>
          <h2 id="workspace-dialog-title">New workspace</h2>
          <p>Choose a local folder. Studio will not create or write files yet.</p>
        </div>
        <button aria-label="Close new workspace dialog" className="workspace-dialog-close" onClick={onClose} type="button"><CrossIcon size={16} /></button>
      </div>
      <label className="workspace-dialog-field">
        <span>Workspace name</span>
        <input onChange={(event) => setName(event.target.value)} placeholder="My workspace" ref={nameInputRef} value={name} />
      </label>
      <label className="workspace-dialog-field">
        <span>Save to</span>
        <span className="workspace-directory-input">
          <input aria-label="Workspace folder" placeholder="Choose a folder" readOnly value={selection?.name ?? ''} />
          <button aria-label="Choose workspace folder" className="workspace-directory-picker" disabled={isChoosing} onClick={() => void chooseDirectory()} title="Choose folder" type="button"><WorkflowFolderIcon size={17} /></button>
        </span>
        <small>The browser keeps this selection on your device only.</small>
      </label>
      <div className="workspace-dialog-actions">
        <button className="button small" onClick={onClose} type="button">Cancel</button>
        <button className="button primary small" disabled={!selection || !name.trim()} type="submit">Create workspace</button>
      </div>
    </form>
  </div>
}

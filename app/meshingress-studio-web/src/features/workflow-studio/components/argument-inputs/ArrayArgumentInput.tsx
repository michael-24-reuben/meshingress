import { useState } from 'react'
import { AddValueIcon, RemoveIcon } from '../../../../components/icons/node-icons'
import { arrayValues, type ArgumentInputProps } from './contracts'

export function ArrayArgumentInput({ id, name, value, describedById, onChange }: ArgumentInputProps) {
    const [draft, setDraft] = useState('')
    const values = arrayValues(value)
    const commit = () => {
        const nextValue = draft.trim()
        if (!nextValue) return
        onChange([...values, nextValue])
        setDraft('')
    }

    return <div className="argument-array-input argument-input-field">
        {values.length > 0 && <ul aria-label={`${name} values`} className="argument-array-values">
            {values.map((item, index) => <li className="argument-array-value" key={`${item}-${index}`}>
                <span>{item}</span>
                <button aria-label={`Remove ${item}`} className="argument-array-remove" onClick={() =>
                    onChange(values.filter((_, candidateIndex) => candidateIndex !== index))} type="button">
                    <RemoveIcon size={12} />
                </button>
            </li>)}
        </ul>}
        <div className="argument-array-entry">
            <input aria-describedby={describedById} aria-label={`Add ${name} value`} className="argument-input" id={id}
                onChange={(event) => setDraft(event.target.value)} onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                        event.preventDefault()
                        commit()
                    }
                }} type="text" value={draft} />
            <button aria-label={`Add ${name} value`} className="argument-array-add" disabled={!draft.trim()} onClick={commit} type="button">
                <AddValueIcon size={16} />
            </button>
        </div>
    </div>
}

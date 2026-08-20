import type { ScalarArgumentInputProps } from './contracts'

export function DateArgumentInput({ id, value, describedById, onChange }: ScalarArgumentInputProps) {
    return <input aria-describedby={describedById} className="argument-input argument-input-field" id={id}
        onChange={(event) => onChange(event.target.value)} type="date" value={value} />
}

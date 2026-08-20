import type { ScalarArgumentInputProps } from './contracts'

export function TimeArgumentInput({ id, value, describedById, onChange }: ScalarArgumentInputProps) {
    return <input aria-describedby={describedById} className="argument-input argument-input-field" id={id}
        onChange={(event) => onChange(event.target.value)} type="time" value={value} />
}

import type { ScalarArgumentInputProps } from './contracts'

export function PasswordArgumentInput({ id, value, describedById, onChange }: ScalarArgumentInputProps) {
    return <input aria-describedby={describedById} autoComplete="new-password" className="argument-input argument-input-field" id={id}
        onChange={(event) => onChange(event.target.value)} type="password" value={value} />
}

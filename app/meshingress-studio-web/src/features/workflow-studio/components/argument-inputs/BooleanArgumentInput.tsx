import type {ScalarArgumentInputProps} from './contracts'

export function BooleanArgumentInput({id, name, value, describedById, onChange}: ScalarArgumentInputProps) {
    return <label className="argument-boolean-control" htmlFor={id}>
        <input aria-describedby={describedById} checked={value === 'true'} id={id}
               onChange={(event) => onChange(event.target.checked ? 'true' : 'false')} type="checkbox"/>
        <span>{value === 'true' ? `${name} enabled` : `${name} disabled`}</span>
    </label>
}

import { normalizeArgumentSchema, type ScalarArgumentInputProps } from './contracts'

export function EnumArgumentInput({ id, schema, value, describedById, onChange }: ScalarArgumentInputProps) {
    const { enumValues } = normalizeArgumentSchema(schema)
    return <select aria-describedby={describedById} className="argument-input argument-input-field" id={id}
        onChange={(event) => onChange(event.target.value)} value={value}>
        {value === '' && <option value="">Select a value</option>}
        {enumValues.map((option) => <option key={option} value={option}>{option}</option>)}
    </select>
}

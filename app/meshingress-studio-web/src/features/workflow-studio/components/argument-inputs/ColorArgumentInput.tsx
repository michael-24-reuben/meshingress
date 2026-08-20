import type { CSSProperties } from 'react'
import { ColorObjectTypeIcon } from '../../../../components/icons/node-icons'
import type { ScalarArgumentInputProps } from './contracts'

const DEFAULT_PICKER_COLOR = '#7c8cff'

export function ColorArgumentInput({ id, name, value, describedById, onChange }: ScalarArgumentInputProps) {
    const pickerColor = pickerColorFor(value)
    const previewStyle = value ? { backgroundColor: value } as CSSProperties : undefined

    return <div className="argument-color-control">
        <input aria-describedby={describedById} className="argument-input argument-input-field" id={id}
            onChange={(event) => onChange(event.target.value)} type="text" value={value} />
        <span aria-hidden="true" className="argument-color-preview" style={previewStyle} />
        <label aria-label={`Choose ${name} color`} className="argument-color-picker" title="Choose color">
            <ColorObjectTypeIcon aria-hidden="true" size={16} />
            <input className="argument-color-picker-input" onChange={(event) => onChange(event.target.value)} type="color" value={pickerColor} />
        </label>
    </div>
}

function pickerColorFor(value: string): string {
    if (/^#[0-9a-fA-F]{6}$/.test(value)) return value
    if (/^#[0-9a-fA-F]{3}$/.test(value)) return `#${value.slice(1).split('').map((digit) => digit.repeat(2)).join('')}`
    return DEFAULT_PICKER_COLOR
}

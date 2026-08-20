import { useEffect, useState, type CSSProperties } from 'react'
import { normalizeArgumentSchema, type ScalarArgumentInputProps } from './contracts'

interface NumberArgumentInputProps extends ScalarArgumentInputProps {
    integer?: boolean
}

export function NumberArgumentInput({ id, name, schema, value, describedById, integer = false, onChange }: NumberArgumentInputProps) {
    const normalized = normalizeArgumentSchema(schema)
    const minimum = normalized.minimum
    const maximum = normalized.maximum
    const step = normalized.multipleOf ?? (integer ? 1 : undefined)
    const [draft, setDraft] = useState(value)
    const [valueHovered, setValueHovered] = useState(false)
    const [valueFocused, setValueFocused] = useState(false)

    useEffect(() => setDraft(value), [value])

    if (normalized.control === 'slider' && minimum !== undefined && maximum !== undefined && step !== undefined) {
        const sliderValue = sliderValueFor(value, minimum, maximum, step)
        const midpoint = snapToStep((minimum + maximum) / 2, minimum, maximum, step)
        const scaleFormatter = numberFormatter(step)
        const valueExpanded = valueHovered || valueFocused
        const compactValue = compactSliderValue(draft)
        const valueDisplay = valueExpanded ? draft : compactValue.mantissa
        const valueStyle = { width: `${valueExpanded ? Math.max(3, draft.length + 2) : 3}ch` } as CSSProperties
        const commitDraft = () => {
            if (!draft.trim()) {
                onChange('')
                return
            }

            const parsed = Number(draft)
            if (!Number.isFinite(parsed)) {
                setDraft(value)
                return
            }
            onChange(formatNumber(snapToStep(parsed, minimum, maximum, step), step))
        }

        return <div className={`argument-slider-control${valueExpanded ? ' is-value-expanded' : ''}`}>
            <div className="argument-slider-range">
                <input aria-describedby={describedById} aria-label={`${name} slider`} className="argument-slider" id={id}
                    max={maximum} min={minimum} onChange={(event) =>
                        onChange(formatNumber(snapToStep(Number(event.target.value), minimum, maximum, step), step))}
                    step={step} type="range" value={sliderValue} />
                <div aria-hidden="true" className="argument-slider-scale">
                    <span>{scaleFormatter.format(minimum)}</span>
                    <span>{scaleFormatter.format(midpoint)}</span>
                    <span>{scaleFormatter.format(maximum)}</span>
                </div>
            </div>
            <div className="argument-slider-value-control">
                <input aria-describedby={describedById} aria-label={`Set ${name} value`} className="argument-input argument-input-field argument-slider-value" inputMode={integer ? 'numeric' : 'decimal'}
                    onBlur={() => {
                        setValueFocused(false)
                        commitDraft()
                    }} onChange={(event) => setDraft(event.target.value)} onFocus={() => setValueFocused(true)}
                    onMouseEnter={() => setValueHovered(true)} onMouseLeave={() => setValueHovered(false)}
                    onKeyDown={(event) => {
                        if (event.key === 'Enter') event.currentTarget.blur()
                        if (event.key === 'Escape') {
                            setDraft(value)
                            event.currentTarget.blur()
                        }
                    }} style={valueStyle} type="text" value={valueDisplay} />
                {!valueExpanded && compactValue.unit && <span aria-hidden="true" className="argument-slider-unit">{compactValue.unit}</span>}
            </div>
        </div>
    }

    const inputStep = step ?? 'any'
    const changeBy = (direction: 1 | -1) => {
        const increment = typeof inputStep === 'number' ? inputStep : 1
        const initial = Number(value)
        const baseline = Number.isFinite(initial) ? initial : normalized.minimum ?? 0
        const next = clamp(baseline + direction * increment, normalized.minimum, normalized.maximum)
        onChange(integer ? String(Math.trunc(next)) : String(next))
    }

    return <div className="argument-counter">
        <button aria-label={`Decrease ${name}`} className="argument-counter-button" onClick={() => changeBy(-1)} type="button">−</button>
        <input aria-describedby={describedById} aria-label={name} className="argument-input argument-input-field argument-number-input" id={id}
            max={normalized.maximum} min={normalized.minimum} onChange={(event) => onChange(event.target.value)}
            step={inputStep} type="number" value={value} />
        <button aria-label={`Increase ${name}`} className="argument-counter-button" onClick={() => changeBy(1)} type="button">+</button>
    </div>
}

function clamp(value: number, minimum?: number, maximum?: number): number {
    if (minimum !== undefined && value < minimum) return minimum
    if (maximum !== undefined && value > maximum) return maximum
    return value
}

function sliderValueFor(value: string, minimum: number, maximum: number, step: number): string {
    const parsed = Number(value)
    const baseline = Number.isFinite(parsed) ? parsed : minimum
    return formatNumber(snapToStep(baseline, minimum, maximum, step), step)
}

function snapToStep(value: number, minimum: number, maximum: number, step: number): number {
    const clamped = clamp(value, minimum, maximum)
    const snapped = minimum + Math.round((clamped - minimum) / step) * step
    return Number(snapped.toFixed(decimalPlaces(step)))
}

function formatNumber(value: number, step: number): string {
    const precision = decimalPlaces(step)
    return precision === 0 ? String(value) : value.toFixed(precision).replace(/\.?0+$/, '')
}

function numberFormatter(step: number): Intl.NumberFormat {
    const maximumFractionDigits = decimalPlaces(step)
    return new Intl.NumberFormat(undefined, { maximumFractionDigits })
}

function decimalPlaces(value: number): number {
    const exponent = value.toString().split(/[eE]/)
    const fraction = exponent[0].split('.')[1]?.length ?? 0
    const exponentShift = Number(exponent[1] ?? 0)
    return Math.max(0, Math.min(12, fraction - exponentShift))
}

function compactSliderValue(value: string): { mantissa: string, unit: string } {
    const numeric = Number(value)
    if (!Number.isFinite(numeric) || numeric === 0) return { mantissa: value, unit: '' }

    const units = ['', 'K', 'M', 'B', 'T', 'P', 'E']
    let unitIndex = Math.max(0, Math.min(Math.floor(Math.log10(Math.abs(numeric)) / 3), units.length - 1))
    let scaled = numeric / 1000 ** unitIndex
    let rounded = roundToSignificantDigits(scaled, 3)

    if (Math.abs(rounded) >= 1000 && unitIndex < units.length - 1) {
        unitIndex += 1
        scaled = numeric / 1000 ** unitIndex
        rounded = roundToSignificantDigits(scaled, 3)
    }

    return { mantissa: formatSignificantDigits(rounded, 3), unit: units[unitIndex] }
}

function roundToSignificantDigits(value: number, digits: number): number {
    const magnitude = 10 ** (Math.floor(Math.log10(Math.abs(value))) - digits + 1)
    return Math.round(value / magnitude) * magnitude
}

function formatSignificantDigits(value: number, digits: number): string {
    if (value === 0) return '0'
    const maximumFractionDigits = Math.max(0, digits - 1 - Math.floor(Math.log10(Math.abs(value))))
    return new Intl.NumberFormat(undefined, { maximumFractionDigits, useGrouping: false }).format(value)
}

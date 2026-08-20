export type ArgumentSchema = Record<string, unknown>
export type ArgumentValue = string | string[]

import {resolveArgumentType} from './schema-type'

export interface ArgumentInputProps {
    id: string
    name: string
    schema?: ArgumentSchema
    value: ArgumentValue
    describedById?: string
    onChange: (value: ArgumentValue) => void
}

export interface ScalarArgumentInputProps extends Omit<ArgumentInputProps, 'value' | 'onChange'> {
    value: string
    onChange: (value: string) => void
}

export interface NormalizedArgumentSchema {
    type?: string
    format?: string
    enumValues: string[]
    minimum?: number
    maximum?: number
    multipleOf?: number
    control?: string
}

export function propertySchemaFor(inputSchema: ArgumentSchema | undefined, name: string): ArgumentSchema | undefined {
    const properties = inputSchema?.properties
    if (!properties || typeof properties !== 'object' || Array.isArray(properties)) return undefined

    const property = (properties as Record<string, unknown>)[name]
    return property && typeof property === 'object' && !Array.isArray(property)
        ? property as ArgumentSchema
        : undefined
}

export function isRequiredArgument(inputSchema: ArgumentSchema | undefined, name: string): boolean {
    return Array.isArray(inputSchema?.required) && inputSchema.required.includes(name)
}

export function argumentDescription(schema: ArgumentSchema | undefined): string | undefined {
    return typeof schema?.description === 'string' && schema.description.trim()
        ? schema.description
        : undefined
}

export function argumentTypeClass(schema: ArgumentSchema | undefined): string {
    return `arg-type-${resolveArgumentType(schema).kind}`
}

export function arrayValues(value: ArgumentValue): string[] {
    return Array.isArray(value) ? value : value ? [value] : []
}

export function normalizeArgumentSchema(schema: ArgumentSchema | undefined): NormalizedArgumentSchema {
    return {
        type: typeof schema?.type === 'string' ? schema.type : undefined,
        format: typeof schema?.format === 'string' ? schema.format : undefined,
        enumValues: Array.isArray(schema?.enum)
            ? schema.enum.filter((value): value is string | number | boolean =>
                typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean').map(String)
            : [],
        minimum: finiteNumber(schema?.minimum),
        maximum: finiteNumber(schema?.maximum),
        multipleOf: positiveFiniteNumber(schema?.multipleOf),
        control: typeof schema?.['x-mcp-control'] === 'string' ? schema['x-mcp-control'] : undefined,
    }
}

export function isIntegerValue(value: string): boolean {
    return /^-?\d+$/.test(value)
}

export function isNumberValue(value: string): boolean {
    return /^-?(?:\d+|\d*\.\d+)(?:[eE][+-]?\d+)?$/.test(value)
}

export function isDateValue(value: string): boolean {
    return /^\d{4}-\d{2}-\d{2}$/.test(value)
}

export function isDateTimeValue(value: string): boolean {
    return /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2}(?:\.\d+)?)?$/.test(value)
}

export function isTimeValue(value: string): boolean {
    return /^\d{2}:\d{2}(?::\d{2}(?:\.\d+)?)?$/.test(value)
}

function finiteNumber(value: unknown): number | undefined {
    return typeof value === 'number' && Number.isFinite(value) ? value : undefined
}

function positiveFiniteNumber(value: unknown): number | undefined {
    const number = finiteNumber(value)
    return number && number > 0 ? number : undefined
}

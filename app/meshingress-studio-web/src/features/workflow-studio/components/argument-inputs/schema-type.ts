import type {ArgumentSchema} from './contracts'

export type ArgumentTypeKind =
    | 'array'
    | 'binary'
    | 'boolean'
    | 'clock'
    | 'color'
    | 'code'
    | 'date'
    | 'decimal'
    | 'duration'
    | 'email'
    | 'enum'
    | 'file'
    | 'geo'
    | 'image'
    | 'interval'
    | 'json'
    | 'location'
    | 'null'
    | 'number'
    | 'secret'
    | 'text'
    | 'time'
    | 'unknown'
    | 'url'

export interface ResolvedArgumentType {
    kind: ArgumentTypeKind
    label: string
}

/**
 * Resolves the existing JSON Schema type surface used by Studio controls, labels, and icons.
 * Constraint keywords intentionally do not participate in this decision yet.
 */
export function resolveArgumentType(schema: ArgumentSchema | undefined): ResolvedArgumentType {
    const type = typeof schema?.type === 'string' ? schema.type : undefined
    const format = typeof schema?.format === 'string' ? schema.format : undefined

    if (Array.isArray(schema?.enum)) return {kind: 'enum', label: 'enum'}
    if (format === 'date' || format === 'date-time') return {kind: 'date', label: format}
    if (format === 'time') return {kind: 'time', label: format}
    if (format === 'duration') return {kind: 'duration', label: format}
    if (format === 'clock') return {kind: 'clock', label: format}
    if (format === 'interval') return {kind: 'interval', label: format}
    if (format === 'uri' || format === 'uri-reference' || format === 'url') return {kind: 'url', label: format}
    if (format === 'email') return {kind: 'email', label: format}
    if (format === 'password') return {kind: 'secret', label: format}
    if (format === 'code') return {kind: 'code', label: format}
    if (format === 'file') return {kind: 'file', label: format}
    if (format === 'image') return {kind: 'image', label: format}
    if (format === 'color') return {kind: 'color', label: format}
    if (format === 'location') return {kind: 'location', label: format}
    if (format === 'geo') return {kind: 'geo', label: format}
    if (format === 'json') return {kind: 'json', label: format}
    if (format === 'null') return {kind: 'null', label: format}
    if (format === 'unknown') return {kind: 'unknown', label: format}
    if (format === 'binary' || schema?.contentEncoding === 'base64') return {kind: 'binary', label: format ?? 'binary'}
    if (schema?.contentMediaType === 'application/json') return {kind: 'json', label: 'json'}

    switch (type) {
        case 'string': return {kind: 'text', label: type}
        case 'number': return {kind: 'decimal', label: type}
        case 'integer': return {kind: 'number', label: type}
        case 'array': return {kind: 'array', label: type}
        case 'object': return {kind: 'json', label: type}
        case 'boolean': return {kind: 'boolean', label: type}
        case 'null': return {kind: 'null', label: type}
        case 'code': return {kind: 'code', label: type}
        case 'file': return {kind: 'file', label: type}
        default: return {kind: 'unknown', label: 'unknown type'}
    }
}

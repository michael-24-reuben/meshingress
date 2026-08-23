export type JsonSchema = Record<string, unknown>

export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

export function stableJson(value: unknown): string {
  if (Array.isArray(value)) return `[${value.map(stableJson).join(',')}]`
  if (isRecord(value)) return `{${Object.keys(value).toSorted().map((key) => `${JSON.stringify(key)}:${stableJson(value[key])}`).join(',')}}`
  return JSON.stringify(value)
}

export function uniqueSchemas(schemas: JsonSchema[]): JsonSchema[] {
  const unique = new Map<string, JsonSchema>()
  for (const schema of schemas) unique.set(stableJson(schema), schema)
  return [...unique.entries()].toSorted(([left], [right]) => left.localeCompare(right)).map(([, schema]) => schema)
}

/**
 * Infer an observed JSON schema from a sample runtime value.
 */
export function inferObservedOutputSchema(value: unknown): JsonSchema {
  if (value === null) return { type: 'null' }
  if (typeof value === 'string') return { type: 'string' }
  if (typeof value === 'boolean') return { type: 'boolean' }
  if (typeof value === 'number') return Number.isInteger(value) ? { type: 'integer' } : { type: 'number' }
  if (Array.isArray(value)) {
    const itemSchemas = uniqueSchemas(value.map(inferObservedOutputSchema))
    if (!itemSchemas.length) return { type: 'array' }
    return {
      type: 'array',
      items: itemSchemas.length === 1 ? itemSchemas[0] : { anyOf: itemSchemas },
    }
  }
  if (isRecord(value)) {
    const properties: Record<string, JsonSchema> = {}
    const names = Object.keys(value).toSorted()
    for (const name of names) properties[name] = inferObservedOutputSchema(value[name])
    return {
      type: 'object',
      properties,
      required: names,
      additionalProperties: false,
    }
  }
  return {}
}

/**
 * Turns an arbitrary JSON-compatible JavaScript value into an inferred JSON Schema.
 */
export function jsonToSchema(value: unknown): JsonSchema {
  const schema = inferObservedOutputSchema(value)
  if (!schema || Object.keys(schema).length === 0) return {}
  schema['$schema'] = 'https://json-schema.org/draft/2020-12/schema'
  schema['additionalProperties'] = false
  return schema
}

import type { ElementAttributeInput, ElementAttributesNormalized } from "./types"

export function formatElementAttributes(attributes?: ElementAttributeInput, extras: ElementAttributeInput = {}): ElementAttributesNormalized {
    const result: ElementAttributesNormalized = {}

    for (const [key, value] of Object.entries(extras)) {
        result[key] = Array.isArray(value) ? value.join(' ') : value
    }
    if (!attributes) return result

    for (const [key, value] of Object.entries(attributes)) {
        const normalizedValue = Array.isArray(value) ? value.join(' ') : value
        result[key] = key === 'className' && result.className
            ? `${result.className} ${normalizedValue}`
            : normalizedValue
    }

    return result
}

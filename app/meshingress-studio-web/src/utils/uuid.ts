/**
 * Safely generates an RFC4122 v4 compliant UUID in any environment
 * (secure HTTPS contexts, localhost, or insecure LAN HTTP contexts via --host).
 */
export function generateUuid(): string {
  const g = typeof globalThis !== 'undefined' ? globalThis : (typeof window !== 'undefined' ? window : (typeof self !== 'undefined' ? self : {} as typeof globalThis))
  if (typeof g.crypto !== 'undefined') {
    if (typeof g.crypto.randomUUID === 'function') {
      try {
        return g.crypto.randomUUID()
      } catch {
        // Fall through if randomUUID throws unexpectedly
      }
    }
    if (typeof g.crypto.getRandomValues === 'function') {
      try {
        const bytes = new Uint8Array(16)
        g.crypto.getRandomValues(bytes)
        bytes[6] = (bytes[6] & 0x0f) | 0x40 // Version 4
        bytes[8] = (bytes[8] & 0x3f) | 0x80 // Variant 10xx
        const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('')
        return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
      } catch {
        // Fall through
      }
    }
  }

  // Math.random fallback (e.g. legacy/insecure environments without crypto.getRandomValues)
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

export const uuid = generateUuid

// Self-install polyfill if crypto.randomUUID is not present on globalThis.crypto
if (typeof globalThis !== 'undefined') {
  const g = globalThis as typeof globalThis & { crypto?: Crypto }
  if (typeof g.crypto === 'undefined') {
    try {
      // @ts-ignore
      g.crypto = {}
    } catch {
      // Ignore if cannot assign
    }
  }
  if (g.crypto && typeof g.crypto.randomUUID !== 'function') {
    try {
      Object.defineProperty(g.crypto, 'randomUUID', {
        value: generateUuid,
        configurable: true,
        writable: true,
      })
    } catch {
      try {
        // @ts-ignore
        g.crypto.randomUUID = generateUuid
      } catch {
        // Ignore if read-only
      }
    }
  }
}

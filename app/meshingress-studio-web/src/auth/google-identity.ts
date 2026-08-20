export interface GoogleIdentity {
  subject: string
  displayName: string
  email: string
  avatarUrl?: string
}

export function readGoogleIdentity(credential: string): GoogleIdentity {
  const payload = credential.split('.')[1]
  if (!payload) throw new Error('Google did not return a readable identity token.')

  const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(payload.length / 4) * 4, '='))
  const claims = JSON.parse(decoded) as Record<string, unknown>
  const subject = text(claims.sub)
  if (!subject) throw new Error('Google identity token did not include a subject.')

  return {
    subject,
    displayName: text(claims.name) || text(claims.email) || 'Google user',
    email: text(claims.email),
    avatarUrl: text(claims.picture) || undefined,
  }
}

function text(value: unknown): string {
  return typeof value === 'string' ? value.trim() : ''
}

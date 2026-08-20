import { RuntimeConfiguration } from '../runtime/RuntimeConfiguration'

export const meshingressApiBaseUrl = () => RuntimeConfiguration.current.apiBaseUrl

let studioAccessToken = ''

export function setStudioAccessToken(value: string | null): void {
  studioAccessToken = value?.trim() ?? ''
}

export class MeshingressApiError extends Error {
  readonly status: number
  readonly body: unknown

  constructor(status: number, body: unknown) {
    super(`Meshingress API request failed with status ${status}.`)
    this.name = 'MeshingressApiError'
    this.status = status
    this.body = body
  }
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (studioAccessToken && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${studioAccessToken}`)
  }
  const response = await fetch(`${meshingressApiBaseUrl()}${path}`, {
    ...init,
    headers,
  })

  const body = await readResponseBody(response)
  if (!response.ok) {
    throw new MeshingressApiError(response.status, body)
  }

  return body as T
}

async function readResponseBody(response: Response): Promise<unknown> {
  if (response.status === 204) {
    return undefined
  }

  const contentType = response.headers.get('content-type') ?? ''
  return contentType.includes('application/json') ? response.json() : response.text()
}

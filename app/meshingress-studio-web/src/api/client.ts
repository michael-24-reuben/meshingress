const configuredApiBaseUrl = import.meta.env.VITE_MESHINGRESS_API_BASE_URL

export const meshingressApiBaseUrl = (configuredApiBaseUrl ?? 'http://localhost:4737').replace(
  /\/$/,
  '',
)

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
  const response = await fetch(`${meshingressApiBaseUrl}${path}`, {
    ...init,
    headers: {
      Accept: 'application/json',
      ...init.headers,
    },
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

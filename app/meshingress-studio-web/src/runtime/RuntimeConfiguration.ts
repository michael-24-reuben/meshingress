import { parse } from 'yaml'

interface RuntimeConfigurationDocument {
  meshingress?: {
    apiBaseUrl?: unknown
    workflowWebSocketPath?: unknown
    googleClientId?: unknown
  }
}

const fallbackApiBaseUrl = (import.meta.env.VITE_MESHINGRESS_API_BASE_URL ?? 'http://localhost:4737').replace(/\/$/, '')
const fallbackWorkflowWebSocketPath = '/api/v1/workflows/ws'
type RuntimeConfigurationListener = (configuration: RuntimeConfiguration) => void

export class RuntimeConfiguration {
  static current = new RuntimeConfiguration(fallbackApiBaseUrl, fallbackWorkflowWebSocketPath, '')
  private static readonly listeners = new Set<RuntimeConfigurationListener>()

  readonly apiBaseUrl: string
  readonly workflowWebSocketPath: string
  readonly googleClientId: string

  private constructor(apiBaseUrl: string, workflowWebSocketPath: string, googleClientId: string) {
    this.apiBaseUrl = apiBaseUrl
    this.workflowWebSocketPath = workflowWebSocketPath
    this.googleClientId = googleClientId
  }

  static async load(): Promise<RuntimeConfiguration> {
    try {
      const response = await fetch(`${import.meta.env.BASE_URL}runtime-config.yaml`, { cache: 'no-store' })
      if (!response.ok) return RuntimeConfiguration.current

      const document = parse(await response.text()) as RuntimeConfigurationDocument
      const apiBaseUrl = document?.meshingress?.apiBaseUrl
      if (typeof apiBaseUrl === 'string' && apiBaseUrl.trim()) {
        const configuredPath = document?.meshingress?.workflowWebSocketPath
        const normalizedApiBaseUrl = normalizeApiBaseUrl(apiBaseUrl)
        const configuredGoogleClientId = typeof document?.meshingress?.googleClientId === 'string'
          ? document.meshingress.googleClientId.trim()
          : ''
        RuntimeConfiguration.update(new RuntimeConfiguration(
          normalizedApiBaseUrl,
          typeof configuredPath === 'string' && configuredPath.trim()
            ? normalizePath(configuredPath)
            : fallbackWorkflowWebSocketPath,
          configuredGoogleClientId,
        ))
      }
    } catch (error) {
      console.warn('Unable to load runtime-config.yaml; using the build-time API URL.', error)
    }

    return RuntimeConfiguration.current
  }

  static async loadGoogleClientIdInBackground(): Promise<void> {
    const configuration = RuntimeConfiguration.current
    if (configuration.googleClientId) return

    const googleClientId = await loadGoogleClientId(configuration.apiBaseUrl)
    if (!googleClientId) return

    RuntimeConfiguration.update(new RuntimeConfiguration(
      configuration.apiBaseUrl,
      configuration.workflowWebSocketPath,
      googleClientId,
    ))
  }

  static subscribe(listener: RuntimeConfigurationListener): () => void {
    RuntimeConfiguration.listeners.add(listener)
    return () => RuntimeConfiguration.listeners.delete(listener)
  }

  private static update(configuration: RuntimeConfiguration): void {
    RuntimeConfiguration.current = configuration
    RuntimeConfiguration.listeners.forEach((listener) => listener(configuration))
  }
}

async function loadGoogleClientId(apiBaseUrl: string): Promise<string> {
  try {
    const response = await fetch(`${apiBaseUrl}/api/v1/auth/config`, { cache: 'no-store' })
    if (!response.ok) return ''
    const document = await response.json() as { googleClientId?: unknown }
    return typeof document.googleClientId === 'string' ? document.googleClientId.trim() : ''
  } catch {
    return ''
  }
}

function normalizeApiBaseUrl(value: string): string {
  return value.trim().replace(/\/+$/, '')
}

function normalizePath(value: string): string {
  return `/${value.trim().replace(/^\/+|\/+$/g, '')}`
}

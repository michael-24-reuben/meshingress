import { apiRequest, meshingressApiBaseUrl } from './client'

export interface ToolModuleSummary {
  toolId: string
  source: 'classpath' | 'runtime' | string
  namespace: string
  title: string
  icon?: { href: string; mediaType: string; alt: string } | null
  href: string
}

interface ToolModuleListResponse {
  items?: ToolModuleSummary[]
}

/** Returns compact module cards; detailed manifest documents are fetched only on demand. */
export async function listToolModules(): Promise<ToolModuleSummary[]> {
  const response = await apiRequest<ToolModuleListResponse>('/api/v1/tool-modules')
  return (response.items ?? []).map((module) => ({
    ...module,
    href: resolveApiHref(module.href),
    icon: module.icon && { ...module.icon, href: resolveApiHref(module.icon.href) },
  }))
}

function resolveApiHref(href: string): string {
  if (/^https?:\/\//i.test(href)) return href
  return `${meshingressApiBaseUrl()}${href.startsWith('/') ? href : `/${href}`}`
}

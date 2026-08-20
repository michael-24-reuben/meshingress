import type { McpToolFunction } from '../../api/mcp'
import type { ToolModuleSummary } from '../../api/tool-modules'
import type { NodeIconDescriptor } from '../../components/icons/node-icons'
import type { RegisteredTool, WorkflowNode } from './types'

export interface ToolPresentationIndex {
  forNode(node: WorkflowNode): NodeIconDescriptor
  forNamespace(tools: RegisteredTool[]): NodeIconDescriptor
}

const folderIcon: NodeIconDescriptor = { source: 'built-in', name: 'folder' }
const toolIcon: NodeIconDescriptor = { source: 'built-in', name: 'tool' }

/** Creates O(1) module and function lookups shared by every Studio icon consumer. */
export function createToolPresentationIndex(functions: McpToolFunction[], modules: ToolModuleSummary[]): ToolPresentationIndex {
  const moduleByToolId = new Map(modules.map((module) => [module.toolId, module]))
  const functionModuleToolId = new Map(functions.flatMap((functionEntry) => functionEntry.moduleToolId ? [[functionEntry.name, functionEntry.moduleToolId] as const] : []))

  const moduleIcon = (moduleToolId?: string): NodeIconDescriptor => {
    const module = moduleToolId ? moduleByToolId.get(moduleToolId) : undefined
    return module?.icon
      ? { source: 'image', href: module.icon.href, alt: module.icon.alt, fallback: 'tool' }
      : toolIcon
  }

  return {
    forNode(node) {
      const iconKind = node.iconKind ?? (node.kind === 'trigger' ? 'workflow-manual-trigger' : 'tool')
      if (iconKind === 'workflow-manual-trigger') return { source: 'built-in', name: 'manual-trigger' }
      if (iconKind === 'meshingress-api') return { source: 'built-in', name: 'meshingress-api' }
      return moduleIcon(node.moduleToolId ?? functionModuleToolId.get(`${node.toolId}.${node.functionName}`))
    },
    forNamespace(tools) {
      const moduleToolIds = new Set(tools.flatMap((tool) => tool.moduleToolId ? [tool.moduleToolId] : []))
      if (moduleToolIds.size !== 1) return folderIcon
      return moduleIcon(moduleToolIds.values().next().value)
    },
  }
}

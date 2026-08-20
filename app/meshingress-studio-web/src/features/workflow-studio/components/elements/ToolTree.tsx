import type { NodeIconDescriptor } from '../../../../components/icons/node-icons'
import { ChevronRightIcon, CubeIcon, WorkflowNodeIcon } from '../../../../components/icons/node-icons'
import type { ToolPresentationIndex } from '../../node-presentation'
import { TOOL_NODE_DRAG_TYPE, type RegisteredTool } from '../../types'
import type { SortOption } from '../StudioLeftPanel'

// TODO: Make the automatic tree-expansion limit configurable in the Settings tab.
const AUTO_EXPAND_CHILD_LIMIT = 1

interface ToolTreeFunction extends RegisteredTool {
    functionName: string
}

interface ToolTreeTool {
    name: string
    functions: ToolTreeFunction[]
}

interface ToolTreeNamespace {
    name: string
    icon: NodeIconDescriptor
    tools: ToolTreeTool[]
    totalFunctions: number
}

export function ToolTree({ presentations, tools, expandAll, sortBy, onToolAdd }: {
    presentations: ToolPresentationIndex
    tools: RegisteredTool[]
    expandAll: boolean | null
    sortBy: SortOption
    onToolAdd: (toolName: string) => void
}) {
    const namespaces = toolTree(tools, presentations, sortBy)
    const openState = expandAll === true ? true : expandAll === false ? false : undefined
    return <div className="tool-tree">
        {namespaces.map((namespace) =>
            <details className="tool-tree-namespace" key={namespace.name} onToggle={(event) => autoExpandChildBranches(event.currentTarget)} open={openState ?? undefined}>
                <summary className="tree-view-item list-item">
                    {/* <span className="tool-tree-chevron" aria-hidden="true">›</span> */}
                    <ChevronRightIcon className="tool-tree-chevron" size={18} aria-hidden="true" />
                    <WorkflowNodeIcon className="tool-tree-icon" descriptor={namespace.icon} size={16} />
                    <span className="tool-tree-branch">{namespace.name}</span>
                    <span className="tool-tree-count">{namespace.totalFunctions}</span>
                </summary>
                <div className="tool-tree-tools">
                    {namespace.tools.map((tool) =>
                        <details className="tool-tree-tool" key={tool.name} onToggle={(event) => autoExpandChildBranches(event.currentTarget)} open={openState ?? undefined}>
                            <summary className="tree-view-item list-item">
                                {/* <span className="tool-tree-chevron" aria-hidden="true">›</span> */}
                                <ChevronRightIcon className="tool-tree-chevron" size={18} aria-hidden="true" />
                                <CubeIcon className="tool-tree-icon" size={16} aria-hidden="true" />
                                <span className="tool-tree-branch">{tool.name}</span>
                                <span className="tool-tree-count">{tool.functions.length}</span>
                            </summary>
                            <div className="tool-tree-functions">
                                {tool.functions.map((functionEntry) =>
                                    <button className="tree-view-item list-item tool-node tool-tree-function" draggable key={functionEntry.id} onDoubleClick={() => onToolAdd(functionEntry.id)} onDragStart={(event) => {
                                        event.dataTransfer.effectAllowed = 'copy'
                                        event.dataTransfer.setData(TOOL_NODE_DRAG_TYPE, functionEntry.id)
                                        event.dataTransfer.setData('text/plain', functionEntry.id)
                                    }} title={functionEntry.description} type="button">
                                        <span className="item-icon">T</span>
                                        <span className="item-main">
                                            <span className="item-title">{functionEntry.functionName}</span>
                                            <span className="item-sub">{functionEntry.title === functionEntry.functionName ? functionEntry.id : functionEntry.title}</span>
                                        </span>
                                    </button>)}
                            </div>
                        </details>)}
                </div>
            </details>)}
    </div>
}

function autoExpandChildBranches(branch: HTMLDetailsElement) {
    if (!branch.open) return
    const children = Array.from(branch.querySelectorAll<HTMLDetailsElement>(':scope > .tool-tree-tools > .tool-tree-tool'))
    if (children.length > AUTO_EXPAND_CHILD_LIMIT) return
    children.forEach((child) => { child.open = true })
}

function toolTree(tools: RegisteredTool[], presentations: ToolPresentationIndex, sortBy: SortOption = 'name-asc'): ToolTreeNamespace[] {
    const namespaces = new Map<string, { tools: Map<string, ToolTreeFunction[]> }>()
    for (const tool of tools) {
        const [namespace, toolName, ...functionSegments] = tool.id.split('.')
        const functionName = functionSegments.join('.')
        if (!namespace || !toolName || !functionName) continue
        const namespaceEntry = namespaces.get(namespace) ?? { tools: new Map<string, ToolTreeFunction[]>() }
        const functions = namespaceEntry.tools.get(toolName) ?? []
        functions.push({ ...tool, functionName })
        namespaceEntry.tools.set(toolName, functions)
        namespaces.set(namespace, namespaceEntry)
    }

    const sortFn = (a: string, b: string) => {
        if (sortBy === 'name-desc') return b.localeCompare(a)
        return a.localeCompare(b)
    }

    const result = [...namespaces].map(([name, { tools: toolsByName }]) => {
        const namespaceTools = [...toolsByName.values()].flat()
        const sortedTools = [...toolsByName].map(([toolName, functions]) => ({
            name: toolName,
            functions: functions.toSorted((left, right) => sortFn(left.functionName, right.functionName)),
        })).toSorted((left, right) => sortFn(left.name, right.name))

        return {
            name,
            icon: presentations.forNamespace(namespaceTools),
            tools: sortedTools,
            totalFunctions: namespaceTools.length,
        }
    })

    if (sortBy === 'name-desc') {
        return result.toSorted((left, right) => right.name.localeCompare(left.name))
    } else if (sortBy === 'count-desc') {
        return result.toSorted((left, right) => right.totalFunctions - left.totalFunctions)
    } else if (sortBy === 'namespace') {
        return result.toSorted((left, right) => left.name.localeCompare(right.name))
    }
    return result.toSorted((left, right) => left.name.localeCompare(right.name))
}

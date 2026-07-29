export interface AvailableToolFunction {
  id: string
  toolId: string
  functionName: string
  title: string
}

/**
 * The Studio's current function catalog, grouped by owning tool for the
 * node-specific Function selector.
 */
export const availableToolFunctions: AvailableToolFunction[] = [
  { id: 'cli.powershell.execute', toolId: 'cli.powershell', functionName: 'execute', title: 'PowerShell' },
  { id: 'helloworld.greet', toolId: 'helloworld', functionName: 'greet', title: 'Hello World' },
  { id: 'toonverse.search', toolId: 'toonverse', functionName: 'search', title: 'Toonverse' },
]

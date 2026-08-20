export interface AvailableToolFunction {
  id: string
  toolId: string
  functionName: string
  title: string
  inputSchema?: Record<string, unknown>
}

/**
 * Offline metadata fallback when live MCP tool discovery is unavailable.
 * The node-specific Function selector prefers the live functions for its tool.
 */
export const availableToolFunctions: AvailableToolFunction[] = [
  {
    id: 'powershell.cli.execute',
    toolId: 'cli.powershell',
    functionName: 'execute',
    title: 'PowerShell',
    inputSchema: {
      type: 'object',
      required: ['script'],
      properties: {
        script: {type: 'string', description: 'PowerShell script body to execute.'},
        workingDirectory: {type: 'string', description: 'Optional working directory. Defaults to the current JVM working directory.'},
        timeoutMs: {type: 'integer', description: 'Optional execution timeout in milliseconds. Defaults to 20000 and is capped by the tool.'},
        executable: {type: 'string', description: 'Optional PowerShell executable. Defaults to pwsh. Use powershell.exe for Windows PowerShell.'},
        arguments: {type: 'array', items: {type: 'string'}, description: 'Optional script arguments passed after -File.'},
        environment: {type: 'object', description: 'Optional environment variables to add or override for the child process.'},
        includeScriptInStructuredContent: {type: 'boolean', description: 'Whether to echo the script body in structuredContent. Defaults to false.'},
      },
    },
  },
  {
    id: 'helloworld.greeting.greet',
    toolId: 'helloworld',
    functionName: 'greet',
    title: 'Hello World',
    inputSchema: {
      type: 'object',
      required: ['name'],
      properties: {name: {type: 'string', description: 'The name of the person to greet'}},
    },
  },
  {
    id: 'open-ink-library.toonverse.search',
    toolId: 'toonverse',
    functionName: 'search',
    title: 'Toonverse',
    inputSchema: {
      type: 'object',
      properties: {
        name: {type: 'string', description: 'Optional work name to search.'},
        genres: {type: 'array', items: {type: 'string'}, description: 'Genres to include; use genreMode to combine them.'},
        excludeGenres: {type: 'array', items: {type: 'string'}, description: 'Genres to exclude.'},
        genreMode: {type: 'string', enum: ['or', 'and'], description: 'Genre operator: or or and.'},
        type: {type: 'string', enum: ['manhwa', 'manhua', 'manga'], description: 'Publication type: manhwa, manhua, or manga.'},
        minChapters: {type: 'integer', description: 'Minimum chapter count.'},
        maxChapters: {type: 'integer', description: 'Maximum chapter count.'},
        minRating: {type: 'number', description: 'Minimum rating from 0 through 5.'},
        maxRating: {type: 'number', description: 'Maximum rating from 0 through 5.'},
        author: {type: 'string', description: 'Author name filter.'},
        status: {type: 'string', enum: ['ongoing', 'completed', 'hiatus'], description: 'Publication status: ongoing, completed, or hiatus.'},
        sortBy: {type: 'string', enum: ['popular', 'trending', 'updated', 'rating', 'library', 'newest', 'chapters', 'alphabetical'], description: 'Sort: popular, trending, updated, rating, library, newest, chapters, or alphabetical.'},
        limit: {type: 'integer', description: 'Result count from 1 through 100; defaults to 20.'},
        offset: {type: 'integer', description: 'Zero-based result offset.'},
      },
    },
  },
]

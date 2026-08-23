export type WorkspaceLayoutSpace = {
  id: string
  path: string
  role: 'workflow-source' | 'transform-source' | 'integration-definition' | 'asset' | 'configuration' | 'test-source' | 'generated-output' | 'local-studio-state'
  studio: Readonly<Record<'browse' | 'edit' | 'validate' | 'run', boolean | 'test-only'>>
  versionControl: 'tracked' | 'ignored'
}

export type WorkspaceLayoutPackage = {
  id: string
  version: number
  spaces: readonly WorkspaceLayoutSpace[]
  scaffold: {
    directories: readonly string[]
    files: Readonly<Record<string, (workspaceName: string) => string>>
  }
}

const workspaceManifest = (workspaceName: string) => `apiVersion: meshingress.dev/v1alpha1
kind: Project

metadata:
  name: ${JSON.stringify(workspaceName)}

layout:
  id: workflow-project
  version: 1
`

export const WORKFLOW_PROJECT_LAYOUT_V1: WorkspaceLayoutPackage = {
  id: 'workflow-project',
  version: 1,
  spaces: [
    { id: 'workflows', path: 'src/workflows', role: 'workflow-source', studio: { browse: true, edit: true, validate: true, run: true }, versionControl: 'tracked' },
    { id: 'transforms', path: 'src/transforms', role: 'transform-source', studio: { browse: true, edit: true, validate: true, run: false }, versionControl: 'tracked' },
    { id: 'integrations', path: 'src/integrations', role: 'integration-definition', studio: { browse: true, edit: true, validate: true, run: false }, versionControl: 'tracked' },
    { id: 'resources', path: 'resources', role: 'asset', studio: { browse: true, edit: false, validate: false, run: false }, versionControl: 'tracked' },
    { id: 'configuration', path: 'config', role: 'configuration', studio: { browse: true, edit: true, validate: true, run: false }, versionControl: 'tracked' },
    { id: 'tests', path: 'tests', role: 'test-source', studio: { browse: true, edit: true, validate: true, run: 'test-only' }, versionControl: 'tracked' },
    { id: 'runtime', path: 'var', role: 'generated-output', studio: { browse: true, edit: false, validate: false, run: false }, versionControl: 'ignored' },
    { id: 'runs', path: 'var/runs', role: 'generated-output', studio: { browse: true, edit: false, validate: false, run: false }, versionControl: 'ignored' },
    { id: 'tool-cache', path: 'var/cache/tools', role: 'generated-output', studio: { browse: true, edit: false, validate: false, run: false }, versionControl: 'ignored' },
    { id: 'local-state', path: '.meshingress', role: 'local-studio-state', studio: { browse: false, edit: false, validate: false, run: false }, versionControl: 'ignored' },
  ],
  scaffold: {
    directories: [
      'src/workflows',
      'src/transforms',
      'src/integrations',
      'resources/assets',
      'resources/templates',
      'resources/fixtures',
      'config/environments',
      'tests/workflows',
      'tests/fixtures',
      'var/cache/tools',
      'var/runs',
      'var',
      '.meshingress',
    ],
    files: {
      'meshingress.project.yaml': workspaceManifest,
      'README.md': (workspaceName) => `# ${workspaceName}\n\nLocal Meshingress workflow project using the \`workflow-project\` layout v1.\n`,
      '.gitignore': () => '/var/\n/.meshingress/\n/.env\n*.secret.*\n',
      '.env.example': () => '# Put local secrets in .env. Do not commit that file.\n',
    },
  },
}

const LAYOUT_PACKAGES: readonly WorkspaceLayoutPackage[] = [WORKFLOW_PROJECT_LAYOUT_V1]

export function findWorkspaceLayout(id: string, version: number): WorkspaceLayoutPackage | null {
  return LAYOUT_PACKAGES.find((layout) => layout.id === id && layout.version === version) ?? null
}

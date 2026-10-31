import { WorkflowStudioPage } from './WorkflowStudioPage'

/** Temporary entry point that shares Studio runtime state but swaps only the workbench surface. */
export function StudioWorkbenchTmpPage() {
  return <WorkflowStudioPage workbench="honeycomb-tmp" />
}

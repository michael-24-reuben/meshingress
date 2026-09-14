import { type NodeOverlay, type WorkflowModelSnapshot } from '../../../states/WorkflowStudioState'

/**
 * Routes Honeycomb interaction state into the stable WorkflowModelSnapshot.
 * The renderer owns only ephemeral view concerns such as geometry and tooltip
 * position; this adapter does not create a second phase-state store.
 */
export class HoneycombWorkflowModelAdapter {
  private readonly model: WorkflowModelSnapshot

  public constructor(model: WorkflowModelSnapshot) {
    this.model = model
  }

  public setNodeOverlay(nodeId: string, phase: NodeOverlay, active: boolean = true): void {
    this.model.applyMutation({
      target: { kind: 'node', nodeId },
      category: 'nodeOverlay',
      phase,
      active,
    })
  }
}

import type { WorkflowNode } from '../../types'
import {
  CanvasModelSnapshot,
  NodeModelSnapshot,
  PathModelSnapshot,
  SlotModelSnapshot,
  WorkflowModelSnapshot,
  WorkflowStateSnapshot,
  type WorkflowStudioSceneData,
} from '../WorkflowStudioState'

class WorkflowModelFactoryNodeSnapshot extends NodeModelSnapshot {
  public constructor(node: WorkflowNode) {
    super(node)
  }

  protected snapshot(): null {
    return null
  }
}

class WorkflowModelFactorySlotSnapshot extends SlotModelSnapshot {
  public constructor() {
    super(null)
  }

  protected snapshot(): null {
    return null
  }
}

class WorkflowModelFactoryCanvasSnapshot extends CanvasModelSnapshot {
  public constructor(slotModels: SlotModelSnapshot[]) {
    super(slotModels)
  }

  protected snapshot(): null {
    return null
  }
}

/**
 * Creates stable, renderer-neutral workflow model identities from Studio scene
 * data. Honeycomb owns JSX while consuming these models' projected attributes.
 */
export class WorkflowModelSnapshotFactory {
  public create(data: WorkflowStudioSceneData): WorkflowModelSnapshot {
    const nodeModels = new Map(data.nodes.map((node) => [node.id, new WorkflowModelFactoryNodeSnapshot(node)]))
    const pathModels = new Map(data.edges.map((edge) => {
      const model = new PathModelSnapshot(edge)
      return [model.getPathId(), model]
    }))
    const slotModels = new Map(data.placements.map(({ q, r }) => {
      const model = new WorkflowModelFactorySlotSnapshot()
      model.occupancy.setPhase('occupied')
      return [`slot/q${q}-r${r}`, model]
    }))
    const workflowState = new WorkflowStateSnapshot()
    workflowState.setPhase('incomplete')

    return new WorkflowModelSnapshot({
      workflowState,
      canvasModel: new WorkflowModelFactoryCanvasSnapshot([...slotModels.values()]),
      nodeModels,
      pathModels,
      slotModels,
    })
  }
}

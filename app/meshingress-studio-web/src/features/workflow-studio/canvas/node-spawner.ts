import { clamp, snapToGrid } from './grid'
import { GRID_SIZE, NODE_HEIGHT, NODE_STAGE_INSET, NODE_WIDTH, type WorkflowNode } from '../types'

export interface NodePosition {
    x: number
    y: number
}

export interface NodeSpawnBounds {
    width: number
    height: number
}

/**
 * Constrains a node's upper-left corner so its complete bounds remain on the canvas stage.
 */
export function constrainNodePosition(rawX: number, rawY: number, bounds: NodeSpawnBounds): NodePosition {
    return {
        x: snapToGrid(clamp(rawX, NODE_STAGE_INSET, bounds.width - NODE_WIDTH - NODE_STAGE_INSET)),
        y: snapToGrid(clamp(rawY, NODE_STAGE_INSET, bounds.height - NODE_HEIGHT - NODE_STAGE_INSET)),
    }
}

function collides(position: NodePosition, node: WorkflowNode): boolean {
    return position.x - NODE_STAGE_INSET < node.x + NODE_WIDTH
        && position.x + NODE_WIDTH + NODE_STAGE_INSET > node.x
        && position.y - NODE_STAGE_INSET < node.y + NODE_HEIGHT
        && position.y + NODE_HEIGHT + NODE_STAGE_INSET > node.y
}

/**
 * Finds the nearest grid-aligned, in-bounds position that preserves the stage inset around
 * all existing nodes. Returns null only when the canvas has no usable placement remaining.
 */
export function findNodeSpawnPosition(target: NodePosition, nodes: WorkflowNode[], bounds: NodeSpawnBounds): NodePosition | null {
    const requested = constrainNodePosition(target.x, target.y, bounds)
    const minimumX = NODE_STAGE_INSET
    const minimumY = NODE_STAGE_INSET
    const maximumX = bounds.width - NODE_WIDTH - NODE_STAGE_INSET
    const maximumY = bounds.height - NODE_HEIGHT - NODE_STAGE_INSET
    let closest: NodePosition | null = null
    let closestDistance = Number.POSITIVE_INFINITY

    for (let y = minimumY; y <= maximumY; y += GRID_SIZE) {
        for (let x = minimumX; x <= maximumX; x += GRID_SIZE) {
            const candidate = { x, y }
            if (nodes.some((node) => collides(candidate, node))) continue

            const distance = (candidate.x - requested.x) ** 2 + (candidate.y - requested.y) ** 2
            if (distance < closestDistance
                || (distance === closestDistance && (closest === null || candidate.y < closest.y || (candidate.y === closest.y && candidate.x < closest.x)))) {
                closest = candidate
                closestDistance = distance
            }
        }
    }

    return closest
}

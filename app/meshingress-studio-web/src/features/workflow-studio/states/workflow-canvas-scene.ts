import stateEnums from './enums'
import type { WorkflowEdge, WorkflowNode } from '../types'

export const workflowCanvasStateCatalog = stateEnums

export type WorkflowCanvasStateCategory = keyof typeof workflowCanvasStateCatalog
export type WorkflowCanvasSceneTargetKind = 'canvas' | 'node' | 'path' | 'slot' | 'workflow'

export interface WorkflowCanvasPlacement {
  nodeId: string
  q: number
  r: number
}

export interface WorkflowCanvasSceneData {
  edges: WorkflowEdge[]
  nodes: WorkflowNode[]
  placements: WorkflowCanvasPlacement[]
}

export interface WorkflowCanvasSceneElement {
  attributes: Record<string, string>
  states: Partial<Record<WorkflowCanvasStateCategory, string | string[]>>
}

export interface WorkflowCanvasScene {
  canvas: WorkflowCanvasSceneElement
  data: WorkflowCanvasSceneData
  nodes: Record<string, WorkflowCanvasSceneElement>
  paths: Record<string, WorkflowCanvasSceneElement>
  slots: Record<string, WorkflowCanvasSceneElement>
  workflow: WorkflowCanvasSceneElement
}

const targetCategories: Record<WorkflowCanvasSceneTargetKind, readonly WorkflowCanvasStateCategory[]> = {
  canvas: ['canvasInteraction'],
  workflow: ['workflowState'],
  slot: ['slotOccupancy', 'slotDropState'],
  node: ['nodeExecutionState', 'nodeConnectionState', 'nodeOverlay'],
  path: ['pathLifecycleState', 'pathFlowState'],
}

function createElement(attributes: Record<string, string> = {}): WorkflowCanvasSceneElement {
  return { attributes, states: {} }
}

function cloneElement(element: WorkflowCanvasSceneElement): WorkflowCanvasSceneElement {
  return { attributes: { ...element.attributes }, states: { ...element.states } }
}

function cloneElements(elements: Record<string, WorkflowCanvasSceneElement>): Record<string, WorkflowCanvasSceneElement> {
  return Object.fromEntries(Object.entries(elements).map(([id, element]) => [id, cloneElement(element)]))
}

function cloneScene(scene: WorkflowCanvasScene): WorkflowCanvasScene {
  return {
    canvas: cloneElement(scene.canvas),
    data: {
      edges: [...scene.data.edges],
      nodes: [...scene.data.nodes],
      placements: [...scene.data.placements],
    },
    nodes: cloneElements(scene.nodes),
    paths: cloneElements(scene.paths),
    slots: cloneElements(scene.slots),
    workflow: cloneElement(scene.workflow),
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function isSceneElement(value: unknown): value is WorkflowCanvasSceneElement {
  if (!isRecord(value) || !isRecord(value.attributes) || !isRecord(value.states)) return false
  return Object.values(value.attributes).every((attribute) => typeof attribute === 'string')
    && Object.values(value.states).every((state) => typeof state === 'string' || (Array.isArray(state) && state.every((entry) => typeof entry === 'string')))
}

function isSceneElementMap(value: unknown): value is Record<string, WorkflowCanvasSceneElement> {
  return isRecord(value) && Object.values(value).every(isSceneElement)
}

function isSceneData(value: unknown): value is WorkflowCanvasSceneData {
  return isRecord(value) && Array.isArray(value.edges) && Array.isArray(value.nodes) && Array.isArray(value.placements)
}

function stateAttributeName(category: WorkflowCanvasStateCategory): string {
  return `data-${category.replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`)}`
}

function appliesState(category: WorkflowCanvasStateCategory, state: string): boolean {
  return (workflowCanvasStateCatalog[category] as readonly string[]).includes(state)
}

function assignState(element: WorkflowCanvasSceneElement, category: WorkflowCanvasStateCategory, state: string): void {
  if (!appliesState(category, state)) throw new Error(`${state} is not a valid ${category} state.`)
  element.states[category] = state
  element.attributes[stateAttributeName(category)] = state
}

function assignNodeOverlay(element: WorkflowCanvasSceneElement, state: string, active = true): void {
  if (!appliesState('nodeOverlay', state)) throw new Error(`${state} is not a valid nodeOverlay state.`)

  const current = element.states.nodeOverlay
  const overlays = new Set(Array.isArray(current) ? current : current ? [current] : [])

  if (active) overlays.add(state)
  else overlays.delete(state)

  const next = [...overlays]

  if (next.length === 0) delete element.states.nodeOverlay
  else element.states.nodeOverlay = next

  if (next.length === 0) delete element.attributes[stateAttributeName('nodeOverlay')]
  else element.attributes[stateAttributeName('nodeOverlay')] = next.join(' ')
}

/**
 * Parses outside scene data into the one shape that canvas renderers consume.
 * This deliberately rejects invalid enum values before they become DOM state.
 */
export function parseWorkflowCanvasScene(input: unknown): WorkflowCanvasScene {
  if (!isRecord(input)
    || !isSceneElement(input.canvas)
    || !isSceneData(input.data)
    || !isSceneElementMap(input.nodes)
    || !isSceneElementMap(input.paths)
    || !isSceneElementMap(input.slots)
    || !isSceneElement(input.workflow)) {
    throw new Error('Workflow canvas scene must contain normalized canvas, workflow, slot, node, path, and data records.')
  }
  const scene = cloneScene(input as unknown as WorkflowCanvasScene)
  const validate = (element: WorkflowCanvasSceneElement, target: WorkflowCanvasSceneTargetKind) => {
    Object.entries(element.states).forEach(([category, state]) => {
      if (!targetCategories[target].includes(category as WorkflowCanvasStateCategory)) {
        throw new Error(`${category} cannot be assigned to a ${target}.`)
      }
      if (category === 'nodeOverlay') {
        const overlays = Array.isArray(state) ? state : [state]
        delete element.states.nodeOverlay
        overlays.forEach((overlay) => assignNodeOverlay(element, overlay))
      } else if (typeof state === 'string') {
        assignState(element, category as WorkflowCanvasStateCategory, state)
      } else {
        throw new Error(`${category} accepts one state value.`)
      }
    })
  }
  validate(scene.canvas, 'canvas')
  validate(scene.workflow, 'workflow')
  Object.values(scene.slots).forEach((element) => validate(element, 'slot'))
  Object.values(scene.nodes).forEach((element) => validate(element, 'node'))
  Object.values(scene.paths).forEach((element) => validate(element, 'path'))
  return scene
}

export function createWorkflowCanvasScene(data: WorkflowCanvasSceneData): WorkflowCanvasScene {
  const slots = Object.fromEntries(data.placements.map(({ q, r }) => [`slot/q${q}-r${r}`, createElement()]))
  const nodes = Object.fromEntries(data.nodes.map((node) => [node.id, createElement()]))
  const paths = Object.fromEntries(data.edges.map((edge) => [`${edge.source}->${edge.target}`, createElement()]))
  const scene: WorkflowCanvasScene = {
    canvas: createElement(),
    data: { edges: [...data.edges], nodes: [...data.nodes], placements: [...data.placements] },
    nodes,
    paths,
    slots,
    workflow: createElement(),
  }
  assignState(scene.canvas, 'canvasInteraction', 'idle')
  assignState(scene.workflow, 'workflowState', 'incomplete')
  Object.values(scene.slots).forEach((slot) => assignState(slot, 'slotOccupancy', 'empty'))
  data.placements.forEach(({ nodeId, q, r }) => assignState(scene.slots[`slot/q${q}-r${r}`], 'slotOccupancy', nodeId ? 'occupied' : 'empty'))
  Object.values(scene.nodes).forEach((node) => {
    assignState(node, 'nodeExecutionState', 'idle')
    assignState(node, 'nodeConnectionState', 'isolated')
  })
  Object.values(scene.paths).forEach((path) => {
    assignState(path, 'pathLifecycleState', 'connected')
    assignState(path, 'pathFlowState', 'idle')
  })
  return scene
}

export function stateCategoriesFor(target: WorkflowCanvasSceneTargetKind): readonly WorkflowCanvasStateCategory[] {
  return targetCategories[target]
}

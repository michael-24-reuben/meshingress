import { describe, expect, it } from 'vitest'
import { createStudioWorkbenchTmpPlacements, honeycombExecutionStateForRunState, honeycombNodeConnectionStateForEdges, honeycombPathStatesForRun, honeycombWorkflowStateForRun, syncHoneycombPathStates } from '../features/workflow-studio/components/elements/studio-workbench-tmp-state'
import { WorkflowModelSnapshotFactory } from '../features/workflow-studio/states/factories/WorkflowModelSnapshotFactory'
import { StateCategory } from '../features/workflow-studio/states/WorkflowStudioState'
import type { WorkflowNode } from '../features/workflow-studio/types'

const node = (id: string): WorkflowNode => ({
  id,
  title: id,
  kind: 'tool',
  toolId: 'test.tool',
  functionName: 'run',
  output: id,
  x: 0,
  y: 0,
  arguments: {},
  annotations: { audit: false, scopes: [] },
  capabilities: { acceptsInput: true, acceptsOutput: true },
})

describe('StudioWorkbenchTmp', () => {
  it('maps every live Studio run state to Honeycomb execution state', () => {
    expect(honeycombExecutionStateForRunState(undefined)).toBe(StateCategory.ofIdle)
    expect(honeycombExecutionStateForRunState(StateCategory.ofIdle)).toBe(StateCategory.ofIdle)
    expect(honeycombExecutionStateForRunState(StateCategory.ofRunning)).toBe(StateCategory.ofRunning)
    expect(honeycombExecutionStateForRunState(StateCategory.ofSuccess)).toBe(StateCategory.ofSuccess)
    expect(honeycombExecutionStateForRunState(StateCategory.ofError)).toBe(StateCategory.ofError)
  })

  it('preserves prototype positions and assigns every temporary node exactly once', () => {
    const placements = createStudioWorkbenchTmpPlacements([node('r-001'), node('r-002'), node('r-004'), node('r-009')])

    expect(placements).toEqual(expect.arrayContaining([
      { nodeId: 'r-001', q: 0, r: 2 },
      { nodeId: 'r-002', q: 1, r: 2 },
      { nodeId: 'r-004', q: 4, r: 2 },
    ]))
    expect(new Set(placements.map(({ nodeId }) => nodeId))).toEqual(new Set(['r-001', 'r-002', 'r-004', 'r-009']))
    expect(new Set(placements.map(({ q, r }) => `${q}/${r}`))).toHaveLength(placements.length)
  })

  it('derives only unambiguous workflow and node-connection states', () => {
    const nodes = [node('r-001'), node('r-002')]

    expect(honeycombWorkflowStateForRun(nodes, {}, false)).toBe(StateCategory.ofIncomplete)
    expect(honeycombWorkflowStateForRun(nodes, {}, true)).toBe(StateCategory.ofExecuting)
    expect(honeycombWorkflowStateForRun(nodes, { 'r-001': StateCategory.ofSuccess, 'r-002': StateCategory.ofSuccess }, false)).toBe(StateCategory.ofCompleted)
    expect(honeycombWorkflowStateForRun(nodes, { 'r-001': StateCategory.ofError }, false)).toBe(StateCategory.ofFailed)
    expect(honeycombNodeConnectionStateForEdges('r-001', [{ source: 'r-001', target: 'r-002' }])).toBe(StateCategory.ofConnected)
    expect(honeycombNodeConnectionStateForEdges('r-009', [{ source: 'r-001', target: 'r-002' }])).toBe(StateCategory.ofIsolated)
  })

  it('validates path endpoints, capabilities, self-links, and cycles independently of flow', () => {
    const nodes = [node('a'), node('b'), node('c'), node('d')]
    const states = honeycombPathStatesForRun(nodes, [
      { source: 'a', target: 'b' }, { source: 'b', target: 'a' },
      { source: 'b', target: 'c' }, { source: 'c', target: 'c' },
      { source: 'missing', target: 'd' }, { source: 'd', target: 'missing' },
    ], { a: 'success', b: 'success' }, false)
    expect(states.get('a->b')).toEqual({ pathLifecycleState: 'invalid', pathFlowState: 'idle' })
    expect(states.get('b->a')?.pathLifecycleState).toBe('invalid')
    expect(states.get('b->c')?.pathLifecycleState).toBe('connected')
    expect(states.get('c->c')?.pathLifecycleState).toBe('invalid')
    expect(states.get('missing->d')?.pathLifecycleState).toBe('unconnected')
    expect(states.get('d->missing')?.pathLifecycleState).toBe('unconnected')

    nodes[0].capabilities.acceptsOutput = false
    expect(honeycombPathStatesForRun(nodes, [{ source: 'a', target: 'b' }], {}, false).get('a->b')?.pathLifecycleState).toBe('invalid')
    nodes[0].capabilities.acceptsOutput = true
    nodes[1].capabilities.acceptsInput = false
    expect(honeycombPathStatesForRun(nodes, [{ source: 'a', target: 'b' }], {}, false).get('a->b')?.pathLifecycleState).toBe('invalid')
  })

  it('projects delivery only from both endpoint states and never from source execution alone', () => {
    const nodes = [node('a'), node('b')]
    const edges = [{ source: 'a', target: 'b' }]
    const flow = (runStates: Parameters<typeof honeycombPathStatesForRun>[2], running = true) =>
      honeycombPathStatesForRun(nodes, edges, runStates, running).get('a->b')!.pathFlowState
    expect(flow({})).toBe('idle')
    expect(flow({ a: 'running' })).toBe('idle')
    expect(flow({ a: 'success' })).toBe('idle')
    expect(flow({ b: 'running' })).toBe('idle')
    expect(flow({ a: 'success', b: 'running' })).toBe('active')
    expect(flow({ a: 'success', b: 'running' }, false)).toBe('idle')
    expect(flow({ a: 'success', b: 'success' }, false)).toBe('success')
    expect(flow({ a: 'success', b: 'error' }, false)).toBe('error')
  })

  it('blocks waiting descendants of an upstream failure without contaminating other branches', () => {
    const nodes = ['a', 'b', 'c', 'd', 'x', 'y'].map(node)
    const edges = [
      { source: 'a', target: 'b' }, { source: 'b', target: 'c' }, { source: 'c', target: 'd' },
      { source: 'x', target: 'y' },
    ]
    const states = honeycombPathStatesForRun(nodes, edges, { a: 'error', x: 'success', y: 'success' }, false)
    expect(states.get('a->b')).toEqual({ pathLifecycleState: 'blocked', pathFlowState: 'error' })
    expect(states.get('b->c')).toEqual({ pathLifecycleState: 'blocked', pathFlowState: 'idle' })
    expect(states.get('c->d')?.pathLifecycleState).toBe('blocked')
    expect(states.get('x->y')).toEqual({ pathLifecycleState: 'connected', pathFlowState: 'success' })
    // A target with another successful incoming branch can still execute.
    expect(honeycombPathStatesForRun(nodes, edges, { a: 'error', b: 'success', c: 'running' }, true).get('b->c'))
      .toEqual({ pathLifecycleState: 'connected', pathFlowState: 'active' })
  })

  it('updates root path attributes through a full run, failure, and rerun reset', () => {
    const nodes = [node('a'), node('b')]
    const edges = [{ source: 'a', target: 'b' }]
    const model = new WorkflowModelSnapshotFactory().create({ nodes, edges, placements: createStudioWorkbenchTmpPlacements(nodes) })
    const events: string[] = []
    const unsubscribe = model.subscribe((event) => events.push(event.category))
    const attributes = () => model.getPathModel('a->b')!.getDataAttributes()
    syncHoneycombPathStates(model, nodes, edges, { a: 'success', b: 'running' }, true)
    expect(attributes()).toMatchObject({ 'data-path-lifecycle-state': 'connected', 'data-path-flow-state': 'active' })
    syncHoneycombPathStates(model, nodes, edges, { a: 'success', b: 'success' }, false)
    expect(attributes()['data-path-flow-state']).toBe('success')
    syncHoneycombPathStates(model, nodes, edges, { a: 'error' }, false)
    expect(attributes()).toMatchObject({ 'data-path-lifecycle-state': 'blocked', 'data-path-flow-state': 'error' })
    syncHoneycombPathStates(model, nodes, edges, { a: 'idle', b: 'idle' }, true)
    expect(attributes()).toMatchObject({ 'data-path-lifecycle-state': 'connected', 'data-path-flow-state': 'idle' })
    expect(events).toContain('pathLifecycleState')
    expect(events).toContain('pathFlowState')
    unsubscribe()
  })
})

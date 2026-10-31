import type { JSX } from "react"
import { validateWorkflowNodeDataPolicy, type WorkflowEdge, type WorkflowNode, type WorkflowNodeCapabilities } from "../types"
import enums from "./enums"


export const CanvasInteractionValues = enums.canvasInteraction
export type CanvasInteraction = typeof CanvasInteractionValues[number]

export const WorkflowStateValues = enums.workflowState
export type WorkflowState = typeof WorkflowStateValues[number]

export const SlotOccupancyValues = enums.slotOccupancy
export type SlotOccupancy = typeof SlotOccupancyValues[number]

export const SlotDropStateValues = enums.slotDropState
export type SlotDropState = typeof SlotDropStateValues[number]

export const NodeExecutionStateValues = enums.nodeExecutionState
export type NodeExecutionState = typeof NodeExecutionStateValues[number]

export const NodeConnectionStateValues = enums.nodeConnectionState
export type NodeConnectionState = typeof NodeConnectionStateValues[number]

export const NodeOverlayValues = enums.nodeOverlay
export type NodeOverlay = typeof NodeOverlayValues[number]

export const PathLifecycleStateValues = enums.pathLifecycleState
export type PathLifecycleState = typeof PathLifecycleStateValues[number]

export const PathFlowStateValues = enums.pathFlowState
export type PathFlowState = typeof PathFlowStateValues[number]

export type StateCategory =
    | CanvasInteraction
    | WorkflowState
    | SlotOccupancy
    | SlotDropState
    | NodeExecutionState
    | NodeConnectionState
    | NodeOverlay
    | PathLifecycleState
    | PathFlowState

/**
 * Variable-backed catalog of every distinct Studio state literal.
 *
 * The existing string contracts remain unchanged. Use this object when a
 * state is referenced in code (for example, `StateCategory.ofIdle`) so state
 * usage is searchable without introducing a second state model.
 */
export const StateCategory = {
    // Canvas Interaction
    ofIdle: 'idle',
    ofPanning: 'panning',
    ofZooming: 'zooming',
    ofSelecting: 'selecting',
    ofDraggingNode: 'dragging-node',
    ofDrawingPath: 'drawing-path',
    ofReconnectingPath: 'reconnecting-path',
    ofLayoutRevealing: 'layout-revealing',
    // Workflow State
    ofValid: 'valid',
    ofIncomplete: 'incomplete',
    ofDisconnected: 'disconnected',
    ofUnreachable: 'unreachable',
    ofCyclic: 'cyclic',
    ofBlocked: 'blocked',
    ofExecuting: 'executing',
    ofCompleted: 'completed',
    ofFailed: 'failed',
    // Slot Occupancy
    ofEmpty: 'empty',
    ofOccupied: 'occupied',
    ofReserved: 'reserved',
    ofOutOfBounds: 'out-of-bounds',
    // Slot Drop State
    ofNone: 'none',
    ofValidDropTarget: 'valid-drop-target',
    ofInvalidDropTarget: 'invalid-drop-target',
    // Node Execution State
    ofRunning: 'running',
    ofSuccess: 'success',
    ofWarning: 'warning',
    ofError: 'error',
    ofDisabled: 'disabled',
    ofUnavailable: 'unavailable',
    // Node Connection State
    ofIsolated: 'isolated',
    ofConnected: 'connected',
    ofMissingRequiredInput: 'missing-required-input',
    ofBlockedByUpstreamError: 'blocked-by-upstream-error',
    ofStaleOutput: 'stale-output',
    // Node Overlay
    ofSelected: 'selected',
    ofHovered: 'hovered',
    ofFocused: 'focused',
    ofBookmarked: 'bookmarked',
    ofDragging: 'dragging',
    ofLiftedFromOrigin: 'lifted-from-origin',
    ofHasUnsavedChanges: 'has-unsaved-changes',
    ofInvalid: 'invalid',
    // Path Lifecycle State
    ofUnconnected: 'unconnected',
    ofDrafting: 'drafting',
    ofPendingValidation: 'pending-validation',
    // Path Flow State
    ofActive: 'active',
    ofMuted: 'muted',
} as const satisfies Record<string, StateCategory>

export type PhaseMultiplicity = 'single' | 'multiple'


/**
 * State category to phase type mapping, utilizing the specific enum types defined above.
 */
export interface WorkflowStudioCategoryStateMap {
    canvasInteraction: CanvasInteraction;
    workflowState: WorkflowState;
    slotOccupancy: SlotOccupancy;
    slotDropState: SlotDropState;
    nodeExecutionState: NodeExecutionState;
    nodeConnectionState: NodeConnectionState;
    nodeOverlay: NodeOverlay[];
    pathLifecycleState: PathLifecycleState;
    pathFlowState: PathFlowState;
}

export type WorkflowStudioStateCategory = keyof WorkflowStudioCategoryStateMap;

export const WorkflowStudioCategoryValues: { [K in WorkflowStudioStateCategory]: readonly string[] } = {
    canvasInteraction: CanvasInteractionValues,
    workflowState: WorkflowStateValues,
    slotOccupancy: SlotOccupancyValues,
    slotDropState: SlotDropStateValues,
    nodeExecutionState: NodeExecutionStateValues,
    nodeConnectionState: NodeConnectionStateValues,
    nodeOverlay: NodeOverlayValues,
    pathLifecycleState: PathLifecycleStateValues,
    pathFlowState: PathFlowStateValues,
};

/**
 * Returns the renderer attribute name shared by every Studio state category.
 */
export function workflowStudioStateAttributeName(category: WorkflowStudioStateCategory): string {
    return `data-${category.replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`)}`
}

/**
 * Normalized canvas scene data that model snapshots may consume without
 * depending on the legacy scene tracker. This is intentionally parse-only:
 * creating defaults and dispatching mutations remain outside this state layer
 * until they are explicitly migrated.
 */
export interface WorkflowStudioScenePlacement {
    nodeId: string;
    q: number;
    r: number;
}

export interface WorkflowStudioSceneData {
    edges: WorkflowEdge[];
    nodes: WorkflowNode[];
    placements: WorkflowStudioScenePlacement[];
}

export interface WorkflowStudioElementStates {
    canvasInteraction?: CanvasInteraction;
    workflowState?: WorkflowState;
    slotOccupancy?: SlotOccupancy;
    slotDropState?: SlotDropState;
    nodeExecutionState?: NodeExecutionState;
    nodeConnectionState?: NodeConnectionState;
    nodeOverlay?: NodeOverlay | NodeOverlay[];
    pathLifecycleState?: PathLifecycleState;
    pathFlowState?: PathFlowState;
}

export interface WorkflowStudioSceneElement<TStates extends Partial<WorkflowStudioElementStates> = Partial<WorkflowStudioElementStates>> {
    attributes: Record<string, string>;
    states: TStates;
}

export type WorkflowStudioCanvasStates = { canvasInteraction?: CanvasInteraction };
export type WorkflowStudioWorkflowStates = { workflowState?: WorkflowState };
export type WorkflowStudioSlotStates = { slotOccupancy?: SlotOccupancy; slotDropState?: SlotDropState };
export type WorkflowStudioNodeStates = { nodeExecutionState?: NodeExecutionState; nodeConnectionState?: NodeConnectionState; nodeOverlay?: NodeOverlay | NodeOverlay[] };
export type WorkflowStudioPathStates = { pathLifecycleState?: PathLifecycleState; pathFlowState?: PathFlowState };

export interface WorkflowStudioScene {
    canvas: WorkflowStudioSceneElement<WorkflowStudioCanvasStates>;
    data: WorkflowStudioSceneData;
    nodes: Record<string, WorkflowStudioSceneElement<WorkflowStudioNodeStates>>;
    paths: Record<string, WorkflowStudioSceneElement<WorkflowStudioPathStates>>;
    slots: Record<string, WorkflowStudioSceneElement<WorkflowStudioSlotStates>>;
    workflow: WorkflowStudioSceneElement<WorkflowStudioWorkflowStates>;
}

export type WorkflowStudioSceneTargetKind = 'canvas' | 'node' | 'path' | 'slot' | 'workflow';

export interface WorkflowStudioCanvasTargetPayload {
    readonly kind: 'canvas';
}

export interface WorkflowStudioWorkflowTargetPayload {
    readonly kind: 'workflow';
}

export interface WorkflowStudioNodeTargetPayload {
    readonly kind: 'node';
    readonly nodeId: string;
}

export interface WorkflowStudioSlotTargetPayload {
    readonly kind: 'slot';
    readonly slotId: string;
}

export interface WorkflowStudioPathTargetPayload {
    readonly kind: 'path';
    readonly pathId: string;
}

export type WorkflowStudioSceneTargetPayload =
    | WorkflowStudioCanvasTargetPayload
    | WorkflowStudioWorkflowTargetPayload
    | WorkflowStudioNodeTargetPayload
    | WorkflowStudioSlotTargetPayload
    | WorkflowStudioPathTargetPayload;

export const workflowStudioSceneTargetCategories: Record<WorkflowStudioSceneTargetKind, readonly WorkflowStudioStateCategory[]> = {
    canvas: ['canvasInteraction'],
    workflow: ['workflowState'],
    slot: ['slotOccupancy', 'slotDropState'],
    node: ['nodeExecutionState', 'nodeConnectionState', 'nodeOverlay'],
    path: ['pathLifecycleState', 'pathFlowState'],
};

function isWorkflowStudioRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function isWorkflowStudioSceneElement(value: unknown): value is WorkflowStudioSceneElement {
    if (!isWorkflowStudioRecord(value) || !isWorkflowStudioRecord(value.attributes) || !isWorkflowStudioRecord(value.states)) return false
    return Object.values(value.attributes).every((attribute) => typeof attribute === 'string')
        && Object.values(value.states).every((state) => typeof state === 'string' || (Array.isArray(state) && state.every((entry) => typeof entry === 'string')))
}

function isWorkflowStudioSceneElementMap(value: unknown): value is Record<string, WorkflowStudioSceneElement> {
    return isWorkflowStudioRecord(value) && Object.values(value).every(isWorkflowStudioSceneElement)
}

function isWorkflowStudioSceneData(value: unknown): value is WorkflowStudioSceneData {
    return isWorkflowStudioRecord(value) && Array.isArray(value.edges) && Array.isArray(value.nodes) && Array.isArray(value.placements)
}

function cloneWorkflowStudioSceneElement<TStates extends Partial<WorkflowStudioElementStates>>(
    element: WorkflowStudioSceneElement<TStates>,
): WorkflowStudioSceneElement<TStates> {
    return { attributes: { ...element.attributes }, states: { ...element.states } }
}

function cloneWorkflowStudioSceneElements(
    elements: Record<string, WorkflowStudioSceneElement>,
): Record<string, WorkflowStudioSceneElement> {
    return Object.fromEntries(Object.entries(elements).map(([id, element]) => [id, cloneWorkflowStudioSceneElement(element)]))
}

function cloneWorkflowStudioScene(scene: WorkflowStudioScene): WorkflowStudioScene {
    return {
        canvas: cloneWorkflowStudioSceneElement(scene.canvas),
        data: {
            edges: [...scene.data.edges],
            nodes: [...scene.data.nodes],
            placements: [...scene.data.placements],
        },
        nodes: cloneWorkflowStudioSceneElements(scene.nodes),
        paths: cloneWorkflowStudioSceneElements(scene.paths),
        slots: cloneWorkflowStudioSceneElements(scene.slots),
        workflow: cloneWorkflowStudioSceneElement(scene.workflow),
    }
}

function workflowStudioSceneStates(element: WorkflowStudioSceneElement): Partial<Record<WorkflowStudioStateCategory, string | string[]>> {
    return element.states as Partial<Record<WorkflowStudioStateCategory, string | string[]>>
}

function assignWorkflowStudioSceneState(
    element: WorkflowStudioSceneElement,
    category: WorkflowStudioStateCategory,
    state: string,
): void {
    if (!WorkflowStudioCategoryValues[category].includes(state)) {
        throw new Error(`${state} is not a valid ${category} state.`)
    }
    workflowStudioSceneStates(element)[category] = state
    element.attributes[workflowStudioStateAttributeName(category)] = state
}

function assignWorkflowStudioNodeOverlay(element: WorkflowStudioSceneElement, state: string, active = true): void {
    if (!WorkflowStudioCategoryValues.nodeOverlay.includes(state)) {
        throw new Error(`${state} is not a valid nodeOverlay state.`)
    }

    const states = workflowStudioSceneStates(element)
    const current = states.nodeOverlay
    const overlays = new Set(Array.isArray(current) ? current : current ? [current] : [])
    if (active) overlays.add(state)
    else overlays.delete(state)

    const next = [...overlays]
    if (next.length === 0) delete states.nodeOverlay
    else states.nodeOverlay = next

    const attributeName = workflowStudioStateAttributeName('nodeOverlay')
    if (next.length === 0) delete element.attributes[attributeName]
    else element.attributes[attributeName] = next.join(' ')
}

/**
 * Validates and normalizes externally supplied scene-shaped data for the
 * state contract. It does not create defaults, dispatch mutations, or replace
 * existing legacy scene-parser callers.
 */
export function parseWorkflowStudioScene(input: unknown): WorkflowStudioScene {
    if (!isWorkflowStudioRecord(input)
        || !isWorkflowStudioSceneElement(input.canvas)
        || !isWorkflowStudioSceneData(input.data)
        || !isWorkflowStudioSceneElementMap(input.nodes)
        || !isWorkflowStudioSceneElementMap(input.paths)
        || !isWorkflowStudioSceneElementMap(input.slots)
        || !isWorkflowStudioSceneElement(input.workflow)) {
        throw new Error('Workflow studio scene must contain normalized canvas, workflow, slot, node, path, and data records.')
    }

    const scene = cloneWorkflowStudioScene(input as unknown as WorkflowStudioScene)
    const validate = (element: WorkflowStudioSceneElement, target: WorkflowStudioSceneTargetKind) => {
        Object.entries(workflowStudioSceneStates(element)).forEach(([category, state]) => {
            if (!workflowStudioSceneTargetCategories[target].includes(category as WorkflowStudioStateCategory)) {
                throw new Error(`${category} cannot be assigned to a ${target}.`)
            }
            if (category === 'nodeOverlay') {
                const overlays = Array.isArray(state) ? state : [state]
                delete workflowStudioSceneStates(element).nodeOverlay
                overlays.forEach((overlay) => assignWorkflowStudioNodeOverlay(element, overlay))
            } else if (typeof state === 'string') {
                assignWorkflowStudioSceneState(element, category as WorkflowStudioStateCategory, state)
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


export interface PhaseChangeEvent<Phase extends StateCategory> {
    phase: Phase;
    active: boolean;
    activePhases: Phase[];
    target: StudioNodeSnapshot<Phase>;
}

export type PhaseChangeListener<Phase extends StateCategory> = (event: PhaseChangeEvent<Phase>) => void;
export type SpecificPhaseListener = (active: boolean) => void;

export interface WorkflowStudioSceneChange<Phase extends StateCategory = StateCategory> {
    readonly target: WorkflowStudioSceneTargetPayload;
    readonly category: WorkflowStudioStateCategory;
    readonly phase: Phase;
    readonly active: boolean;
    readonly activePhases: readonly Phase[];
}

export type WorkflowStudioSceneChangeListener = (change: WorkflowStudioSceneChange) => void;

export interface WorkflowStudioCanvasInteractionMutation {
    readonly target: WorkflowStudioCanvasTargetPayload;
    readonly category: 'canvasInteraction';
    readonly phase: CanvasInteraction;
}

export interface WorkflowStudioWorkflowStateMutation {
    readonly target: WorkflowStudioWorkflowTargetPayload;
    readonly category: 'workflowState';
    readonly phase: WorkflowState;
}

export interface WorkflowStudioSlotOccupancyMutation {
    readonly target: WorkflowStudioSlotTargetPayload;
    readonly category: 'slotOccupancy';
    readonly phase: SlotOccupancy;
}

export interface WorkflowStudioSlotDropStateMutation {
    readonly target: WorkflowStudioSlotTargetPayload;
    readonly category: 'slotDropState';
    readonly phase: SlotDropState;
}

export interface WorkflowStudioNodeExecutionStateMutation {
    readonly target: WorkflowStudioNodeTargetPayload;
    readonly category: 'nodeExecutionState';
    readonly phase: NodeExecutionState;
}

export interface WorkflowStudioNodeConnectionStateMutation {
    readonly target: WorkflowStudioNodeTargetPayload;
    readonly category: 'nodeConnectionState';
    readonly phase: NodeConnectionState;
}

export interface WorkflowStudioNodeOverlayMutation {
    readonly target: WorkflowStudioNodeTargetPayload;
    readonly category: 'nodeOverlay';
    readonly phase: NodeOverlay;
    readonly active?: boolean;
}

export interface WorkflowStudioPathLifecycleStateMutation {
    readonly target: WorkflowStudioPathTargetPayload;
    readonly category: 'pathLifecycleState';
    readonly phase: PathLifecycleState;
}

export interface WorkflowStudioPathFlowStateMutation {
    readonly target: WorkflowStudioPathTargetPayload;
    readonly category: 'pathFlowState';
    readonly phase: PathFlowState;
}

/**
 * State-side mutation input for every current phase-state category. Capabilities
 * remain node data rather than a phase-state mutation.
 */
export type WorkflowStudioSceneMutation =
    | WorkflowStudioCanvasInteractionMutation
    | WorkflowStudioWorkflowStateMutation
    | WorkflowStudioSlotOccupancyMutation
    | WorkflowStudioSlotDropStateMutation
    | WorkflowStudioNodeExecutionStateMutation
    | WorkflowStudioNodeConnectionStateMutation
    | WorkflowStudioNodeOverlayMutation
    | WorkflowStudioPathLifecycleStateMutation
    | WorkflowStudioPathFlowStateMutation;

export abstract class StudioNodeSnapshot<Phase extends StateCategory> {
    protected _phases: { [key in Phase]: boolean };
    protected _basePhase: Phase;
    protected readonly attributeName: string;
    protected readonly phaseMultiplicity: PhaseMultiplicity;

    // Listener registries
    private _listeners: Set<PhaseChangeListener<Phase>> = new Set();
    private _specificListeners: Map<Phase, Set<SpecificPhaseListener>> = new Map();

    // Batching state
    private _batchDepth: number = 0;
    private _pendingNotifications: Map<Phase, boolean> = new Map();
    private _initialBatchPhases: Map<Phase, boolean> | null = null;

    protected constructor(
        phases: { [key in Phase]: boolean },
        basePhase: Phase,
        category: WorkflowStudioStateCategory,
        phaseMultiplicity: PhaseMultiplicity = 'single',
    ) {
        this._basePhase = basePhase;
        this.attributeName = this.stateAttributeName(category);
        this.phaseMultiplicity = phaseMultiplicity;

        // Wrap _phases with a Proxy so any direct mutation `this._phases[key] = val` 
        // automatically notifies listeners (or queues notifications if inside a batch)!
        this._phases = new Proxy({ ...phases }, {
            set: (target, prop, value) => {
                const phase = prop as Phase;
                const prev = target[phase];
                const next = Boolean(value);
                target[phase] = next;
                if (prev !== next) {
                    if (this._batchDepth > 0 && this._initialBatchPhases) {
                        const initial = this._initialBatchPhases.get(phase);
                        if (initial !== next) {
                            this._pendingNotifications.set(phase, next);
                        } else {
                            this._pendingNotifications.delete(phase);
                        }
                    } else if (this._batchDepth === 0) {
                        this._notifyListeners(phase, next);
                    }
                }
                return true;
            }
        });
    }

    public abstract init(): void;

    /**
     * Returns the attribute name for a given state category.
     * @param category The state category.
     * @returns The attribute name.
     */
    private stateAttributeName(category: WorkflowStudioStateCategory): string {
        return workflowStudioStateAttributeName(category)
    }

    /**
     * Executes a callback function in a batch, suppressing intermediate listener notifications
     * until all mutations in the batch complete. Once the outermost batch finishes, listeners
     * are notified of the final phase changes with consistent active phases.
     * @param fn The callback to execute.
     * @returns The result of the callback.
     */
    public batch<T>(fn: () => T): T {
        const isRoot = this._batchDepth === 0;
        if (isRoot) {
            this._initialBatchPhases = new Map(
                Object.entries(this._phases).map(([k, v]) => [k as Phase, Boolean(v)])
            );
        }
        this._batchDepth++;
        try {
            return fn();
        } finally {
            this._batchDepth--;
            if (this._batchDepth === 0) {
                this._flushPendingNotifications();
                this._initialBatchPhases = null;
            }
        }
    }

    private _flushPendingNotifications(): void {
        if (this._pendingNotifications.size === 0) return;
        const notifications = Array.from(this._pendingNotifications.entries());
        this._pendingNotifications.clear();
        for (const [phase, active] of notifications) {
            this._notifyListeners(phase, active);
        }
    }

    /* -------------------------------------------------------------
     *  GUI-Style Listener Registration & Subscriptions
     * ------------------------------------------------------------- */
    /**
     * Subscribes a listener to any phase change on this node snapshot.
     * @returns An unsubscribe cleanup function.
     */
    public addPhaseChangeListener(listener: PhaseChangeListener<Phase>): () => void {
        this._listeners.add(listener);
        return () => this._listeners.delete(listener);
    }

    public removePhaseChangeListener(listener: PhaseChangeListener<Phase>): void {
        this._listeners.delete(listener);
    }

    /**
     * Subscribes a listener to a specific phase change (e.g., 'panning' or 'idle').
     * @returns An unsubscribe cleanup function.
     */
    public on(phase: Phase, listener: SpecificPhaseListener): () => void {
        if (!this._specificListeners.has(phase)) {
            this._specificListeners.set(phase, new Set());
        }
        this._specificListeners.get(phase)!.add(listener);
        return () => this._specificListeners.get(phase)?.delete(listener);
    }

    public off(phase: Phase, listener: SpecificPhaseListener): void {
        this._specificListeners.get(phase)?.delete(listener);
    }

    /**
     * Dispatches phase change events to all registered listeners.
     */
    protected _notifyListeners(phase: Phase, active: boolean): void {
        const event: PhaseChangeEvent<Phase> = {
            phase,
            active,
            activePhases: this.getActivePhases(),
            target: this
        };
        // Notify global listeners
        this._listeners.forEach((listener) => {
            try {
                listener(event);
            } catch (err) {
                console.error(`Error in phase change listener for ${String(phase)}:`, err);
            }
        });
        // Notify specific phase listeners
        const specific = this._specificListeners.get(phase);
        if (specific) {
            specific.forEach((listener) => {
                try {
                    listener(active);
                } catch (err) {
                    console.error(`Error in specific phase listener for ${String(phase)}:`, err);
                }
            });
        }
    }

    /* -------------------------------------------------------------
     *  Phase State Operations
     * ------------------------------------------------------------- */
    /**
     * Sets a phase to asynchronous (non-blocking/non-integratable) or back to synchronous.
     * If a phase is set to true, it becomes active, and the base phase is deactivated.
     * If all asynchronous phases are set to false, the base phase is reactivated.
     * @param phase The phase to set.
     * @param set Whether to set the phase to active (true) or inactive (false).
     * @returns An array of currently active phases.
     */
    public setAsynchronousPhase(phase: Phase, set: boolean = true): Phase[] {
        return this.batch(() => {
            this._phases[phase] = set;

            const activeAsyncPhases = Object.entries(this._phases)
                .filter(([, value]) => value === true)
                .map(([key, _]) => key) as Phase[];

            if (activeAsyncPhases.length === 0) {
                this._phases[this._basePhase] = true;
                return [this._basePhase];
            } else {
                this._phases[this._basePhase] = false;
                return activeAsyncPhases;
            }
        });
    }

    /**
     * Unsets a phase (sets it to false), reactivating the base phase if no other phases are active.
     * This is a convenience method equivalent to `setAsynchronousPhase(phase, false)`.
     * @param phase The phase to unset.
     * @returns An array of currently active phases.
     */
    public unsetAsynchronousPhase(phase: Phase): Phase[] {
        return this.setAsynchronousPhase(phase, false);
    }

    /**
     * Checks if a specific phase is currently active.
     * @param phase The phase to check.
     * @returns True if the phase is active, false otherwise.
     */
    public hasPhase(phase: Phase): boolean {
        return this._phases[phase];
    }

    /**
     * Sets the base phase of the snapshot.
     * The base phase is the default or primary state that is active when no other asynchronous phases are active.
     * @param phase The new base phase.
     */
    public setBasePhase(phase: Phase): void {
        this._basePhase = phase;
    }

    /**
     * Activates the configured base phase and clears every other phase.
     * This does not change which phase is configured as the base.
     */
    public activateBasePhase(): Phase[] {
        this.batch(() => {
            this._phases[this._basePhase] = true;
            this.setAllElsePhases([this._basePhase], false);
        });
        return [this._basePhase];
    }

    /**
     * Sets a specific phase to active (true) or inactive (false).
     * If the phase is set to true, all other asynchronous phases are deactivated.
     * If the phase is set to false, the base phase is reactivated (if no other phases are active).
     * @param phase The phase to set.
     * @param set Whether to set the phase to active (true) or inactive (false).
     */
    public setPhase(phase: Phase, set: boolean = true): void {
        this.batch(() => {
            this._phases[phase] = set;

            if (set) {
                this.setAllElsePhases([phase], false);
            } else {
                this.setAsynchronousPhase(phase);
            }
        });
    }

    public getActivePhases(): Phase[] {
        return Object.entries(this._phases)
            .filter(([, value]) => value === true)
            .map(([key, _]) => key) as Phase[];
    }

    public getBasePhase(): Phase {
        return this._basePhase;
    }

    public getPhases(): { [key in Phase]: boolean } {
        return this._phases;
    }

    public getAttributeName(): string {
        return this.attributeName;
    }

    public getDataAttributes(): Record<string, string> {
        const active = this.getActivePhases()
        if (active.length === 0) return {}

        if (this.phaseMultiplicity === 'single') {
            if (active.length !== 1) {
                throw new Error(`${this.attributeName} requires exactly one active phase.`)
            }
            return { [this.attributeName]: active[0] }
        }

        return { [this.attributeName]: active.join(' ') }
    }

    /**
     * Creates a new snapshot of the current canvas model.
     * @returns A new CanvasModelSnapshot with the same canvas and interaction state.
     */
    public abstract clone(): StudioNodeSnapshot<Phase>;

    protected setAllElsePhases(exclusivePhases: Phase[], set: boolean = true): void {
        this.batch(() => {
            Object.entries(this._phases)
                .filter(([key, _]) => !exclusivePhases.includes(key as Phase))
                .forEach(([key, _]) => this._phases[key as Phase] = set);
        });
    }
}

export class SlotOccupancySnapshot extends StudioNodeSnapshot<SlotOccupancy> {
    private _slot: JSX.Element | null;

    constructor(slot: JSX.Element | null = null) {
        super({
            'empty': true,
            'occupied': false,
            'reserved': false,
            'blocked': false,
            'out-of-bounds': false
        }, 'empty', 'slotOccupancy');
        this._slot = slot;
    }

    public init(): void {
        this._slot;
    }

    public setNode(slot: JSX.Element | null): SlotOccupancySnapshot {
        this._slot = slot;
        return this;
    }

    public setOccupancyEmpty(state: boolean = true): SlotOccupancySnapshot {
        this.batch(() => {
            this._phases['empty'] = state;
            if (state) {
                this.setAllElsePhases(['empty'], false);
            }
        });
        return this;
    }

    public setOccupancyOccupied(state: boolean = true): SlotOccupancySnapshot {
        this.batch(() => {
            this._phases['occupied'] = state;
            if (state) {
                this.setAllElsePhases(['occupied'], false);
            }
        });
        return this;
    }

    public setOccupancyReserved(state: boolean = true): SlotOccupancySnapshot {
        this.batch(() => {
            this._phases['reserved'] = state;
            if (state) {
                this.setAllElsePhases(['reserved'], false);
            }
        });
        return this;
    }

    public setOccupancyBlocked(state: boolean = true): SlotOccupancySnapshot {
        this.batch(() => {
            this._phases['blocked'] = state;
            if (state) {
                this.setAllElsePhases(['blocked'], false);
            }
        });
        return this;
    }

    public setOccupancyOutOfBounds(state: boolean = true): SlotOccupancySnapshot {
        this.batch(() => {
            this._phases['out-of-bounds'] = state;
            if (state) {
                this.setAllElsePhases(['out-of-bounds'], false);
            }
        });
        return this;
    }

    public getOccupancyEmpty(): boolean {
        return this._phases['empty'];
    }

    public getOccupancyOccupied(): boolean {
        return this._phases['occupied'];
    }

    public getOccupancyReserved(): boolean {
        return this._phases['reserved'];
    }

    public getOccupancyBlocked(): boolean {
        return this._phases['blocked'];
    }

    public getOccupancyOutOfBounds(): boolean {
        return this._phases['out-of-bounds'];
    }

    /**
     * Creates a new snapshot of the slot occupancy.
     * @returns A new SlotOccupancySnapshot with the same slot and occupancy state.
     */
    public clone(): SlotOccupancySnapshot {
        const cloned = new SlotOccupancySnapshot(this._slot);
        Object.assign(cloned.getPhases(), this._phases);
        return cloned;
    }

    public getSlot(): JSX.Element | null {
        return this._slot;
    }
}

export class SlotDropStateSnapshot extends StudioNodeSnapshot<SlotDropState> {
    private _slot: JSX.Element | null;

    constructor(slot: JSX.Element | null = null) {
        super({
            'none': true,
            'valid-drop-target': false,
            'invalid-drop-target': false
        }, 'none', 'slotDropState');
        this._slot = slot;
    }

    public init(): void {
        this._slot;
    }

    public setNode(slot: JSX.Element | null): SlotDropStateSnapshot {
        this._slot = slot;
        return this;
    }

    public setDropStateNone(state: boolean = true): SlotDropStateSnapshot {
        this.batch(() => {
            this._phases['none'] = state;
            if (state) {
                this.setAllElsePhases(['none'], false);
            }
        });
        return this;
    }

    public setDropStateValidDropTarget(state: boolean = true): SlotDropStateSnapshot {
        this.batch(() => {
            this._phases['valid-drop-target'] = state;
            if (state) {
                this.setAllElsePhases(['valid-drop-target'], false);
            }
        });
        return this;
    }

    public setDropStateInvalidDropTarget(state: boolean = true): SlotDropStateSnapshot {
        this.batch(() => {
            this._phases['invalid-drop-target'] = state;
            if (state) {
                this.setAllElsePhases(['invalid-drop-target'], false);
            }
        });
        return this;
    }

    public getDropStateNone(): boolean {
        return this._phases['none'];
    }

    public getDropStateValidDropTarget(): boolean {
        return this._phases['valid-drop-target'];
    }

    public getDropStateInvalidDropTarget(): boolean {
        return this._phases['invalid-drop-target'];
    }

    /**
     * Creates a new snapshot of the slot drop state.
     * @returns A new SlotDropStateSnapshot with the same slot and drop state.
     */
    public clone(): SlotDropStateSnapshot {
        const cloned = new SlotDropStateSnapshot(this._slot);
        Object.assign(cloned.getPhases(), this._phases);
        return cloned;
    }

    public getSlot(): JSX.Element | null {
        return this._slot;
    }
}

export class NodeExecutionStateSnapshot extends StudioNodeSnapshot<NodeExecutionState> {
    private _node: JSX.Element | null;

    constructor(node: JSX.Element | null = null) {
        super({
            'idle': true,
            'running': false,
            'success': false,
            'warning': false,
            'error': false,
            'disabled': false,
            'unavailable': false
        }, 'idle', 'nodeExecutionState');
        this._node = node;
    }

    public init(): void {
        this._node;
    }

    public setNode(node: JSX.Element | null): NodeExecutionStateSnapshot {
        this._node = node;
        return this;
    }

    public setExecutionIdle(state: boolean = true): NodeExecutionStateSnapshot {
        this.batch(() => {
            this._phases['idle'] = state;
            if (state) this.setAllElsePhases(['idle'], false);
        });
        return this;
    }

    public setExecutionRunning(state: boolean = true): NodeExecutionStateSnapshot {
        this.batch(() => {
            this._phases['running'] = state;
            if (state) this.setAllElsePhases(['running'], false);
        });
        return this;
    }

    public setExecutionSuccess(state: boolean = true): NodeExecutionStateSnapshot {
        this.batch(() => {
            this._phases['success'] = state;
            if (state) this.setAllElsePhases(['success'], false);
        });
        return this;
    }

    public setExecutionWarning(state: boolean = true): NodeExecutionStateSnapshot {
        this.batch(() => {
            this._phases['warning'] = state;
            if (state) this.setAllElsePhases(['warning'], false);
        });
        return this;
    }

    public setExecutionError(state: boolean = true): NodeExecutionStateSnapshot {
        this.batch(() => {
            this._phases['error'] = state;
            if (state) this.setAllElsePhases(['error'], false);
        });
        return this;
    }

    public setExecutionDisabled(state: boolean = true): NodeExecutionStateSnapshot {
        this.batch(() => {
            this._phases['disabled'] = state;
            if (state) this.setAllElsePhases(['disabled'], false);
        });
        return this;
    }

    public setExecutionUnavailable(state: boolean = true): NodeExecutionStateSnapshot {
        this.batch(() => {
            this._phases['unavailable'] = state;
            if (state) this.setAllElsePhases(['unavailable'], false);
        });
        return this;
    }

    public getExecutionIdle(): boolean {
        return this._phases['idle'];
    }

    public getExecutionRunning(): boolean {
        return this._phases['running'];
    }

    public getExecutionSuccess(): boolean {
        return this._phases['success'];
    }

    public getExecutionWarning(): boolean {
        return this._phases['warning'];
    }

    public getExecutionError(): boolean {
        return this._phases['error'];
    }

    public getExecutionDisabled(): boolean {
        return this._phases['disabled'];
    }

    public getExecutionUnavailable(): boolean {
        return this._phases['unavailable'];
    }

    public clone(): NodeExecutionStateSnapshot {
        const cloned = new NodeExecutionStateSnapshot(this._node);
        Object.assign(cloned.getPhases(), this._phases);
        return cloned;
    }

    public getNode(): JSX.Element | null {
        return this._node;
    }
}

export class NodeConnectionStateSnapshot extends StudioNodeSnapshot<NodeConnectionState> {
    private _node: JSX.Element | null;

    constructor(node: JSX.Element | null = null) {
        super({
            'isolated': true,
            'connected': false,
            'missing-required-input': false,
            'blocked-by-upstream-error': false,
            'stale-output': false
        }, 'isolated', 'nodeConnectionState');
        this._node = node;
    }

    public init(): void {
        this._node;
    }

    public setNode(node: JSX.Element | null): NodeConnectionStateSnapshot {
        this._node = node;
        return this;
    }

    public setConnectionIsolated(state: boolean = true): NodeConnectionStateSnapshot {
        this.batch(() => {
            this._phases['isolated'] = state;
            if (state) this.setAllElsePhases(['isolated'], false);
        });
        return this;
    }

    public setConnectionConnected(state: boolean = true): NodeConnectionStateSnapshot {
        this.batch(() => {
            this._phases['connected'] = state;
            if (state) this.setAllElsePhases(['connected'], false);
        });
        return this;
    }

    public setConnectionMissingRequiredInput(state: boolean = true): NodeConnectionStateSnapshot {
        this.batch(() => {
            this._phases['missing-required-input'] = state;
            if (state) this.setAllElsePhases(['missing-required-input'], false);
        });
        return this;
    }

    public setConnectionBlockedByUpstreamError(state: boolean = true): NodeConnectionStateSnapshot {
        this.batch(() => {
            this._phases['blocked-by-upstream-error'] = state;
            if (state) this.setAllElsePhases(['blocked-by-upstream-error'], false);
        });
        return this;
    }

    public setConnectionStaleOutput(state: boolean = true): NodeConnectionStateSnapshot {
        this.batch(() => {
            this._phases['stale-output'] = state;
            if (state) this.setAllElsePhases(['stale-output'], false);
        });
        return this;
    }

    public getConnectionIsolated(): boolean {
        return this._phases['isolated'];
    }

    public getConnectionConnected(): boolean {
        return this._phases['connected'];
    }

    public getConnectionMissingRequiredInput(): boolean {
        return this._phases['missing-required-input'];
    }

    public getConnectionBlockedByUpstreamError(): boolean {
        return this._phases['blocked-by-upstream-error'];
    }

    public getConnectionStaleOutput(): boolean {
        return this._phases['stale-output'];
    }

    public clone(): NodeConnectionStateSnapshot {
        const cloned = new NodeConnectionStateSnapshot(this._node);
        Object.assign(cloned.getPhases(), this._phases);
        return cloned;
    }

    public getNode(): JSX.Element | null {
        return this._node;
    }
}

export class NodeOverlaySnapshot extends StudioNodeSnapshot<NodeOverlay> {
    private _node: JSX.Element | null;

    constructor(node: JSX.Element | null = null) {
        // TODO: Refactor overlay projection so mutually exclusive semantic groups
        // (for example, `valid`/`invalid`) cannot be emitted together in one
        // `data-node-overlay` token list. Overlays are currently composable.
        super({
            'selected': false,
            'hovered': false,
            'focused': false,
            'bookmarked': false,
            'dragging': false,
            'lifted-from-origin': false,
            'has-unsaved-changes': false,
            'unreachable': false,
            'valid': false,
            'invalid': false
        }, 'valid', 'nodeOverlay', 'multiple');
        this._node = node;
    }

    public init(): void {
        this._node;
    }

    public setNode(node: JSX.Element | null): NodeOverlaySnapshot {
        this._node = node;
        return this;
    }

    public setPhase(phase: NodeOverlay, set: boolean = true): void {
        this._phases[phase] = set;
        // this.emit('phase-changed', phase);
    }

    public setOverlaySelected(active: boolean = true): NodeOverlaySnapshot {
        this.setPhase('selected', active);
        return this;
    }

    public setOverlayHovered(active: boolean = true): NodeOverlaySnapshot {
        this.setPhase('hovered', active);
        return this;
    }

    public setOverlayFocused(active: boolean = true): NodeOverlaySnapshot {
        this.setPhase('focused', active);
        return this;
    }

    public setOverlayBookmarked(active: boolean = true): NodeOverlaySnapshot {
        this.setPhase('bookmarked', active);
        return this;
    }

    public setOverlayDragging(active: boolean = true): NodeOverlaySnapshot {
        this.setPhase('dragging', active);
        return this;
    }

    public setOverlayLiftedFromOrigin(active: boolean = true): NodeOverlaySnapshot {
        this.setPhase('lifted-from-origin', active);
        return this;
    }

    public setOverlayHasUnsavedChanges(active: boolean = true): NodeOverlaySnapshot {
        this.setPhase('has-unsaved-changes', active);
        return this;
    }

    public setOverlayUnreachable(active: boolean = true): NodeOverlaySnapshot {
        this.setPhase('unreachable', active);
        return this;
    }

    public setOverlayValid(active: boolean = true): NodeOverlaySnapshot {
        this.setPhase('valid', active);
        return this;
    }

    public setOverlayInvalid(active: boolean = true): NodeOverlaySnapshot {
        this.setPhase('invalid', active);
        return this;
    }

    public clearOverlays(): NodeOverlaySnapshot {
        this.batch(() => {
            for (const key of Object.keys(this._phases) as NodeOverlay[]) {
                this._phases[key] = false;
            }
        });
        return this;
    }

    public getOverlaySelected(): boolean {
        return this._phases['selected'];
    }

    public getOverlayHovered(): boolean {
        return this._phases['hovered'];
    }

    public getOverlayFocused(): boolean {
        return this._phases['focused'];
    }

    public getOverlayBookmarked(): boolean {
        return this._phases['bookmarked'];
    }

    public getOverlayDragging(): boolean {
        return this._phases['dragging'];
    }

    public getOverlayLiftedFromOrigin(): boolean {
        return this._phases['lifted-from-origin'];
    }

    public getOverlayHasUnsavedChanges(): boolean {
        return this._phases['has-unsaved-changes'];
    }

    public getOverlayUnreachable(): boolean {
        return this._phases['unreachable'];
    }

    public getOverlayValid(): boolean {
        return this._phases['valid'];
    }

    public getOverlayInvalid(): boolean {
        return this._phases['invalid'];
    }

    public clone(): NodeOverlaySnapshot {
        const cloned = new NodeOverlaySnapshot(this._node);
        Object.assign(cloned.getPhases(), this._phases);
        return cloned;
    }

    public getNode(): JSX.Element | null {
        return this._node;
    }
}

export class PathLifecycleStateSnapshot extends StudioNodeSnapshot<PathLifecycleState> {
    public constructor() {
        super({
            'unconnected': false,
            'drafting': false,
            'connected': true,
            'pending-validation': false,
            'invalid': false,
            'blocked': false,
        }, 'connected', 'pathLifecycleState');
    }

    public init(): void { }

    public setPhase(phase: PathLifecycleState, active: boolean = true): void {
        this.batch(() => {
            if (!active) {
                this._phases[phase] = false;
                if (this.getActivePhases().length === 0) this._phases[this._basePhase] = true;
                return;
            }

            for (const candidate of Object.keys(this._phases) as PathLifecycleState[]) {
                this._phases[candidate] = candidate === phase;
            }
        });
    }

    public setLifecycleUnconnected(active: boolean = true): PathLifecycleStateSnapshot {
        this.setPhase('unconnected', active);
        return this;
    }

    public setLifecycleDrafting(active: boolean = true): PathLifecycleStateSnapshot {
        this.setPhase('drafting', active);
        return this;
    }

    public setLifecycleConnected(active: boolean = true): PathLifecycleStateSnapshot {
        this.setPhase('connected', active);
        return this;
    }

    public setLifecyclePendingValidation(active: boolean = true): PathLifecycleStateSnapshot {
        this.setPhase('pending-validation', active);
        return this;
    }

    public setLifecycleInvalid(active: boolean = true): PathLifecycleStateSnapshot {
        this.setPhase('invalid', active);
        return this;
    }

    public setLifecycleBlocked(active: boolean = true): PathLifecycleStateSnapshot {
        this.setPhase('blocked', active);
        return this;
    }

    public getLifecycleUnconnected(): boolean {
        return this._phases['unconnected'];
    }

    public getLifecycleDrafting(): boolean {
        return this._phases['drafting'];
    }

    public getLifecycleConnected(): boolean {
        return this._phases['connected'];
    }

    public getLifecyclePendingValidation(): boolean {
        return this._phases['pending-validation'];
    }

    public getLifecycleInvalid(): boolean {
        return this._phases['invalid'];
    }

    public getLifecycleBlocked(): boolean {
        return this._phases['blocked'];
    }

    public clone(): PathLifecycleStateSnapshot {
        const cloned = new PathLifecycleStateSnapshot();
        Object.assign(cloned.getPhases(), this._phases);
        return cloned;
    }
}

export class PathFlowStateSnapshot extends StudioNodeSnapshot<PathFlowState> {
    public constructor() {
        super({
            'idle': true,
            'active': false,
            'success': false,
            'warning': false,
            'error': false,
            'muted': false,
        }, 'idle', 'pathFlowState');
    }

    public init(): void { }

    public setPhase(phase: PathFlowState, active: boolean = true): void {
        this.batch(() => {
            if (!active) {
                this._phases[phase] = false;
                if (this.getActivePhases().length === 0) this._phases[this._basePhase] = true;
                return;
            }

            for (const candidate of Object.keys(this._phases) as PathFlowState[]) {
                this._phases[candidate] = candidate === phase;
            }
        });
    }

    public setFlowIdle(active: boolean = true): PathFlowStateSnapshot {
        this.setPhase('idle', active);
        return this;
    }

    public setFlowActive(active: boolean = true): PathFlowStateSnapshot {
        this.setPhase('active', active);
        return this;
    }

    public setFlowSuccess(active: boolean = true): PathFlowStateSnapshot {
        this.setPhase('success', active);
        return this;
    }

    public setFlowWarning(active: boolean = true): PathFlowStateSnapshot {
        this.setPhase('warning', active);
        return this;
    }

    public setFlowError(active: boolean = true): PathFlowStateSnapshot {
        this.setPhase('error', active);
        return this;
    }

    public setFlowMuted(active: boolean = true): PathFlowStateSnapshot {
        this.setPhase('muted', active);
        return this;
    }

    public getFlowIdle(): boolean {
        return this._phases['idle'];
    }

    public getFlowActive(): boolean {
        return this._phases['active'];
    }

    public getFlowSuccess(): boolean {
        return this._phases['success'];
    }

    public getFlowWarning(): boolean {
        return this._phases['warning'];
    }

    public getFlowError(): boolean {
        return this._phases['error'];
    }

    public getFlowMuted(): boolean {
        return this._phases['muted'];
    }

    public clone(): PathFlowStateSnapshot {
        const cloned = new PathFlowStateSnapshot();
        Object.assign(cloned.getPhases(), this._phases);
        return cloned;
    }
}

/**
 * Represents one workflow edge and its independently tracked lifecycle and flow state.
 */
export class PathModelSnapshot {
    private readonly _edge: WorkflowEdge;
    private readonly _lifecycle: PathLifecycleStateSnapshot;
    private readonly _flow: PathFlowStateSnapshot;

    public constructor(edge: WorkflowEdge) {
        this._edge = edge;
        this._lifecycle = new PathLifecycleStateSnapshot();
        this._flow = new PathFlowStateSnapshot();
    }

    public init(): void {
        this._lifecycle.init();
        this._flow.init();
    }

    public getPathId(): string {
        return `${this._edge.source}->${this._edge.target}`;
    }

    public getEdge(): WorkflowEdge {
        return this._edge;
    }

    public get lifecycle(): PathLifecycleStateSnapshot {
        return this._lifecycle;
    }

    public get flow(): PathFlowStateSnapshot {
        return this._flow;
    }

    public getDataAttributes(): Record<string, string> {
        return {
            ...this._lifecycle.getDataAttributes(),
            ...this._flow.getDataAttributes(),
        };
    }

    /* -------------------------------------------------------------
     *  Path Lifecycle State Delegation
     * ------------------------------------------------------------- */
    public setLifecycleUnconnected(active: boolean = true): this {
        this._lifecycle.setLifecycleUnconnected(active);
        return this;
    }

    public setLifecycleDrafting(active: boolean = true): this {
        this._lifecycle.setLifecycleDrafting(active);
        return this;
    }

    public setLifecycleConnected(active: boolean = true): this {
        this._lifecycle.setLifecycleConnected(active);
        return this;
    }

    public setLifecyclePendingValidation(active: boolean = true): this {
        this._lifecycle.setLifecyclePendingValidation(active);
        return this;
    }

    public setLifecycleInvalid(active: boolean = true): this {
        this._lifecycle.setLifecycleInvalid(active);
        return this;
    }

    public setLifecycleBlocked(active: boolean = true): this {
        this._lifecycle.setLifecycleBlocked(active);
        return this;
    }

    public getLifecycleUnconnected(): boolean {
        return this._lifecycle.getLifecycleUnconnected();
    }

    public getLifecycleDrafting(): boolean {
        return this._lifecycle.getLifecycleDrafting();
    }

    public getLifecycleConnected(): boolean {
        return this._lifecycle.getLifecycleConnected();
    }

    public getLifecyclePendingValidation(): boolean {
        return this._lifecycle.getLifecyclePendingValidation();
    }

    public getLifecycleInvalid(): boolean {
        return this._lifecycle.getLifecycleInvalid();
    }

    public getLifecycleBlocked(): boolean {
        return this._lifecycle.getLifecycleBlocked();
    }

    /* -------------------------------------------------------------
     *  Path Flow State Delegation
     * ------------------------------------------------------------- */
    public setFlowIdle(active: boolean = true): this {
        this._flow.setFlowIdle(active);
        return this;
    }

    public setFlowActive(active: boolean = true): this {
        this._flow.setFlowActive(active);
        return this;
    }

    public setFlowSuccess(active: boolean = true): this {
        this._flow.setFlowSuccess(active);
        return this;
    }

    public setFlowWarning(active: boolean = true): this {
        this._flow.setFlowWarning(active);
        return this;
    }

    public setFlowError(active: boolean = true): this {
        this._flow.setFlowError(active);
        return this;
    }

    public setFlowMuted(active: boolean = true): this {
        this._flow.setFlowMuted(active);
        return this;
    }

    public getFlowIdle(): boolean {
        return this._flow.getFlowIdle();
    }

    public getFlowActive(): boolean {
        return this._flow.getFlowActive();
    }

    public getFlowSuccess(): boolean {
        return this._flow.getFlowSuccess();
    }

    public getFlowWarning(): boolean {
        return this._flow.getFlowWarning();
    }

    public getFlowError(): boolean {
        return this._flow.getFlowError();
    }

    public getFlowMuted(): boolean {
        return this._flow.getFlowMuted();
    }


    public clone(): PathModelSnapshot {
        const cloned = new PathModelSnapshot({ ...this._edge });
        Object.assign(cloned.lifecycle.getPhases(), this._lifecycle.getPhases());
        Object.assign(cloned.flow.getPhases(), this._flow.getPhases());
        return cloned;
    }
}

/**
 * Model snapshot class representing a Node in the workflow studio canvas.
 * Manages the node element, its identifier, and all node sub-snapshots:
 * NodeExecutionState, NodeConnectionState, and NodeOverlay.
 */
export abstract class NodeModelSnapshot {
    private readonly _element: JSX.Element | null;
    protected readonly data: WorkflowNode;
    private _capabilities: WorkflowNodeCapabilities;

    // Sub-snapshots
    private readonly _execution: NodeExecutionStateSnapshot;
    private readonly _connection: NodeConnectionStateSnapshot;
    private readonly _overlay: NodeOverlaySnapshot;

    protected constructor(node: WorkflowNode) {
        this.data = node;
        this._capabilities = { ...node.capabilities };
        this._element = this.snapshot(
            this._execution = new NodeExecutionStateSnapshot(null),
            this._connection = new NodeConnectionStateSnapshot(null),
            this._overlay = new NodeOverlaySnapshot(null)
        );

        this._execution.setNode(this._element);
        this._connection.setNode(this._element);
        this._overlay.setNode(this._element);
    }

    public init(): void {
        const nodeValidationResult = validateWorkflowNodeDataPolicy(this.data)
        if (!nodeValidationResult.valid) {
            console.error(`WorkflowNode data policy violation for node ${this.data.id}:`, nodeValidationResult.errors.join('\n'))
            this.setOverlayInvalid(true)
            this.setOverlayUnreachable(true)
            this._execution.setExecutionIdle(true)
            this._connection.setConnectionBlockedByUpstreamError(true)
            this._overlay.init();
            return;
        }

        this._element;
        this._execution.init();
        this._connection.init();
        this._overlay.init();
    }

    protected abstract snapshot(execution: NodeExecutionStateSnapshot, connection: NodeConnectionStateSnapshot, overlay: NodeOverlaySnapshot): JSX.Element | null;

    /* -------------------------------------------------------------
     *  Sub-snapshot Accessors
     * ------------------------------------------------------------- */
    public get execution(): NodeExecutionStateSnapshot {
        return this._execution;
    }

    public get connection(): NodeConnectionStateSnapshot {
        return this._connection;
    }

    public get overlay(): NodeOverlaySnapshot {
        return this._overlay;
    }

    public getDataAttributes(): Record<string, string> {
        return {
            ...this._execution.getDataAttributes(),
            ...this._connection.getDataAttributes(),
            ...this._overlay.getDataAttributes(),
            'data-accepts-input': String(this._capabilities.acceptsInput),
            'data-accepts-output': String(this._capabilities.acceptsOutput),
        };
    }

    /**
     * Updates the renderer-facing capabilities without mutating the workflow
     * node data supplied by the tracker.
     */
    public setCapabilities(capabilities: WorkflowNodeCapabilities): this {
        this._capabilities = { ...capabilities };
        return this;
    }

    public getCapabilities(): Readonly<WorkflowNodeCapabilities> {
        return this._capabilities;
    }

    /* -------------------------------------------------------------
     *  Node Reference & ID Management
     * ------------------------------------------------------------- */
    public getNode(): JSX.Element | null {
        return this._element;
    }

    public getNodeId(): string | null {
        return this.data?.id || null;
    }

    public setNodeId(nodeId: string): this {
        this.data.id = nodeId;
        return this;
    }

    public getNodeData(): WorkflowNode {
        return this.data;
    }

    /* -------------------------------------------------------------
     *  Node Execution State Delegation
     * ------------------------------------------------------------- */
    public setExecutionIdle(state: boolean = true): this {
        this._execution.setExecutionIdle(state);
        return this;
    }

    public setExecutionRunning(state: boolean = true): this {
        this._execution.setExecutionRunning(state);
        return this;
    }

    public setExecutionSuccess(state: boolean = true): this {
        this._execution.setExecutionSuccess(state);
        return this;
    }

    public setExecutionWarning(state: boolean = true): this {
        this._execution.setExecutionWarning(state);
        return this;
    }

    public setExecutionError(state: boolean = true): this {
        this._execution.setExecutionError(state);
        return this;
    }

    public setExecutionDisabled(state: boolean = true): this {
        this._execution.setExecutionDisabled(state);
        return this;
    }

    public setExecutionUnavailable(state: boolean = true): this {
        this._execution.setExecutionUnavailable(state);
        return this;
    }

    public getExecutionIdle(): boolean {
        return this._execution.getExecutionIdle();
    }

    public getExecutionRunning(): boolean {
        return this._execution.getExecutionRunning();
    }

    public getExecutionSuccess(): boolean {
        return this._execution.getExecutionSuccess();
    }

    public getExecutionWarning(): boolean {
        return this._execution.getExecutionWarning();
    }

    public getExecutionError(): boolean {
        return this._execution.getExecutionError();
    }

    public getExecutionDisabled(): boolean {
        return this._execution.getExecutionDisabled();
    }

    public getExecutionUnavailable(): boolean {
        return this._execution.getExecutionUnavailable();
    }

    public getActiveExecutionState(): NodeExecutionState[] {
        return this._execution.getActivePhases();
    }

    /* -------------------------------------------------------------
     *  Node Connection State Delegation
     * ------------------------------------------------------------- */
    public setConnectionIsolated(state: boolean = true): this {
        this._connection.setConnectionIsolated(state);
        return this;
    }

    public setConnectionConnected(state: boolean = true): this {
        this._connection.setConnectionConnected(state);
        return this;
    }

    public setConnectionMissingRequiredInput(state: boolean = true): this {
        this._connection.setConnectionMissingRequiredInput(state);
        return this;
    }

    public setConnectionBlockedByUpstreamError(state: boolean = true): this {
        this._connection.setConnectionBlockedByUpstreamError(state);
        return this;
    }

    public setConnectionStaleOutput(state: boolean = true): this {
        this._connection.setConnectionStaleOutput(state);
        return this;
    }

    public getConnectionIsolated(): boolean {
        return this._connection.getConnectionIsolated();
    }

    public getConnectionConnected(): boolean {
        return this._connection.getConnectionConnected();
    }

    public getConnectionMissingRequiredInput(): boolean {
        return this._connection.getConnectionMissingRequiredInput();
    }

    public getConnectionBlockedByUpstreamError(): boolean {
        return this._connection.getConnectionBlockedByUpstreamError();
    }

    public getConnectionStaleOutput(): boolean {
        return this._connection.getConnectionStaleOutput();
    }

    public getActiveConnectionState(): NodeConnectionState[] {
        return this._connection.getActivePhases();
    }

    /* -------------------------------------------------------------
     *  Node Overlay Delegation
     * ------------------------------------------------------------- */
    public setOverlaySelected(active: boolean = true): this {
        this._overlay.setOverlaySelected(active);
        return this;
    }

    public setOverlayHovered(active: boolean = true): this {
        this._overlay.setOverlayHovered(active);
        return this;
    }

    public setOverlayFocused(active: boolean = true): this {
        this._overlay.setOverlayFocused(active);
        return this;
    }

    public setOverlayBookmarked(active: boolean = true): this {
        this._overlay.setOverlayBookmarked(active);
        return this;
    }

    public setOverlayDragging(active: boolean = true): this {
        this._overlay.setOverlayDragging(active);
        return this;
    }

    public setOverlayLiftedFromOrigin(active: boolean = true): this {
        this._overlay.setOverlayLiftedFromOrigin(active);
        return this;
    }

    public setOverlayHasUnsavedChanges(active: boolean = true): this {
        this._overlay.setOverlayHasUnsavedChanges(active);
        return this;
    }

    public setOverlayUnreachable(active: boolean = true): this {
        this._overlay.setOverlayUnreachable(active);
        return this;
    }

    public setOverlayValid(active: boolean = true): this {
        this._overlay.setOverlayValid(active);
        return this;
    }

    public setOverlayInvalid(active: boolean = true): this {
        this._overlay.setOverlayInvalid(active);
        return this;
    }

    public clearOverlays(): this {
        this._overlay.clearOverlays();
        return this;
    }

    public getOverlaySelected(): boolean {
        return this._overlay.getOverlaySelected();
    }

    public getOverlayHovered(): boolean {
        return this._overlay.getOverlayHovered();
    }

    public getOverlayFocused(): boolean {
        return this._overlay.getOverlayFocused();
    }

    public getOverlayBookmarked(): boolean {
        return this._overlay.getOverlayBookmarked();
    }

    public getOverlayDragging(): boolean {
        return this._overlay.getOverlayDragging();
    }

    public getOverlayLiftedFromOrigin(): boolean {
        return this._overlay.getOverlayLiftedFromOrigin();
    }

    public getOverlayHasUnsavedChanges(): boolean {
        return this._overlay.getOverlayHasUnsavedChanges();
    }

    public getOverlayUnreachable(): boolean {
        return this._overlay.getOverlayUnreachable();
    }

    public getOverlayValid(): boolean {
        return this._overlay.getOverlayValid();
    }

    public getOverlayInvalid(): boolean {
        return this._overlay.getOverlayInvalid();
    }

    public getActiveOverlays(): NodeOverlay[] {
        return this._overlay.getActivePhases();
    }

    /* -------------------------------------------------------------
     *  Cloning
     * ------------------------------------------------------------- */
    public clone(): this {
        const SubclassConstructor = this.constructor as new (nodeId?: string | null) => this;
        const snapshot = new SubclassConstructor(this.getNodeId());
        Object.assign(snapshot.execution.getPhases(), this._execution.getPhases());
        Object.assign(snapshot.connection.getPhases(), this._connection.getPhases());
        Object.assign(snapshot.overlay.getPhases(), this._overlay.getPhases());
        return snapshot;
    }
}

/**
 * Merged snapshot class for a Slot in the workflow canvas.
 * Merges the 2 slot snapshots (SlotOccupancy and SlotDropState) and references
 * the placed NodeModelSnapshot when occupied.
 */
export abstract class SlotModelSnapshot {
    private _slot: JSX.Element | null;
    private _nodeModel: NodeModelSnapshot | null;

    // Sub-snapshots
    private readonly _occupancy: SlotOccupancySnapshot;
    private readonly _dropState: SlotDropStateSnapshot;

    protected constructor(nodeModel: NodeModelSnapshot | null = null) {
        this._nodeModel = nodeModel;

        this._slot = this.snapshot(
            nodeModel,
            this._occupancy = new SlotOccupancySnapshot(null),
            this._dropState = new SlotDropStateSnapshot(null),
        );

        this._occupancy.setNode(this._slot);
        this._dropState.setNode(this._slot);

        if (nodeModel !== null) {
            this._occupancy.setOccupancyOccupied(true);
        }
    }

    public init(): void {
        this._slot;
        this._occupancy.init();
        this._dropState.init();
        this._nodeModel?.init();
    }

    protected abstract snapshot(nodeModel: NodeModelSnapshot | null, occupancy: SlotOccupancySnapshot, dropState: SlotDropStateSnapshot): JSX.Element | null;

    /* -------------------------------------------------------------
     *  Sub-snapshot Accessors
     * ------------------------------------------------------------- */
    public get occupancy(): SlotOccupancySnapshot {
        return this._occupancy;
    }

    public get dropState(): SlotDropStateSnapshot {
        return this._dropState;
    }

    public getDataAttributes(): Record<string, string> {
        return {
            ...this._occupancy.getDataAttributes(),
            ...this._dropState.getDataAttributes(),
        };
    }

    public get nodeModel(): NodeModelSnapshot | null {
        return this._nodeModel;
    }

    /* -------------------------------------------------------------
     *  Slot & Node Reference Management
     * ------------------------------------------------------------- */
    public getSlot(): JSX.Element | null {
        return this._slot;
    }

    public getNodeModel(): NodeModelSnapshot | null {
        return this._nodeModel;
    }

    public hasNode(): boolean {
        return this._nodeModel !== null || this._occupancy.getOccupancyOccupied();
    }

    public setNodeModel(nodeModel: NodeModelSnapshot | null): this {
        this._nodeModel = nodeModel;
        this._slot = this.snapshot(nodeModel, this._occupancy, this._dropState);
        if (nodeModel !== null) {
            this._occupancy.setOccupancyOccupied(true);
        } else {
            this._occupancy.setOccupancyEmpty(true);
        }
        return this;
    }

    public removeNode(): this {
        this._nodeModel = null;
        this._occupancy.setOccupancyEmpty(true);
        return this;
    }

    /* -------------------------------------------------------------
     *  Slot Occupancy Delegation
     * ------------------------------------------------------------- */
    public setOccupancyEmpty(state: boolean = true): this {
        this._occupancy.setOccupancyEmpty(state);
        return this;
    }

    public setOccupancyOccupied(state: boolean = true): this {
        this._occupancy.setOccupancyOccupied(state);
        return this;
    }

    public setOccupancyReserved(state: boolean = true): this {
        this._occupancy.setOccupancyReserved(state);
        return this;
    }

    public setOccupancyBlocked(state: boolean = true): this {
        this._occupancy.setOccupancyBlocked(state);
        return this;
    }

    public setOccupancyOutOfBounds(state: boolean = true): this {
        this._occupancy.setOccupancyOutOfBounds(state);
        return this;
    }

    public getOccupancyEmpty(): boolean {
        return this._occupancy.getOccupancyEmpty();
    }

    public getOccupancyOccupied(): boolean {
        return this._occupancy.getOccupancyOccupied();
    }

    public getOccupancyReserved(): boolean {
        return this._occupancy.getOccupancyReserved();
    }

    public getOccupancyBlocked(): boolean {
        return this._occupancy.getOccupancyBlocked();
    }

    public getOccupancyOutOfBounds(): boolean {
        return this._occupancy.getOccupancyOutOfBounds();
    }

    public getActiveOccupancy(): SlotOccupancy[] {
        return this._occupancy.getActivePhases();
    }

    /* -------------------------------------------------------------
     *  Slot Drop State Delegation
     * ------------------------------------------------------------- */
    public setDropStateNone(state: boolean = true): this {
        this._dropState.setDropStateNone(state);
        return this;
    }

    public setDropStateValidDropTarget(state: boolean = true): this {
        this._dropState.setDropStateValidDropTarget(state);
        return this;
    }

    public setDropStateInvalidDropTarget(state: boolean = true): this {
        this._dropState.setDropStateInvalidDropTarget(state);
        return this;
    }

    public getDropStateNone(): boolean {
        return this._dropState.getDropStateNone();
    }

    public getDropStateValidDropTarget(): boolean {
        return this._dropState.getDropStateValidDropTarget();
    }

    public getDropStateInvalidDropTarget(): boolean {
        return this._dropState.getDropStateInvalidDropTarget();
    }

    public getActiveDropState(): SlotDropState[] {
        return this._dropState.getActivePhases();
    }

    /* -------------------------------------------------------------
     *  Cloning
     * ------------------------------------------------------------- */
    public clone(): this {
        const SubclassConstructor = this.constructor as new (nodeModel?: NodeModelSnapshot | null) => this;
        const snapshot = new SubclassConstructor(this._nodeModel ? this._nodeModel.clone() : null);
        Object.assign(snapshot._occupancy.getPhases(), this._occupancy.getPhases());
        Object.assign(snapshot._dropState.getPhases(), this._dropState.getPhases());
        return snapshot;
    }
}

export abstract class CanvasModelSnapshot extends StudioNodeSnapshot<CanvasInteraction> {
    private readonly _canvas: JSX.Element | null;
    private readonly _slotModels: SlotModelSnapshot[];

    protected constructor(slotModels: SlotModelSnapshot[] = []) {
        super({
            'idle': true, // integratable
            'panning': false, // integratable
            'zooming': false, // integratable
            'selecting': false, // later
            'dragging-node': false,
            'drawing-path': false,
            'reconnecting-path': false,
            'layout-revealing': false // integratable
        }, 'idle', 'canvasInteraction');
        this._slotModels = slotModels;
        this._canvas = this.snapshot();
    }

    public init(): void {
        this._canvas
        this._slotModels.forEach((slotModel) => slotModel.init());
    }

    protected abstract snapshot(): JSX.Element | null;

    public setInteractionIdle(state: boolean = true): CanvasModelSnapshot {
        this.batch(() => {
            this._phases['idle'] = state;
            if (state) {
                this.setAllElsePhases(['idle'], false);
            }
        });
        return this;
    }

    public setInteractionPanning(state: boolean = true): CanvasModelSnapshot {
        this.batch(() => {
            this._phases['panning'] = state;
            if (state) {
                this.setAllElsePhases(['panning']);
            }
        });
        return this;
    }

    public setInteractionZooming(state: boolean = true): CanvasModelSnapshot {
        this.batch(() => {
            this._phases['zooming'] = state;
            if (state) {
                this.setAllElsePhases(['zooming']);
            }
        });
        return this;
    }

    public setInteractionSelecting(state: boolean = true): CanvasModelSnapshot {
        this.batch(() => {
            this._phases['selecting'] = state;
            if (state) {
                this.setAllElsePhases(['selecting']);
            }
        });
        return this;
    }

    public setInteractionDraggingNode(state: boolean = true): CanvasModelSnapshot {
        this.batch(() => {
            this._phases['dragging-node'] = state;
            if (state) {
                this.setAllElsePhases(['dragging-node']);
            }
        });
        return this;
    }

    public setInteractionDrawingPath(state: boolean = true): CanvasModelSnapshot {
        this.batch(() => {
            this._phases['drawing-path'] = state;
            if (state) {
                this.setAllElsePhases(['drawing-path']);
            }
        });
        return this;
    }

    public setInteractionReconnectingPath(state: boolean = true): CanvasModelSnapshot {
        this.batch(() => {
            this._phases['reconnecting-path'] = state;
            if (state) {
                this.setAllElsePhases(['reconnecting-path']);
            }
        });
        return this;
    }

    public setInteractionLayoutRevealing(state: boolean = true): CanvasModelSnapshot {
        this.batch(() => {
            this._phases['layout-revealing'] = state;
            if (state) {
                this.setAllElsePhases(['layout-revealing']);
            }
        });
        return this;
    }

    public getInteractionIdle(): boolean {
        return this._phases['idle'];
    }

    public getInteractionPanning(): boolean {
        return this._phases['panning'];
    }

    public getInteractionZooming(): boolean {
        return this._phases['zooming'];
    }

    public getInteractionSelecting(): boolean {
        return this._phases['selecting'];
    }

    public getInteractionDraggingNode(): boolean {
        return this._phases['dragging-node'];
    }

    public getInteractionDrawingPath(): boolean {
        return this._phases['drawing-path'];
    }

    public getInteractionReconnectingPath(): boolean {
        return this._phases['reconnecting-path'];
    }

    public getInteractionLayoutRevealing(): boolean {
        return this._phases['layout-revealing'];
    }

    /**
     * Creates a new snapshot of the current canvas model.
     * @returns A new CanvasModelSnapshot with the same canvas and interaction state.
     */
    public clone(): CanvasModelSnapshot {
        const SubclassConstructor = this.constructor as new (canvas?: JSX.Element | null, slotModels?: SlotModelSnapshot[] | null) => this;
        return new SubclassConstructor(this._canvas, this._slotModels.map(slot => slot.clone()));
    }

    public getCanvas(): JSX.Element | null {
        return this._canvas;
    }

    public getSlotModels(): SlotModelSnapshot[] {
        return this._slotModels;
    }
}

export class WorkflowStateSnapshot extends StudioNodeSnapshot<WorkflowState> {

    constructor() {
        super({
            'valid': true,
            'incomplete': false,
            'disconnected': false,
            'unreachable': false,
            'cyclic': false,
            'blocked': false,
            'executing': false,
            'completed': false,
            'failed': false
        }, 'valid', 'workflowState');
    }

    public init(): void { }

    public setStateValid(state: boolean = true): WorkflowStateSnapshot {
        this.batch(() => {
            this._phases['valid'] = state;
            if (state) {
                this.setAllElsePhases(['valid'], false);
            }
        });
        return this;
    }

    public setStateIncomplete(state: boolean = true): WorkflowStateSnapshot {
        this.batch(() => {
            this._phases['incomplete'] = state;
            if (state) {
                this.setAllElsePhases(['incomplete'], false);
            }
        });
        return this;
    }

    public setStateDisconnected(state: boolean = true): WorkflowStateSnapshot {
        this.batch(() => {
            this._phases['disconnected'] = state;
            if (state) {
                this.setAllElsePhases(['disconnected'], false);
            }
        });
        return this;
    }

    public setStateUnreachable(state: boolean = true): WorkflowStateSnapshot {
        this.batch(() => {
            this._phases['unreachable'] = state;
            if (state) {
                this.setAllElsePhases(['unreachable'], false);
            }
        });
        return this;
    }

    public setStateCyclic(state: boolean = true): WorkflowStateSnapshot {
        this.batch(() => {
            this._phases['cyclic'] = state;
            if (state) {
                this.setAllElsePhases(['cyclic'], false);
            }
        });
        return this;
    }

    public setStateBlocked(state: boolean = true): WorkflowStateSnapshot {
        this.batch(() => {
            this._phases['blocked'] = state;
            if (state) {
                this.setAllElsePhases(['blocked'], false);
            }
        });
        return this;
    }

    public setStateExecuting(state: boolean = true): WorkflowStateSnapshot {
        this.batch(() => {
            this._phases['executing'] = state;
            if (state) {
                this.setAllElsePhases(['executing'], false);
            }
        });
        return this;
    }

    public setStateCompleted(state: boolean = true): WorkflowStateSnapshot {
        this.batch(() => {
            this._phases['completed'] = state;
            if (state) {
                this.setAllElsePhases(['completed'], false);
            }
        });
        return this;
    }

    public setStateFailed(state: boolean = true): WorkflowStateSnapshot {
        this.batch(() => {
            this._phases['failed'] = state;
            if (state) {
                this.setAllElsePhases(['failed'], false);
            }
        });
        return this;
    }

    public getStateValid(): boolean {
        return this._phases['valid'];
    }

    public getStateIncomplete(): boolean {
        return this._phases['incomplete'];
    }

    public getStateDisconnected(): boolean {
        return this._phases['disconnected'];
    }

    public getStateUnreachable(): boolean {
        return this._phases['unreachable'];
    }

    public getStateCyclic(): boolean {
        return this._phases['cyclic'];
    }

    public getStateBlocked(): boolean {
        return this._phases['blocked'];
    }

    public getStateExecuting(): boolean {
        return this._phases['executing'];
    }

    public getStateCompleted(): boolean {
        return this._phases['completed'];
    }

    public getStateFailed(): boolean {
        return this._phases['failed'];
    }


    /**
     * Creates a new snapshot of the current workflow state.
     * @returns A new WorkflowStateSnapshot with the current workflow state.
     */
    public clone(): WorkflowStateSnapshot {
        return new WorkflowStateSnapshot();
    }
}

export interface WorkflowModelSnapshotOptions {
    workflowState: WorkflowStateSnapshot;
    canvasModel: CanvasModelSnapshot;
    nodeModels: ReadonlyMap<string, NodeModelSnapshot>;
    pathModels: ReadonlyMap<string, PathModelSnapshot>;
    slotModels: ReadonlyMap<string, SlotModelSnapshot>;
}

/**
 * Coordinates stable model identities and behavior that spans multiple models.
 * It intentionally does not own scene data or rendering subscriptions.
 * Its mutation boundary currently supports node overlays only while the canvas
 * scene tracker remains in use for all other behavior.
 */
export class WorkflowModelSnapshot {
    private readonly _workflowState: WorkflowStateSnapshot;
    private readonly _canvasModel: CanvasModelSnapshot;
    private readonly _nodeModels: Map<string, NodeModelSnapshot>;
    private readonly _pathModels: Map<string, PathModelSnapshot>;
    private readonly _slotModels: Map<string, SlotModelSnapshot>;

    public constructor(options: WorkflowModelSnapshotOptions) {
        this._workflowState = options.workflowState;
        this._canvasModel = options.canvasModel;
        this._nodeModels = new Map(options.nodeModels);
        this._pathModels = new Map(options.pathModels);
        this._slotModels = new Map(options.slotModels);
    }

    public init(): void {
    }

    public getWorkflowState(): WorkflowStateSnapshot {
        return this._workflowState;
    }

    public getCanvasModel(): CanvasModelSnapshot {
        return this._canvasModel;
    }

    public getNodeModel(nodeId: string): NodeModelSnapshot | undefined {
        return this._nodeModels.get(nodeId);
    }

    public getPathModel(pathId: string): PathModelSnapshot | undefined {
        return this._pathModels.get(pathId);
    }

    public getSlotModel(slotId: string): SlotModelSnapshot | undefined {
        return this._slotModels.get(slotId);
    }

    /**
     * Updates renderer-facing node capabilities, which are node data rather
     * than a phase-state mutation.
     */
    public setNodeCapabilities(nodeId: string, capabilities: WorkflowNodeCapabilities): void {
        const target = this._nodeModels.get(nodeId);
        if (!target) throw new Error(`Unknown workflow node model: ${nodeId}`);
        target.setCapabilities(capabilities);
    }

    /**
     * Observes phase changes from every registered model while retaining the
     * target identity that the local snapshot listener does not know.
     */
    public subscribe(listener: WorkflowStudioSceneChangeListener): () => void {
        const unsubscribe = [
            this.subscribeToSnapshot(this._canvasModel, { kind: 'canvas' }, 'canvasInteraction', listener),
            this.subscribeToSnapshot(this._workflowState, { kind: 'workflow' }, 'workflowState', listener),
        ];

        for (const [nodeId, node] of this._nodeModels) {
            const target: WorkflowStudioNodeTargetPayload = { kind: 'node', nodeId };
            unsubscribe.push(
                this.subscribeToSnapshot(node.execution, target, 'nodeExecutionState', listener),
                this.subscribeToSnapshot(node.connection, target, 'nodeConnectionState', listener),
                this.subscribeToSnapshot(node.overlay, target, 'nodeOverlay', listener),
            );
        }

        for (const [slotId, slot] of this._slotModels) {
            const target: WorkflowStudioSlotTargetPayload = { kind: 'slot', slotId };
            unsubscribe.push(
                this.subscribeToSnapshot(slot.occupancy, target, 'slotOccupancy', listener),
                this.subscribeToSnapshot(slot.dropState, target, 'slotDropState', listener),
            );
        }

        for (const [pathId, path] of this._pathModels) {
            const target: WorkflowStudioPathTargetPayload = { kind: 'path', pathId };
            unsubscribe.push(
                this.subscribeToSnapshot(path.lifecycle, target, 'pathLifecycleState', listener),
                this.subscribeToSnapshot(path.flow, target, 'pathFlowState', listener),
            );
        }

        return () => unsubscribe.forEach((dispose) => dispose());
    }

    private subscribeToSnapshot<Phase extends StateCategory>(
        snapshot: StudioNodeSnapshot<Phase>,
        target: WorkflowStudioSceneTargetPayload,
        category: WorkflowStudioStateCategory,
        listener: WorkflowStudioSceneChangeListener,
    ): () => void {
        return snapshot.addPhaseChangeListener((event) => listener({
            target,
            category,
            phase: event.phase,
            active: event.active,
            activePhases: [...event.activePhases],
        }));
    }


    public applyMutation(mutation: WorkflowStudioSceneMutation): void {
        switch (mutation.category) {
            case 'canvasInteraction':
                this._canvasModel.setPhase(mutation.phase);
                return;
            case 'workflowState':
                this._workflowState.setPhase(mutation.phase);
                return;
            case 'slotOccupancy': {
                const target = this._slotModels.get(mutation.target.slotId);
                if (!target) throw new Error(`Unknown workflow slot model: ${mutation.target.slotId}`);
                target.occupancy.setPhase(mutation.phase);
                return;
            }
            case 'slotDropState': {
                const target = this._slotModels.get(mutation.target.slotId);
                if (!target) throw new Error(`Unknown workflow slot model: ${mutation.target.slotId}`);
                target.dropState.setPhase(mutation.phase);
                return;
            }
            case 'nodeExecutionState': {
                const target = this._nodeModels.get(mutation.target.nodeId);
                if (!target) throw new Error(`Unknown workflow node model: ${mutation.target.nodeId}`);
                target.execution.setPhase(mutation.phase);
                return;
            }
            case 'nodeConnectionState': {
                const target = this._nodeModels.get(mutation.target.nodeId);
                if (!target) throw new Error(`Unknown workflow node model: ${mutation.target.nodeId}`);
                target.connection.setPhase(mutation.phase);
                return;
            }
            case 'nodeOverlay': {
                const target = this._nodeModels.get(mutation.target.nodeId);
                if (!target) throw new Error(`Unknown workflow node model: ${mutation.target.nodeId}`);

                const active = mutation.active ?? true;
                // If the mutation is for the 'selected' phase and is being activated, ensures that all other nodes are deselected.
                if (mutation.phase === 'selected' && active) {
                    for (const [id, node] of this._nodeModels) {
                        if (id !== mutation.target.nodeId) node.overlay.setPhase('selected', false);
                    }
                }

                target.overlay.setPhase(mutation.phase, active);
                return;
            }
            case 'pathLifecycleState': {
                const target = this._pathModels.get(mutation.target.pathId);
                if (!target) throw new Error(`Unknown workflow path model: ${mutation.target.pathId}`);
                target.lifecycle.setPhase(mutation.phase);
                return;
            }
            case 'pathFlowState': {
                const target = this._pathModels.get(mutation.target.pathId);
                if (!target) throw new Error(`Unknown workflow path model: ${mutation.target.pathId}`);
                target.flow.setPhase(mutation.phase);
                return;
            }
        }
    }

    /**
     * Convenience wrapper for the currently supported node-overlay mutation.
     */
    public setNodeOverlay(nodeId: string, phase: NodeOverlay, active: boolean = true): void {
        this.applyMutation({
            target: { kind: 'node', nodeId },
            category: 'nodeOverlay',
            phase,
            active,
        });
    }
}

export {
    SlotModelSnapshot as SlotSnapshot,
    NodeModelSnapshot as NodeSnapshot
};

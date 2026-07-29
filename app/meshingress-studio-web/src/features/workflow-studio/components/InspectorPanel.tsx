import {useEffect, useState} from 'react'
import {ActionIcon, NodeIcon, PencilIcon, StackIcon} from '../../../components/icons/node-icons'
import {availableToolFunctions} from '../available-tool-functions'
import type {InspectorView, WorkflowEdge, WorkflowNode} from '../types'
import {workflowDefinition} from '../definition'
import {Empty} from './ExplorerPanel'

export function InspectorPanel({inspector, selectedNode, nodes, edges, onInspectorChange, onNodeChange}: {
    inspector: InspectorView;
    selectedNode?: WorkflowNode;
    nodes: WorkflowNode[];
    edges: WorkflowEdge[];
    onInspectorChange: (view: InspectorView) => void;
    onNodeChange: (id: string, changes: Partial<WorkflowNode>) => void
}) {
    const tabs: Array<{ id: InspectorView; label: string }> = [{id: 'node', label: 'Workflow node'}, {id: 'workflow', label: 'Workflow definition'}, {id: 'annotations', label: 'Node annotations'}, {
        id: 'payload',
        label: 'Node payload'
    }]
    const [editingTitle, setEditingTitle] = useState(false)
    const [titleDraft, setTitleDraft] = useState(selectedNode?.title ?? '')

    useEffect(() => {
        setEditingTitle(false)
        setTitleDraft(selectedNode?.title ?? '')
    }, [selectedNode?.id, selectedNode?.title])

    const commitTitle = () => {
        const title = titleDraft.trim()
        if (selectedNode && title) onNodeChange(selectedNode.id, {title})
        setEditingTitle(false)
    }

    return <aside className="right-panel">
        <div className="panel-head">
            <div className="panel-title node-name">{selectedNode ? <>
                <span className="node-icon panel-node-icon" title={`${selectedNode.toolId} / ${selectedNode.functionName}`}>
                    <NodeIcon nodeKind={selectedNode.kind} toolId={selectedNode.toolId}/>
                </span>{editingTitle ?
                <input aria-label="Node name" autoFocus className="node-name-input" onBlur={commitTitle} onChange={(event) =>
                    setTitleDraft(event.target.value)} onKeyDown={(event) => {
                    if (event.key === 'Enter') event.currentTarget.blur()
                }} value={titleDraft}/>
                : <button className="node-name-label" onClick={() => setEditingTitle(true)} type="button">{selectedNode.title}</button>}
                <button aria-label="Edit node name" className="node-name-edit" onClick={() => setEditingTitle(true)} title="Edit node name" type="button"><PencilIcon size={15}/></button>
            </> : 'No node selected'}</div>
        </div>
        <div className="tabs">{tabs.map((tab) => <button className={`tab${inspector === tab.id ? ' active' : ''}`} key={tab.id} onClick={() => onInspectorChange(tab.id)} type="button">{tab.label}</button>)}</div>
        <div className="inspector">{!selectedNode ? <Empty message="Select a workflow node."/> :
            <InspectorContent edges={edges} inspector={inspector} node={selectedNode} nodes={nodes} onNodeChange={onNodeChange}/>}</div>
    </aside>
}

function InspectorContent({inspector, node, nodes, edges, onNodeChange}: {
    inspector: InspectorView;
    node: WorkflowNode;
    nodes: WorkflowNode[];
    edges: WorkflowEdge[];
    onNodeChange: (id: string, changes: Partial<WorkflowNode>) => void
}) {
    if (inspector === 'workflow')
        return <JsonBox value={workflowDefinition(nodes, edges)}/>
    if (inspector === 'annotations')
        return <JsonBox value={node.annotations}/>
    if (inspector === 'payload')
        return <JsonBox value={node.arguments}/>

    const currentFunction = {id: `${node.toolId}.${node.functionName}`, toolId: node.toolId, functionName: node.functionName, title: node.toolId}
    const functionOptions = node.kind === 'trigger'
        ? [currentFunction]
        : availableToolFunctions.filter((option) => option.toolId === node.toolId)
    const selectableFunctions = functionOptions.length ? functionOptions : [currentFunction]

    const selectedFunctionId = selectableFunctions.find((option) => option.functionName === node.functionName)?.id ?? selectableFunctions[0].id
    const changeFunction = (functionId: string) => {
        const selectedFunction = selectableFunctions.find((option) => option.id === functionId)
        if (selectedFunction) onNodeChange(node.id, {functionName: selectedFunction.functionName})
    }
    const outputField =
        <div className="field">
            <label htmlFor="node-output">Output variables</label>
            <input id="node-output" onChange={(event) => onNodeChange(node.id, {output: event.target.value})} value={node.output}/>
        </div>
    return <>
        <div className="field"><label htmlFor="node-function">Function</label>
            <div className="function-select-wrap">
                <span aria-hidden="true" className="function-field-icon">
                  <ActionIcon size={15}/>
                </span>
                <span aria-hidden="true" className="function-dropdown-icon">
                  <StackIcon size={15}/>
                </span>
                <select id="node-function" className={"function-choice-field"}
                        onChange={(event) => changeFunction(event.target.value)}
                        value={selectedFunctionId}>{selectableFunctions.map((option) =>
                    <option key={option.id} value={option.id}>{option.functionName}</option>)}
                </select>
            </div>
        </div>
        <div className="section-heading">Arguments</div>
        {Object.entries(node.arguments).length ? Object.entries(node.arguments).map(([key, value]) =>
                <div className="box" key={key}>
                    <label className="muted" htmlFor={`argument-${key}`}>{key}</label>
                    <textarea id={`argument-${key}`} onChange={(event) => onNodeChange(node.id, {arguments: {...node.arguments, [key]: event.target.value}})} value={value}/>
                </div>) :
            <Empty message="No input arguments."/>}{outputField}</>
}

function JsonBox({value}: { value: unknown }) {
    return <div className="box">
        <pre>{JSON.stringify(value, null, 2)}</pre>
    </div>
}

import { useEffect, useState } from 'react'
import {
    ActionIcon,
    ArrayObjectTypeIcon,
    ArrowRight,
    BinaryObjectTypeIcon,
    BooleanObjectTypeIcon,
    CodeObjectTypeIcon,
    ColorObjectTypeIcon,
    DateObjectTypeIcon,
    DecimalObjectTypeIcon,
    DurationObjectTypeIcon,
    EmailObjectTypeIcon,
    EnumObjectTypeIcon,
    FileObjectTypeIcon,
    ImageObjectTypeIcon,
    IntervalObjectTypeIcon,
    JsonObjectTypeIcon,
    LocationObjectTypeIcon,
    NullObjectTypeIcon,
    NumberObjectTypeIcon,
    SecretObjectTypeIcon,
    TextObjectTypeIcon,
    TimeObjectTypeIcon,
    UnknownObjectTypeIcon,
    UrlObjectTypeIcon,
} from '../../../../components/icons/node-icons'
import type { McpToolFunction } from '../../../../api/mcp'
import { availableToolFunctions } from '../../available-tool-functions'
import type { WorkflowArgumentValue, WorkflowNode } from '../../types'
import { ArgumentInput } from '../argument-inputs/ArgumentInput'
import { argumentDescription, argumentTypeClass, isRequiredArgument, propertySchemaFor, type ArgumentSchema } from '../argument-inputs/contracts'
import { resolveArgumentType, type ArgumentTypeKind } from '../argument-inputs/schema-type'
import { Empty } from '../elements/Empty'
import type { ToolPresentationIndex } from '../../node-presentation'

export type EditNodePanelTitleProps = {
    selectedNode?: WorkflowNode;
    presentations: ToolPresentationIndex;
    onNodeChange: (id: string, changes: Partial<WorkflowNode>) => void
}

export interface NodeToolFunctionPanelProps {
    node: WorkflowNode
    toolFunctions: McpToolFunction[]
    onNodeChange: (id: string, changes: Partial<WorkflowNode>) => void
}

export function NodeToolFunctionPanel({ node, toolFunctions, onNodeChange }: NodeToolFunctionPanelProps) {
    const [showOptionalArguments, setShowOptionalArguments] = useState(false)
    const [isSelectOpen, setIsSelectOpen] = useState(false)

    useEffect(() => {
        setShowOptionalArguments(false)
        setIsSelectOpen(false)
    }, [node?.functionName, node?.id, node?.toolId])

    const selectableFunctions = functionOptionsFor(node, toolFunctions)
    const selectedFunctionId = selectableFunctions.find((option) => option.functionName === node.functionName)?.id ?? selectableFunctions[0]?.id ?? ''

    const changeFunction = (functionId: string) => {
        const selectedFunction = selectableFunctions.find((option) => option.id === functionId)
        if (selectedFunction) onNodeChange(node.id, {
            functionName: selectedFunction.functionName,
            arguments: argumentsFor(selectedFunction.inputSchema),
        })
    }

    const outputField = (
        <div className="field">
            <label htmlFor="node-output">Output variables</label>
            <input id="node-output" onChange={(event) => onNodeChange(node.id, { output: event.target.value })} value={node.output} />
        </div>
    )

    const inputSchema = inputSchemaFor(node, toolFunctions)
    const argumentNames = argumentNamesFor(inputSchema, node.arguments)
    const optionalArgumentNames = argumentNames.filter((name) => !isRequiredArgument(inputSchema, name))
    const visibleArgumentNames = showOptionalArguments
        ? argumentNames
        : argumentNames.filter((name) => isRequiredArgument(inputSchema, name))

    return (
        <>
            <div className="field">
                <label htmlFor="node-function">Function</label>
                <div className="function-select-wrap" data-open={isSelectOpen ? 'true' : undefined}>
                    <span aria-hidden="true" className="function-field-icon">
                        <ActionIcon size={15} />
                    </span>
                    <select
                        className="function-choice-field"
                        id="node-function"
                        onBlur={() => setIsSelectOpen(false)}
                        onChange={(event) => {
                            changeFunction(event.target.value)
                            setIsSelectOpen(false)
                        }}
                        onClick={() => setIsSelectOpen((open) => !open)}
                        value={selectedFunctionId}
                    >
                        {selectableFunctions.map((option) => (
                            <option key={option.id} value={option.id}>{option.functionName}</option>
                        ))}
                    </select>
                    <span aria-hidden="true" className="function-dropdown-icon">
                        <ArrowRight size={15} />
                    </span>
                </div>
            </div>
            <div className="section-heading">Arguments</div>
            {argumentNames.length ? (
                visibleArgumentNames.map((key) => {
                    const schema = propertySchemaFor(inputSchema, key)
                    const value = node.arguments[key] ?? defaultArgumentValue(schema)
                    const description = argumentDescription(schema)
                    const descriptionId = description ? `argument-${key}-description` : undefined
                    return (
                        <div className={`box function-field ${argumentTypeClass(schema)}`} key={key}>
                            <ArgumentLabel argName={key} label={(schema?.title as string) || key} required={isRequiredArgument(inputSchema, key)} schema={schema} />
                            {description && <p className="argument-description" id={descriptionId}>{description}</p>}
                            <ArgumentInput
                                describedById={descriptionId}
                                id={`argument-${key}`}
                                name={key}
                                onChange={(nextValue) =>
                                    onNodeChange(node.id, { arguments: { ...node.arguments, [key]: nextValue } })}
                                schema={schema}
                                value={value}
                            />
                        </div>
                    )
                })
            ) : (
                <Empty message="No input arguments." />
            )}
            {optionalArgumentNames.length > 0 && (
                <button
                    aria-expanded={showOptionalArguments}
                    className="optional-arguments-toggle"
                    onClick={() => setShowOptionalArguments((show) => !show)}
                    type="button"
                >
                    --- {showOptionalArguments ? 'show less' : 'show more'} <span aria-hidden="true" className="optional-arguments-toggle-icon">↗</span> ---
                </button>
            )}
            {outputField}
        </>
    )
}

function inputSchemaFor(node: WorkflowNode, toolFunctions: McpToolFunction[]): ArgumentSchema | undefined {
    const functionName = `${node.toolId}.${node.functionName}`
    return toolFunctions.find((functionEntry) => functionEntry.name === functionName)?.inputSchema
        ?? availableToolFunctions.find((functionEntry) => functionEntry.id === functionName)?.inputSchema
}

interface FunctionOption {
    id: string
    functionName: string
    inputSchema?: ArgumentSchema
}

function functionOptionsFor(node: WorkflowNode, toolFunctions: McpToolFunction[]): FunctionOption[] {
    const currentFunction: FunctionOption = {
        id: `${node.toolId}.${node.functionName}`,
        functionName: node.functionName,
        inputSchema: inputSchemaFor(node, toolFunctions),
    }
    if (node.kind === 'trigger') return [currentFunction]

    const liveOptions = toolFunctions.flatMap((functionEntry) => functionOptionFromName(node.toolId, functionEntry.name, functionEntry.inputSchema))
    const fallbackOptions = availableToolFunctions
        .filter((functionEntry) => functionEntry.toolId === node.toolId)
        .map((functionEntry) => ({ id: functionEntry.id, functionName: functionEntry.functionName, inputSchema: functionEntry.inputSchema }))
    const options = liveOptions.length ? liveOptions : fallbackOptions

    return options.some((option) => option.id === currentFunction.id) ? options : [currentFunction, ...options]
}

function functionOptionFromName(toolId: string, name: string, inputSchema?: ArgumentSchema): FunctionOption[] {
    const prefix = `${toolId}.`
    if (!name.startsWith(prefix)) return []

    const functionName = name.slice(prefix.length)
    return functionName ? [{ id: name, functionName, inputSchema }] : []
}

function argumentsFor(inputSchema: ArgumentSchema | undefined): Record<string, WorkflowArgumentValue> {
    const properties = inputSchema?.properties
    if (!properties || typeof properties !== 'object' || Array.isArray(properties)) return {}

    return Object.fromEntries(Object.entries(properties as Record<string, unknown>).map(([name, property]) =>
        [name, defaultArgumentValue(property as ArgumentSchema)]))
}

function defaultArgumentValue(schema: ArgumentSchema | undefined): WorkflowArgumentValue {
    return schema?.type === 'array' ? [] : ''
}

function argumentNamesFor(inputSchema: ArgumentSchema | undefined, argumentsByName: Record<string, WorkflowArgumentValue>): string[] {
    const properties = inputSchema?.properties
    const schemaNames = properties && typeof properties === 'object' && !Array.isArray(properties)
        ? Object.keys(properties as Record<string, unknown>)
        : []

    return [...schemaNames, ...Object.keys(argumentsByName).filter((name) => !schemaNames.includes(name))]
}

function ArgumentLabel({ argName, label = argName, schema, required }: { argName: string; label?: string; schema?: ArgumentSchema; required: boolean }) {
    const type = resolveArgumentType(schema)

    return (
        <label className="argument-label muted" htmlFor={`argument-${argName}`}>
            <span aria-hidden="true" className="argument-type-icon" title={type.label}>
                <ParameterTypeIcon type={type.kind} />
            </span>
            <span>{label}</span>
            {required && <span aria-label="required" className="argument-required">*</span>}
        </label>
    )
}

function ParameterTypeIcon({ type }: { type: ArgumentTypeKind }) {
    const commonProps = { size: 14 }

    switch (type) {
        case 'enum': return <EnumObjectTypeIcon {...commonProps} />
        case 'date': return <DateObjectTypeIcon {...commonProps} />
        case 'time': return <TimeObjectTypeIcon {...commonProps} />
        case 'clock': return <TimeObjectTypeIcon {...commonProps} />
        case 'duration': return <DurationObjectTypeIcon {...commonProps} />
        case 'interval': return <IntervalObjectTypeIcon {...commonProps} />
        case 'url': return <UrlObjectTypeIcon {...commonProps} />
        case 'email': return <EmailObjectTypeIcon {...commonProps} />
        case 'secret': return <SecretObjectTypeIcon {...commonProps} />
        case 'binary': return <BinaryObjectTypeIcon {...commonProps} />
        case 'color': return <ColorObjectTypeIcon {...commonProps} />
        case 'text': return <TextObjectTypeIcon {...commonProps} />
        case 'decimal': return <DecimalObjectTypeIcon {...commonProps} />
        case 'number': return <NumberObjectTypeIcon {...commonProps} />
        case 'array': return <ArrayObjectTypeIcon {...commonProps} />
        case 'json': return <JsonObjectTypeIcon {...commonProps} />
        case 'boolean': return <BooleanObjectTypeIcon {...commonProps} />
        case 'null': return <NullObjectTypeIcon {...commonProps} />
        case 'code': return <CodeObjectTypeIcon {...commonProps} />
        case 'file': return <FileObjectTypeIcon {...commonProps} />
        case 'image': return <ImageObjectTypeIcon {...commonProps} />
        case 'location': return <LocationObjectTypeIcon {...commonProps} />
        case 'geo': return <LocationObjectTypeIcon {...commonProps} />
        default: return <UnknownObjectTypeIcon {...commonProps} />
    }
}

export default NodeToolFunctionPanel

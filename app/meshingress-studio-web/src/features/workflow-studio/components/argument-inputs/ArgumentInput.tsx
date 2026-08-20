import {BooleanArgumentInput} from './BooleanArgumentInput'
import {ArrayArgumentInput} from './ArrayArgumentInput'
import {ColorArgumentInput} from './ColorArgumentInput'
import {DateArgumentInput} from './DateArgumentInput'
import {DateTimeArgumentInput} from './DateTimeArgumentInput'
import {EmailArgumentInput} from './EmailArgumentInput'
import {EnumArgumentInput} from './EnumArgumentInput'
import {IntegerArgumentInput} from './IntegerArgumentInput'
import {NumberArgumentInput} from './NumberArgumentInput'
import {PasswordArgumentInput} from './PasswordArgumentInput'
import {TextArgumentInput} from './TextArgumentInput'
import {TimeArgumentInput} from './TimeArgumentInput'
import {UrlArgumentInput} from './UrlArgumentInput'
import {isDateTimeValue, isDateValue, isTimeValue, normalizeArgumentSchema, type ArgumentInputProps} from './contracts'
import {resolveArgumentType} from './schema-type'

export function ArgumentInput(props: ArgumentInputProps) {
    const schema = normalizeArgumentSchema(props.schema)
    const type = resolveArgumentType(props.schema)
    if (type.kind === 'array') return <ArrayArgumentInput {...props}/>

    const scalarProps = {...props, value: typeof props.value === 'string' ? props.value : ''}

    if (type.kind === 'enum' && (scalarProps.value === '' || schema.enumValues.includes(scalarProps.value))) return <EnumArgumentInput {...scalarProps}/>
    if (props.schema?.format === 'date' && (scalarProps.value === '' || isDateValue(scalarProps.value))) return <DateArgumentInput {...scalarProps}/>
    if (props.schema?.format === 'date-time' && (scalarProps.value === '' || isDateTimeValue(scalarProps.value))) return <DateTimeArgumentInput {...scalarProps}/>
    if (type.kind === 'time' && (scalarProps.value === '' || isTimeValue(scalarProps.value))) return <TimeArgumentInput {...scalarProps}/>
    if (type.kind === 'url') return <UrlArgumentInput {...scalarProps}/>
    if (type.kind === 'email') return <EmailArgumentInput {...scalarProps}/>
    if (type.kind === 'secret') return <PasswordArgumentInput {...scalarProps}/>
    if (type.kind === 'color') return <ColorArgumentInput {...scalarProps}/>
    if (type.kind === 'number') return <IntegerArgumentInput {...scalarProps}/>
    if (type.kind === 'decimal') return <NumberArgumentInput {...scalarProps}/>
    if (type.kind === 'boolean' && (scalarProps.value === 'true' || scalarProps.value === 'false')) return <BooleanArgumentInput {...scalarProps}/>

    return <TextArgumentInput {...scalarProps}/>
}

import {NumberArgumentInput} from './NumberArgumentInput'
import type {ScalarArgumentInputProps} from './contracts'

export function IntegerArgumentInput(props: ScalarArgumentInputProps) {
    return <NumberArgumentInput {...props} integer/>
}

import { MinusIcon } from '../../../../components/icons/node-icons'

interface HideActionProps {
    onHide: () => void
}

export function HideAction({ onHide }: HideActionProps) {
    return (
        <button
            aria-label="Hide"
            className="panel-action-btn button small"
            onClick={onHide}
            title="Hide"
            type="button"
        >
            <MinusIcon size={16} />
        </button>
    )
}

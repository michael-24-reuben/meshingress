import { CoffeeLoop } from '../../../../components/icons/node-icons'

export function Empty({ message }: { message: string }) {
    return <div className="empty">
        <CoffeeLoop size={28} />
        <span>{message}</span>
    </div>
}

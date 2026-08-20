import { useState } from 'react'

export function useTreeExpand(onStatus?: (message: string) => void) {
    const [expandAllState, setExpandAllState] = useState<boolean | null>(null)
    const [expandKey, setExpandKey] = useState(0)

    const handleExpandAll = () => {
        setExpandAllState(true)
        setExpandKey((prev) => prev + 1)
        onStatus?.('Expanded tool tree')
    }

    const handleCollapseAll = () => {
        setExpandAllState(false)
        setExpandKey((prev) => prev + 1)
        onStatus?.('Collapsed tool tree')
    }

    return {
        expandAllState,
        expandKey,
        handleExpandAll,
        handleCollapseAll,
    }
}

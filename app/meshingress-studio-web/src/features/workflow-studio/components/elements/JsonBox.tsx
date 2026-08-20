export function JsonBox({ value }: { value: unknown }) {
    return (
        <div className="box">
            <pre>{JSON.stringify(value, null, 2)}</pre>
        </div>
    )
}

export default JsonBox

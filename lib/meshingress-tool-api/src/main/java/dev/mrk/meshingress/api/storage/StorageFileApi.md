# `StorageFileApi`

`StorageFileApi` is the provider-neutral contract for a single storage backend. It deliberately exposes the same method vocabulary for local and foreign implementations, but callers must first inspect `capabilities()` and must handle the returned `StorageResult`.

## Write lifecycle

1. Call `createFile` with a namespace/path key, MIME type, and optional expected size.
2. Use the returned `WriteHandle` with `writeFile`, or with one or more `appendFile` calls where supported.
3. Call `completeFile` to make the write final and receive its `FileReceipt`.
4. Call `abortFile` only while the handle is uncommitted.

Creation is create-only: a collision returns `CONFLICT`; implementations must not turn it into an overwrite.

## Observing and removing files

`containsFile`, `getFileMetadata`, `getFileSize`, `readFile`, and `deleteFile` are the observation or mutation operations for a completed file. They are available for a normal local backend when its capabilities allow them.

Write-only foreign backends must deny these calls. Meshingress uses its local metadata/audit store for handoff history instead of probing, reading, modifying, or deleting a foreign object after publication.

## Results

Every operation returns `StorageResult<T>` rather than relying on a provider-specific exception contract:

- `SUCCESS` — `value` contains the requested result.
- `NOT_FOUND` — the key is absent where observation is allowed.
- `CONFLICT` — a create-only target already exists.
- `UNSUPPORTED` — the backend does not implement that operation.
- `DENIED` — the backend policy forbids it, such as a foreign write-only read.
- `FAILED` — an execution failure occurred; `message` is safe diagnostic text.

`InputStream` values returned by `readFile` belong to the caller and must be closed.

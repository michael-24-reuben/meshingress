# Ephemeral Content Storage

## Goal

Plan a generic temporary-content storage capability exposed under `/storage`.

The capability is intended for physical content such as:

- media
- articles
- PDFs
- archives
- tool-produced documents
- similar downloadable byte streams

A source tool may materialize content into this storage layer and return a predictable MCP `structuredContent` lease node. The lease gives the caller an opaque retrieval URL without disclosing a filesystem path, source URL, internal filename, or storage layout.

## Architectural Position

`/storage` is a first-class Meshingress server route alongside `/mcp` and `/artifact`.

It is not:

- a Toonverse-specific endpoint
- an MCP tool family
- a replacement for `/artifact`
- an extension of `meshingress.cache`

The boundaries are:

| Capability | Purpose | Durability |
|---|---|---|
| `/artifact` | Versioned package and publication storage | Durable |
| `/storage` | Temporary delivery of physical content | Ephemeral |
| `meshingress.cache` | Reuse of computed result values | Cache lifecycle |

## Required Storage Model

- Content bytes live under a dedicated filesystem root.
- The database stores lease metadata, lifecycle state, access data, checksums, byte counts, limits, and audit associations.
- Filesystem paths and upstream source URLs are never returned to callers.
- Retrieval uses an opaque capability token.
- Expired and exhausted entries are deleted.
- Ingestion writes to a temporary file, enforces byte limits while streaming, and atomically publishes only a complete entry.

## Proposed Route Surface

| Method | Route | Purpose |
|---|---|---|
| `POST` | `/storage` | Create and ingest an ephemeral entry. Caller and authorization contract remain undecided. |
| `GET` | `/storage/{accessToken}` | Retrieve content bytes. |
| `HEAD` | `/storage/{accessToken}` | Retrieve representation headers without transferring bytes. |
| `DELETE` | `/storage/{storageId}` | Authenticated early deletion. |

Cleanup remains internal and scheduled. It is not a public route.

## Planning Boundary

This record authorizes design work only.

Do not create or change:

- Maven modules
- controllers
- route mappings
- database tables or migrations
- repositories
- schedulers
- cleanup jobs
- filesystem roots
- configuration properties
- tool integrations
- Toonverse behavior

Implementation requires a separate, explicit authorization.

# Brief

Implement the missing ordinary-profile lifecycle in the reusable, framework-free
`packages/aegis` module before any Meshingress API or MCP dispatch wiring.

The resulting contract must support creation, retrieval, bounded listing,
metadata updates, status lifecycle changes, identity linking/unlinking, and
credential-binding lifecycle operations while retaining no raw credential
material. Concurrent writes must use an explicit revision check.

This entry also names the eventual privileged MCP dispatch methods so their
external names are decided before an adapter is introduced.

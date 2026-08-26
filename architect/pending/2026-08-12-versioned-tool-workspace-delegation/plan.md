# Plan

1. Add a tool-author API that provides the loaded tool's coordinate-bound workspace referral and safe child-path resolution.
2. Have the server resolve the workspace from the authoritative artifact repository layout, create it on demand, and inject it into the module context. Do not derive it from `user.dir` or `CodeSource`.
3. Make ordinary reads and all writes use only the current coordinate workspace.
4. Add an explicit read delegation mode or method. It must be visible at the call site and define which earlier versions are eligible.
5. Resolve delegated reads as current version first, then prior eligible versions in descending Maven-version order. Do not use lexical directory ordering.
6. Enforce normalized child paths that remain inside every candidate `workspace/` directory; reject absolute and escaping paths.
7. Define lifecycle behavior for artifact deletion, version retention, and explicit migration. Do not delete a version workspace through cache cleanup.
8. Add focused tests for workspace resolution, containment, write isolation, current-first delegation, version ordering, absent resources, and sensitive-resource non-delegation.

# Assessment

The requested behavior belongs entirely in the Studio client: a browser directory selection can build an in-memory tree while retaining the local-data boundary. A browser cannot reliably expose a full native path through the fallback selector, so the UI intentionally represents the chosen root and relative paths instead.

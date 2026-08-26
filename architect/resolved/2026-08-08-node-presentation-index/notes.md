# Notes

- 2026-08-08: The current `ToolNodeIcon` uses `includes(...)` conditions against tool IDs. This slice removes those conditions; only explicit built-in descriptors and resolved module-image descriptors remain.

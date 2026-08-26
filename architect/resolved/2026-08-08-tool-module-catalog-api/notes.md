# Notes

- 2026-08-08: The owner selected inline bounded manifest data and linked README/icon resources. The implementation must support open-source/classpath modules, not only repository JAR artifacts.
- 2026-08-08: `ToolModuleCatalog` registers manifest beans after Spring initialization with a stable server-owned classpath ID, and runtime loading registers the module-local manifest against `ToolModuleId`. Catalog resources are loaded only from paths declared by the manifest and are bounded before response.
- 2026-08-08: The Studio maps the first published icon for each namespace and retains `FolderIcon` when the catalog has no icon or its image cannot load.

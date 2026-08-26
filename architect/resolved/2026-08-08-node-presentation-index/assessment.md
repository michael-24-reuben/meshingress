# Assessment

The previous `ToolNodeIcon` rendered module-specific icons by matching callable tool-name strings. That duplicated identity knowledge in each visual caller and could not identify multiple extension modules under one namespace deterministically.

The server already owns both sources of identity: manifest definitions for classpath modules and runtime owners for dynamically loaded modules. The correct boundary is to expose an opaque `moduleToolId` on each public MCP function and let the Studio map it to the module catalog once.

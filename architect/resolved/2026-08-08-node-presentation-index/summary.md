# Summary

Resolved the tool-module icon path as a shared presentation system. The backend links public MCP functions to opaque module catalog IDs; the Studio resolves that identity once and renders the same result consistently across the explorer and workflow surfaces. This supports bundled/open-source classpath modules and runtime-installed modules without coupling UI rendering to callable name patterns.

Persisting the optional `moduleToolId` in new `WorkflowNode` documents remains a later workflow-model slice; current nodes retain execution-compatible callable `toolId` values and resolve presentation from the live function catalog.

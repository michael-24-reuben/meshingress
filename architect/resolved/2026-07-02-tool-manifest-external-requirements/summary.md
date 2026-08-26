# Summary

Meshingress tool metadata now has a class-based manifest path. Tool authors can define a manifest class with properties, requirements, links, and README text instead of attaching `McpToolProperty` and `McpToolReadme` annotations to executable classes.

Repository assessment consumes static `tool-manifest.json` resources from quarantined artifacts, preserving the safety boundary that uploaded code is not executed for metadata extraction. Runtime install and metadata lookup also understand the static manifest resource.

`PowerShellCliTool` is the pilot migration. Its production manifest declares only PowerShell-specific properties, scope requirements, executable requirement, links, and README text.

The managed source repository cache/reuse lifecycle is intentionally deferred. The data model can declare and canonicalize source repository requirements, but checkout persistence and cleanup safety should be handled in a separate architect.

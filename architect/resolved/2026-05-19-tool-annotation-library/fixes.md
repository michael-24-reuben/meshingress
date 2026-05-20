# Fixes

## Files Changed

- `pom.xml`
- `lib/meshingress-tool-annotations/pom.xml`
- `lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/*`
- `lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/model/*`
- `lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/scanner/McpToolAnnotationScanner.java`
- `lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/schema/McpJsonSchemaProvider.java`
- `lib/meshingress-tool-annotations/src/test/java/dev/mrk/meshingress/api/tools/annotation/scanner/McpToolAnnotationScannerTests.java`

## Behavioral Scope

- Added a new Maven reactor module for annotation-based tool metadata.
- Added annotations for tools, tool mappings, functions, function parameters, function annotations, tool annotations, input schemas, and input fields.
- Added scanner/model classes that inspect annotated classes and produce metadata/descriptors.
- Did not attach the module to `ToolRegistry`, `ToolExecutor`, `ToolsMcpController`, or server execution logic.

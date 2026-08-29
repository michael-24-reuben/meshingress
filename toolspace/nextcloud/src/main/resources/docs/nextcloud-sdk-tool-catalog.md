# Nextcloud MCP Tool Catalog SDK Manual

Module: `nextcloud-sdk-tool-catalog`  
Artifact: `io.github.uriakleahcim.nextcloud:nextcloud-sdk-tool-catalog:0.0.1-SNAPSHOT`  
JPMS Module: `io.github.uriakleahcim.nextcloud.tool.catalog`

---

## Table of Contents

1. [Overview](#overview)
2. [Module Architecture & Encapsulation](#module-architecture--encapsulation)
3. [Maven Dependency Declaration](#maven-dependency-declaration)
4. [API Reference & Callable Blocks](#api-reference--callable-blocks)
   - [Tool Catalog Model (`ToolCatalog`, `ToolCatalogEntry`)](#tool-catalog-model)
   - [Catalog Builder (`ToolCatalogBuilder`)](#catalog-builder)
   - [Catalog Exporters (`ToolCatalogExporter`, `ToolCatalogJsonWriter`)](#catalog-exporters)
   - [Schema Normalization (`ToolDescriptorNormalizer`, `NormalizedParameter`, `ExportFormat`)](#schema-normalization)
5. [Complete SDK Usage Snippets](#complete-sdk-usage-snippets)

---

## Overview

The `nextcloud-sdk-tool-catalog` module aggregates, normalizes, categorizes, and exports tool descriptors and JSON Schema definitions. It formats catalog models into standard MCP protocol schemas, OpenRPC manifests, or human-readable Markdown reference manuals.

---

## Module Architecture & Encapsulation

```java
module io.github.uriakleahcim.nextcloud.tool.catalog {
    requires transitive io.github.uriakleahcim.nextcloud.core;
    requires transitive io.github.uriakleahcim.nextcloud.tool.api;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;

    exports io.github.uriakleahcim.nextcloud.tool.catalog;

    opens io.github.uriakleahcim.nextcloud.tool.catalog to com.fasterxml.jackson.databind;
}
```

---

## Maven Dependency Declaration

```xml
<dependency>
    <groupId>io.github.uriakleahcim.nextcloud</groupId>
    <artifactId>nextcloud-sdk-tool-catalog</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

---

## API Reference & Callable Blocks

### Tool Catalog Model

#### [`ToolCatalog`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-catalog/src/main/java/org/mcp/nextcloud/tool/catalog/ToolCatalog.java)
- **Components**: `String name()`, `String version()`, `List<ToolCatalogEntry> tools()`, `Map<String, List<ToolCatalogEntry>> byCategory()`, `Map<String, Object> metadata()`.
- **Callable**:
  - `int size()`: Total registered tools.
  - `boolean isEmpty()`: True if catalog has 0 tools.
  - `Optional<ToolCatalogEntry> find(String toolName)`: Looks up tool entry by ID or name.
  - `Set<String> categories()`: Distinct category names (e.g. `files`, `shares`, `trash`, `admin`).
  - `List<ToolCatalogEntry> forCategory(String category)`: Filtered tool entries.
  - `Map<String, Object> toMap()`: Raw JSON-serializable map representation.

#### [`ToolCatalogEntry`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-catalog/src/main/java/org/mcp/nextcloud/tool/catalog/ToolCatalogEntry.java)
- **Components**: `String id()`, `String name()`, `String category()`, `String description()`, `boolean destructive()`, `Set<String> requiredScopes()`, `List<NormalizedParameter> parameters()`, `Map<String, Object> inputJsonSchema()`, `Map<String, Object> metadata()`.
- **Callable**:
  - `Map<String, Object> toMap()`
  - `Map<String, Object> toMcpToolMap()`: Standard MCP `tools/list` JSON format payload.

---

### Catalog Builder

#### [`ToolCatalogBuilder`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-catalog/src/main/java/org/mcp/nextcloud/tool/catalog/ToolCatalogBuilder.java)
- `ToolCatalogBuilder name(String name)`
- `ToolCatalogBuilder version(String version)`
- `ToolCatalogBuilder add(ToolDescriptor descriptor)`
- `ToolCatalogBuilder addAll(Iterable<ToolDescriptor> descriptors)`
- `ToolCatalogBuilder metadata(String key, Object value)`
- `ToolCatalog build()`: Compiles descriptors, normalizes schemas, and groups tools by category.

---

### Catalog Exporters

#### [`ToolCatalogExporter`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-catalog/src/main/java/org/mcp/nextcloud/tool/catalog/ToolCatalogExporter.java)
- `new ToolCatalogExporter()` / `new ToolCatalogExporter(ToolCatalogJsonWriter writer)`
- `String export(ToolCatalog catalog, ExportFormat format) throws IOException`
- `String exportJson(ToolCatalog catalog) throws IOException`
- `String exportMcpJson(ToolCatalog catalog) throws IOException`
- `String exportMarkdown(ToolCatalog catalog)`

#### [`ToolCatalogJsonWriter`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-catalog/src/main/java/org/mcp/nextcloud/tool/catalog/ToolCatalogJsonWriter.java)
- Pretty-prints Jackson JSON outputs.

---

### Schema Normalization

#### [`ToolDescriptorNormalizer`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-catalog/src/main/java/org/mcp/nextcloud/tool/catalog/ToolDescriptorNormalizer.java)
- `static ToolCatalogEntry normalize(ToolDescriptor descriptor)`: Translates Java tool descriptors into JSON Schema objects.

#### [`ExportFormat`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-tool-catalog/src/main/java/org/mcp/nextcloud/tool/catalog/ExportFormat.java)
- Values: `JSON`, `MCP_JSON`, `MARKDOWN`.

---

## Complete SDK Usage Snippets

### 1. Building a Tool Catalog and Exporting to Markdown and MCP JSON

```java

import io.github.uriakleahcim.nextcloud.tool.catalog.ExportFormat;
import io.github.uriakleahcim.nextcloud.tool.catalog.ToolCatalog;
import io.github.uriakleahcim.nextcloud.tool.catalog.ToolCatalogBuilder;
import io.github.uriakleahcim.nextcloud.tool.catalog.ToolCatalogExporter;
import io.github.uriakleahcim.nextcloud.tool.runtime.InMemoryToolRegistry;

// 1. Construct catalog from registered tools
InMemoryToolRegistry registry = new InMemoryToolRegistry();
// ... populate tools ...

        ToolCatalog catalog = new ToolCatalogBuilder()
                .name("Nextcloud MCP Capabilities")
                .version("1.0.0")
                .addAll(registry.list().stream().map(r -> r.descriptor()).toList())
                .build();

System.out.

        println("Catalog built with "+catalog.size() +" tools across categories: "+catalog.

        categories());

        // 2. Export Markdown Documentation
        ToolCatalogExporter exporter = new ToolCatalogExporter();
        String markdownDoc = exporter.exportMarkdown(catalog);
System.out.

        println("Markdown doc length: "+markdownDoc.length());

        // 3. Export MCP JSON tools/list payload
        String mcpToolsJson = exporter.export(catalog, ExportFormat.MCP_JSON);
System.out.

        println("MCP JSON:\n"+mcpToolsJson);
```

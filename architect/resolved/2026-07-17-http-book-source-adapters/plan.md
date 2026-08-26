# Implementation Plan

## 1. Architectural Shape

```text
book-source-api
  -> canonical models and provider interfaces

book-image tool module
  -> annotated ImageBookTool
  -> ImageBookSourceRegistry

book-text tool module
  -> annotated TextBookTool
  -> TextBookSourceRegistry

book-http-runtime
  -> generic configured HTTP execution
  -> selector access helpers

Toonverse source extension
  -> toonverse.yaml
  -> ToonverseClient
  -> ToonverseMapper
  -> ToonverseImageBookSource
  -> ToonverseSourceAutoConfiguration
```

Module names are provisional. Preserve dependency direction even if names change.

## 2. Dependency Direction

```text
book-image tool ---------> book-source-api
book-text tool ----------> book-source-api
book-http-runtime -------> book-source-api
Toonverse source --------> book-source-api + book-http-runtime
server/tool bundle ------> general tools + selected source extensions
```

The general tool must not depend directly on Toonverse.

## 3. Proposed Module Layout

```text
lib/
├── book-source-api/
│   ├── BookKind
│   ├── PublicationType
│   ├── source contracts
│   ├── capability interfaces
│   ├── canonical request/result records
│   └── source descriptors
│
├── book-http-runtime/
│   ├── SourceHttpClient
│   ├── endpoint resolver
│   ├── query/path encoder
│   ├── selector helper
│   ├── redaction
│   └── source configuration loader
│
└── book-schema/
    ├── image-book-result.schema.json
    ├── text-book-result.schema.json
    └── validation support

toolspace/
├── book-image/
│   ├── ImageBookTool
│   ├── ImageBookSourceRegistry
│   └── ImageBookToolAutoConfiguration
│
├── book-text/
│   ├── TextBookTool
│   ├── TextBookSourceRegistry
│   └── TextBookToolAutoConfiguration
│
└── toonverse/
    ├── ToonverseImageBookSource
    ├── ToonverseClient
    ├── ToonverseMapper
    ├── ToonverseProperties/Configuration
    ├── ToonverseSourceAutoConfiguration
    └── resources/sources/toonverse.yaml
```

If a separate `book-schema` module is excessive for the first slice, schemas may begin in `book-source-api` resources and be extracted later.

## 4. General Tool Contract

The public class is the actual annotated tool, not a special unannotated “starter” policy.

```java
@McpTool(
        value = "book.image",
        title = "Image Book",
        description = "Extract image-oriented books from installed sources.",
        defaultFunction = "extract"
)
@McpToolMapping("tools")
@McpToolScopes({
        McpToolScope.HTTP_CLIENT,
        McpToolScope.EXTERNAL_API_READ
})
public final class ImageBookTool {
    private final ImageBookSourceRegistry sources;
    private final ObjectMapper objectMapper;

    public ImageBookTool(
            List<ImageBookSource> sourceProviders,
            ObjectMapper objectMapper
    ) {
        this.sources = new ImageBookSourceRegistry(sourceProviders);
        this.objectMapper = objectMapper;
    }
}
```

The text tool follows the same structure with `List<TextBookSource>`.

## 5. Registry Design

Build the registry once during bean creation.

Required operations:

```text
require(sourceId)
requireCapability(sourceId, capabilityClass)
descriptors()
contains(sourceId)
```

Normalize source IDs using trim plus lowercase. Reject duplicates after normalization.

Capability discovery should rely on Java interfaces, not only booleans in a descriptor.

## 6. Source Auto-Configuration

Each source extension contributes its beans through an auto-configuration class and imports entry.

```java
@AutoConfiguration
public class ToonverseSourceAutoConfiguration {

    @Bean
    ToonverseSourceConfiguration toonverseSourceConfiguration(
            SourceConfigurationLoader loader
    ) {
        return loader.load(
                "classpath:/sources/toonverse.yaml",
                ToonverseSourceConfiguration.class
        );
    }

    @Bean
    ToonverseClient toonverseClient(
            SourceHttpClient httpClient,
            ToonverseSourceConfiguration configuration
    ) {
        return new ToonverseClient(httpClient, configuration);
    }

    @Bean
    ToonverseImageBookSource toonverseImageBookSource(
            ToonverseClient client,
            ToonverseMapper mapper
    ) {
        return new ToonverseImageBookSource(client, mapper);
    }
}
```

Resource:

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Content:

```text
dev.mrk.toolspace.toonverse.ToonverseSourceAutoConfiguration
```

## 7. Toonverse Code-Owned Workflow

```text
input slug
  -> fetch series metadata
  -> select $.data
  -> require metadata.id
  -> fetch chapters using series ID
  -> select chapter payload
  -> map metadata and chapters to canonical result
  -> validate
  -> return
```

Illustrative method:

```java
@Override
public ImageBookResult extract(ImageBookExtractRequest request) {
    JsonNode metadata = client.fetchSeriesMetadata(request.slug());
    String seriesId = selectors.requiredText(metadata, "series.id");

    JsonNode chapters = client.fetchSeriesChapters(
            seriesId,
            request.limit(),
            request.order()
    );

    return mapper.toImageBook(metadata, chapters);
}
```

No workflow definition file is needed.

## 8. YAML Loader

Use typed configuration objects and validate required values at startup.

Minimum startup checks:

- source ID is nonblank;
- API base URL is absolute HTTPS unless development policy permits HTTP;
- referenced endpoint keys exist;
- path templates only reference known placeholders;
- required response selectors are nonblank;
- pagination maxima are positive;
- authentication mode is recognized.

YAML selector syntax should remain narrow. JSONPath is sufficient for the first implementation.

## 9. Mapping

Prefer a source-specific mapper because canonicalization is behavioral and source-dependent.

Examples:

- map `alternativeNames` to `alternativeTitles`;
- map `type=manhwa` to `PublicationType.MANHWA`;
- map `status=ongoing` to `PublicationStatus.ONGOING`;
- prefer `synopsis`, then `description`;
- map `genres[*].name` to canonical genre names;
- keep metrics outside the required core result.

## 10. Result Validation

Use Java records for internal correctness and JSON Schema at the external boundary.

Suggested flow:

```text
source response
  -> source mapper
  -> canonical Java record
  -> semantic validator
  -> Jackson JsonNode
  -> JSON Schema validation
  -> DispatchExecutionResult
```

At minimum, schemas must be exercised in unit tests against fixture output.

## 11. Error Model

Return structured errors for:

```text
UNKNOWN_BOOK_SOURCE
UNSUPPORTED_SOURCE_CAPABILITY
INVALID_SOURCE_CONFIGURATION
SOURCE_REQUEST_FAILED
SOURCE_RATE_LIMITED
SOURCE_UNAUTHORIZED
SOURCE_WORK_NOT_FOUND
SOURCE_RESPONSE_INVALID
CANONICAL_RESULT_INVALID
```

Do not expose response bodies containing secrets or sensitive upstream diagnostics by default.

## 12. Testing Strategy

### Registry tests

- indexes multiple providers;
- rejects duplicate IDs;
- resolves by normalized ID;
- rejects missing source;
- rejects unsupported capability.

### Configuration tests

- loads valid Toonverse YAML;
- rejects missing base URL;
- rejects invalid path placeholders;
- supports selector fallback lists;
- omits optional query parameters.

### Client tests

Use a mock HTTP server to verify:

- correct metadata URL;
- path-segment encoding;
- series ID carried into chapters request;
- limit/order query serialization;
- authorization redaction;
- 404/429/500 handling.

### Mapper tests

Use captured fixtures to verify:

- title and alternate names;
- author/artist;
- status and publication type normalization;
- genres;
- chapter count and chapter list;
- source-native ID preservation.

### Tool tests

- source appears in `sources`;
- dispatch selects Toonverse;
- result uses structured content;
- error state is represented through `DispatchExecutionResult`;
- required scopes appear in the function descriptor.

## 13. Implementation Phases

### Phase 1 — Contracts

- create `BookKind` and `PublicationType`;
- create canonical metadata records;
- create image source interfaces;
- create source descriptor and registry tests.

### Phase 2 — General image tool

- create annotated `ImageBookTool`;
- add `sources` and `metadata` functions;
- add auto-configuration;
- verify tool discovery.

### Phase 3 — HTTP runtime and Toonverse

- create minimal source YAML loader;
- create generic HTTP endpoint execution support;
- create Toonverse client and mapper;
- register Toonverse source bean;
- verify metadata extraction.

### Phase 4 — Chapters

- add chapter capability;
- carry metadata ID into chapter request;
- add pagination defaults and limits;
- compile complete image-book result.

### Phase 5 — Text tool

- derive the text-book contracts from proven image-book abstractions;
- avoid forcing page-image assumptions into text chapters;
- add the first textual source only after the shared boundary is stable.

### Phase 6 — Validation and hardening

- add JSON Schema validation tests;
- add redaction tests;
- add rate-limit/retry policy;
- add source health diagnostics if needed.

## 14. Rejected Alternatives

### Playwright-first extraction

Rejected for the initial implementation because HTTP endpoints are easier to maintain and less sensitive to presentation-layer changes.

### Fully declarative operation nodes

Rejected because defining methods, arguments, exported responses, persisted node results, workflows, and compilers would create a custom workflow engine whose maintenance cost exceeds the value of the extractor.

### YAML workflow sequencing

Rejected because metadata-to-chapter orchestration is clearer, safer, and easier to test in source-specific Java code.

### One interface with every method

Rejected because sources may support metadata but not pages, or chapters but not search. Capability-specific interfaces prevent unsupported no-op methods.

### Separate runtime kinds for manga/manhwa/manhua

Rejected because those are publication formats under the same image-book extraction contract.

## 15. Risks

### Undocumented API changes

Mitigation: keep paths and selectors in YAML, validate source fixtures, and isolate source-specific mapping.

### YAML grows into a programming language

Mitigation: prohibit loops, conditions, expressions, operation dependencies, and arbitrary transforms.

### General tool becomes source-aware

Mitigation: dispatch only through source contracts and registries; prohibit direct Toonverse dependencies.

### Canonical model becomes too broad

Mitigation: keep a required core plus optional source/catalog/metrics sections.

### Tokens leak through diagnostics

Mitigation: execution-scoped token handling, header redaction, and tests that inspect errors/log payloads.

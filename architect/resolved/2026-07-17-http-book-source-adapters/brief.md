# HTTP Book Source Adapters

## Objective

Create two general Meshingress tools for extracting books through public or authorized HTTP APIs:

- an **image-book tool** for manga, manhwa, manhua, webtoons, comics, and graphic novels;
- a **text-book tool** for novels, light novels, serialized prose, and similar textual works.

Each general tool discovers every installed source adapter of its compatible book kind. Installing a source package such as Toonverse contributes an internal Spring bean; it does not create a second public tool with overlapping functions.

## Core Design

```text
Annotated general tool
  -> source registry
    -> injected source adapter selected by source ID
      -> source-specific Java workflow
        -> shared HTTP client
          -> mutable source YAML
            -> canonical book result
              -> DispatchExecutionResult
```

The architectural boundary is deliberate:

```text
YAML answers: Where is mutable source data located?
Java answers: What should execute, in what order, and how is it assembled?
```

## Problem Statement

Book and comic websites expose similar concepts but different APIs, paths, wrappers, pagination rules, field names, and authentication requirements. A fully declarative workflow engine would be expensive to build and maintain. Hard-coding every URL and JSON path would make source updates unnecessarily invasive.

The design therefore uses:

- Java source adapters for behavior and orchestration;
- YAML for source values likely to change independently of behavior;
- shared canonical models for the final result;
- source capability interfaces for safe dispatch;
- Spring auto-configuration for source discovery.

## Initial Source

The first planned source is **Toonverse**, using HTTP endpoints such as:

```text
GET /api/series/slug/{slug}
GET /api/series/{seriesId}/chapters
```

The metadata response supplies the source-native series ID required by the chapter request. This dependency is handled in the Toonverse Java adapter, not encoded as a YAML workflow.

## Scope Boundaries

### In scope

- HTTP GET-based extraction;
- source-specific mutable YAML;
- metadata, chapter lists, and image-page URLs;
- source registries grouped by book kind and provider capability;
- canonical Java results;
- optional final JSON Schema validation;
- annotation-based public Meshingress tool classes;
- Spring auto-configuration for source adapter beans;
- execution-scoped authorization input with redaction.

### Out of scope for the MVP

- Playwright and DOM navigation;
- an n8n-style generic workflow engine;
- YAML-defined operation DAGs;
- arbitrary code or expression execution from YAML;
- per-node result persistence;
- a source-specific compiler framework;
- automatic source discovery from undocumented traffic;
- anti-bot bypassing;
- OCR or canvas reconstruction;
- downloading every image unless explicitly added as a later capability.

## Final Naming Model

`BookKind` describes the broad extraction contract:

```text
TEXT_BOOK
IMAGE_BOOK
```

`PublicationType` describes the work itself:

```text
NOVEL
LIGHT_NOVEL
MANGA
MANHWA
MANHUA
WEBTOON
COMIC
GRAPHIC_NOVEL
UNKNOWN
```

A Toonverse work may therefore be:

```text
bookKind = IMAGE_BOOK
publicationType = MANHWA
```

The book-level data is called **work metadata** or **series metadata**. The complete source response may also contain catalog state, moderation state, release metadata, and engagement metrics.

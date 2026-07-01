# Architect: Typed Structured Content Contract

## Entry Metadata

```json
{
  "id": "2026-06-29-structured-content-contract",
  "title": "Typed Structured Content Contract",
  "status": "pending",
  "createdAt": "2026-06-29T00:00:00-04:00",
  "updatedAt": "2026-06-29T00:00:00-04:00",
  "tags": [
    "tool-api",
    "dispatch-result",
    "structured-content",
    "frontend-contract",
    "schema",
    "result-model"
  ],
  "origin": {
    "source": "chat",
    "summary": "Decision to redefine DispatchExecutionResult.structuredContent as a typed schema-bound payload envelope backed by Java classes, with generic DispatchExecutionResult<S extends StructuredContent>."
  }
}
```

## Problem Statement

`structuredContent` currently behaves as a generic JSON payload slot. That is useful for flexibility, but it does not establish a reliable frontend contract. A tool can return video data, file data, web data, process data, or diagnostic data without any acknowledged layout that a page can map directly to a renderer.

The goal is to make `structuredContent` a stable, typed, schema-bound contract. Frontend consumers should be able to route by `schema` or `kind` and render the result without guessing the originating tool's private payload shape.

## Current State

Tools return `DispatchExecutionResult`. The result can include:

- `content`: human/model-facing result content.
- `structuredContent`: optional machine-readable JSON.
- `_meta`: execution metadata such as status, summary, error code, error message, and generated timestamp.

Current `DispatchExecutionResult` stores `structuredContent` as `JsonNode`, and `toJson()` emits it directly if present:

```java
private JsonNode structuredContent;

public DispatchExecutionResult setStructuredContent(JsonNode structuredContent) {
    this.structuredContent = Objects.requireNonNull(structuredContent, "structuredContent must not be null");
    return this;
}

if (structuredContent != null) {
    root.set("structuredContent", structuredContent);
}
```

This architect keeps that wire location but replaces the internal contract with typed classes.

## Core Decision

`structuredContent` becomes a serialized instance of a known Java structure kind.

Each structure kind is represented by a class that extends:

```java
StructuredContent
```

`DispatchExecutionResult` becomes generic:

```java
DispatchExecutionResult<S extends StructuredContent>
```

This makes the result type stable and reliable at tool implementation time, while still serializing into the same public JSON field:

```json
{
  "content": [],
  "structuredContent": {
    "kind": "media.video",
    "schema": "meshingress.media.video.v1",
    "version": 1,
    "data": {},
    "extra": {}
  },
  "_meta": {}
}
```

## Design Goals

- Make tool result structures frontend-consumable by default.
- Give each common result shape a named `kind` and `schema`.
- Use Java classes to define stable fields for each structure kind.
- Preserve an `extra` node for program/tool/vendor-specific additions.
- Keep `content` for human-readable summaries.
- Keep `_meta` for execution metadata.
- Avoid forcing every program-specific field into the generic schema.
- Preserve compatibility through migration helpers where practical.

## Non-Goals

- Do not define late-game niche schemas yet, such as recipes, automobile specs, hardware specs, or product-specific specs.
- Do not make frontend renderers depend on tool IDs.
- Do not let `extra` override standard fields.
- Do not replace `content` with `structuredContent`.
- Do not put execution state like `status`, `summary`, or `errorCode` inside the structure data object.

## Wire Format

Every typed `structuredContent` serializes into this envelope:

```json
{
  "kind": "media.video",
  "schema": "meshingress.media.video.v1",
  "version": 1,
  "data": {
    "id": "abc123",
    "title": "Example Video"
  },
  "extra": {
    "instagram": {
      "shortcode": "ABC123"
    }
  }
}
```

### Field Rules

| Field | Required | Meaning |
|---|---:|---|
| `kind` | yes | Generic content family, such as `media.video` or `file.archive`. |
| `schema` | yes | Stable renderer/schema identifier, such as `meshingress.media.video.v1`. |
| `version` | yes | Schema version for the structure contract. |
| `data` | yes | Standard fields defined by the Java structure class. |
| `extra` | no | Tool/vendor/program-specific data. |

### `extra` Rules

`extra` is allowed for source-specific data:

```json
{
  "extra": {
    "instagram": {
      "shortcode": "ABC123",
      "typename": "GraphVideo",
      "productType": "clips"
    },
    "ytDlp": {
      "formatId": "137",
      "extractor": "youtube"
    }
  }
}
```

`extra` must not be used to override standard fields:

```json
{
  "extra": {
    "title": "Do not override the standard title field here",
    "sources": []
  }
}
```

If a field becomes common across tools, promote it from `extra` into the formal structure class in a future schema version.

## Proposed Package Layout

```txt
dev.mrk.meshingress.api.result
├─ DispatchExecutionResult.java
├─ ResultContent.java
│
└─ structured
   ├─ StructuredContent.java
   ├─ StructuredContentEnvelope.java
   ├─ StructuredContentMapper.java
   ├─ StructuredContentKind.java
   ├─ StructuredContentSchemas.java
   │
   ├─ common
   │  ├─ LinkRef.java
   │  ├─ ImageRef.java
   │  ├─ FileRef.java
   │  ├─ PersonRef.java
   │  ├─ OriginRef.java
   │  ├─ LocationRef.java
   │  ├─ TimeRangeRef.java
   │  └─ MetricValue.java
   │
   ├─ text
   │  ├─ PlainTextContent.java
   │  ├─ MarkdownContent.java
   │  └─ HtmlTextContent.java
   │
   ├─ media
   │  ├─ VideoContent.java
   │  ├─ ImageContent.java
   │  ├─ AudioContent.java
   │  ├─ GalleryContent.java
   │  ├─ PlaylistContent.java
   │  ├─ CaptionContent.java
   │  ├─ TranscriptContent.java
   │  ├─ MediaSource.java
   │  └─ MediaStats.java
   │
   ├─ file
   │  ├─ GenericFileContent.java
   │  ├─ ArchiveContent.java
   │  ├─ DirectoryContent.java
   │  ├─ FileManifestContent.java
   │  ├─ FileUploadContent.java
   │  └─ FileDownloadContent.java
   │
   ├─ web
   │  ├─ WebPageContent.java
   │  ├─ WebHtmlContent.java
   │  ├─ WebLinkContent.java
   │  ├─ WebLinksContent.java
   │  ├─ WebMetadataContent.java
   │  ├─ WebScreenshotContent.java
   │  ├─ WebDomContent.java
   │  └─ WebElementContent.java
   │
   ├─ process
   │  ├─ ProcessExecutionContent.java
   │  ├─ ProcessStreamContent.java
   │  ├─ ProcessLogContent.java
   │  ├─ ProcessDiagnosticContent.java
   │  └─ ProcessStatusContent.java
   │
   ├─ data
   │  ├─ RecordContent.java
   │  ├─ RecordsContent.java
   │  ├─ TableContent.java
   │  ├─ TreeContent.java
   │  ├─ GraphContent.java
   │  ├─ TimelineContent.java
   │  └─ MetricsContent.java
   │
   ├─ search
   │  ├─ SearchResultsContent.java
   │  ├─ SearchResultContent.java
   │  ├─ RetrievalContextContent.java
   │  ├─ RetrievalMemoryContent.java
   │  └─ RetrievalCitationsContent.java
   │
   ├─ message
   │  ├─ EmailMessageContent.java
   │  ├─ ChatMessageContent.java
   │  ├─ NotificationContent.java
   │  ├─ MessageThreadContent.java
   │  └─ MessageDeliveryContent.java
   │
   ├─ identity
   │  ├─ UserIdentityContent.java
   │  ├─ AccountIdentityContent.java
   │  ├─ PrincipalIdentityContent.java
   │  ├─ SessionIdentityContent.java
   │  └─ AuthIdentityContent.java
   │
   ├─ tool
   │  ├─ ToolDescriptorContent.java
   │  ├─ ToolListContent.java
   │  ├─ ToolCallContent.java
   │  └─ ToolResultContent.java
   │
   ├─ runtime
   │  ├─ RuntimeStatusContent.java
   │  ├─ RuntimeHealthContent.java
   │  └─ RuntimeConfigContent.java
   │
   ├─ security
   │  ├─ SecurityScopeContent.java
   │  ├─ SecurityPolicyContent.java
   │  ├─ SecurityDecisionContent.java
   │  └─ SecurityApprovalContent.java
   │
   ├─ audit
   │  ├─ AuditRecordContent.java
   │  └─ AuditRecordsContent.java
   │
   ├─ calendar
   │  ├─ CalendarEventContent.java
   │  ├─ CalendarEventsContent.java
   │  ├─ ScheduleJobContent.java
   │  └─ ScheduleTriggerContent.java
   │
   ├─ location
   │  ├─ LocationPointContent.java
   │  ├─ LocationPlaceContent.java
   │  ├─ LocationRouteContent.java
   │  ├─ LocationMapContent.java
   │  └─ LocationRegionContent.java
   │
   ├─ network
   │  ├─ HttpRequestContent.java
   │  ├─ HttpResponseContent.java
   │  ├─ WebSocketEventContent.java
   │  ├─ ApiResponseContent.java
   │  ├─ ApiEndpointContent.java
   │  └─ ApiSchemaContent.java
   │
   └─ report
      ├─ DiagnosticResultContent.java
      ├─ DiagnosticIssueContent.java
      ├─ DiagnosticReportContent.java
      ├─ ReportSummaryContent.java
      ├─ ReportFindingsContent.java
      └─ ReportAssessmentContent.java
```

## Generic Structure Kinds

### MVP Kinds

These should be implemented first because they cover most early tools.

| Kind | Schema | Class |
|---|---|---|
| `text.plain` | `meshingress.text.plain.v1` | `PlainTextContent` |
| `text.markdown` | `meshingress.text.markdown.v1` | `MarkdownContent` |
| `text.html` | `meshingress.text.html.v1` | `HtmlTextContent` |
| `media.video` | `meshingress.media.video.v1` | `VideoContent` |
| `media.image` | `meshingress.media.image.v1` | `ImageContent` |
| `media.audio` | `meshingress.media.audio.v1` | `AudioContent` |
| `media.gallery` | `meshingress.media.gallery.v1` | `GalleryContent` |
| `file.generic` | `meshingress.file.generic.v1` | `GenericFileContent` |
| `file.archive` | `meshingress.file.archive.v1` | `ArchiveContent` |
| `file.directory` | `meshingress.file.directory.v1` | `DirectoryContent` |
| `file.manifest` | `meshingress.file.manifest.v1` | `FileManifestContent` |
| `web.page` | `meshingress.web.page.v1` | `WebPageContent` |
| `web.html` | `meshingress.web.html.v1` | `WebHtmlContent` |
| `web.link` | `meshingress.web.link.v1` | `WebLinkContent` |
| `web.metadata` | `meshingress.web.metadata.v1` | `WebMetadataContent` |
| `web.screenshot` | `meshingress.web.screenshot.v1` | `WebScreenshotContent` |
| `web.element` | `meshingress.web.element.v1` | `WebElementContent` |
| `process.execution` | `meshingress.process.execution.v1` | `ProcessExecutionContent` |
| `process.diagnostic` | `meshingress.process.diagnostic.v1` | `ProcessDiagnosticContent` |
| `data.record` | `meshingress.data.record.v1` | `RecordContent` |
| `data.records` | `meshingress.data.records.v1` | `RecordsContent` |
| `data.table` | `meshingress.data.table.v1` | `TableContent` |
| `data.tree` | `meshingress.data.tree.v1` | `TreeContent` |
| `data.timeline` | `meshingress.data.timeline.v1` | `TimelineContent` |
| `search.results` | `meshingress.search.results.v1` | `SearchResultsContent` |
| `retrieval.context` | `meshingress.retrieval.context.v1` | `RetrievalContextContent` |
| `message.notification` | `meshingress.message.notification.v1` | `NotificationContent` |
| `identity.user` | `meshingress.identity.user.v1` | `UserIdentityContent` |
| `identity.account` | `meshingress.identity.account.v1` | `AccountIdentityContent` |
| `tool.descriptor` | `meshingress.tool.descriptor.v1` | `ToolDescriptorContent` |
| `tool.list` | `meshingress.tool.list.v1` | `ToolListContent` |
| `runtime.health` | `meshingress.runtime.health.v1` | `RuntimeHealthContent` |
| `security.decision` | `meshingress.security.decision.v1` | `SecurityDecisionContent` |
| `audit.record` | `meshingress.audit.record.v1` | `AuditRecordContent` |
| `calendar.event` | `meshingress.calendar.event.v1` | `CalendarEventContent` |
| `calendar.events` | `meshingress.calendar.events.v1` | `CalendarEventsContent` |
| `network.http.response` | `meshingress.network.http.response.v1` | `HttpResponseContent` |
| `diagnostic.report` | `meshingress.diagnostic.report.v1` | `DiagnosticReportContent` |
| `report.summary` | `meshingress.report.summary.v1` | `ReportSummaryContent` |

### Post-MVP Generic Kinds

These are still generic, but can follow after the first renderer set exists.

| Kind | Schema | Class |
|---|---|---|
| `document.generic` | `meshingress.document.generic.v1` | `GenericDocumentContent` |
| `document.pdf` | `meshingress.document.pdf.v1` | `PdfDocumentContent` |
| `document.note` | `meshingress.document.note.v1` | `NoteDocumentContent` |
| `media.playlist` | `meshingress.media.playlist.v1` | `PlaylistContent` |
| `media.caption` | `meshingress.media.caption.v1` | `CaptionContent` |
| `media.transcript` | `meshingress.media.transcript.v1` | `TranscriptContent` |
| `file.upload` | `meshingress.file.upload.v1` | `FileUploadContent` |
| `file.download` | `meshingress.file.download.v1` | `FileDownloadContent` |
| `storage.location` | `meshingress.storage.location.v1` | `StorageLocationContent` |
| `web.links` | `meshingress.web.links.v1` | `WebLinksContent` |
| `web.dom` | `meshingress.web.dom.v1` | `WebDomContent` |
| `process.stdout` | `meshingress.process.stdout.v1` | `ProcessStreamContent` |
| `process.stderr` | `meshingress.process.stderr.v1` | `ProcessStreamContent` |
| `process.log` | `meshingress.process.log.v1` | `ProcessLogContent` |
| `process.status` | `meshingress.process.status.v1` | `ProcessStatusContent` |
| `data.graph` | `meshingress.data.graph.v1` | `GraphContent` |
| `data.metrics` | `meshingress.data.metrics.v1` | `MetricsContent` |
| `search.result` | `meshingress.search.result.v1` | `SearchResultContent` |
| `retrieval.memory` | `meshingress.retrieval.memory.v1` | `RetrievalMemoryContent` |
| `retrieval.citations` | `meshingress.retrieval.citations.v1` | `RetrievalCitationsContent` |
| `message.email` | `meshingress.message.email.v1` | `EmailMessageContent` |
| `message.chat` | `meshingress.message.chat.v1` | `ChatMessageContent` |
| `message.thread` | `meshingress.message.thread.v1` | `MessageThreadContent` |
| `message.delivery` | `meshingress.message.delivery.v1` | `MessageDeliveryContent` |
| `runtime.status` | `meshingress.runtime.status.v1` | `RuntimeStatusContent` |
| `runtime.config` | `meshingress.runtime.config.v1` | `RuntimeConfigContent` |
| `security.scope` | `meshingress.security.scope.v1` | `SecurityScopeContent` |
| `security.policy` | `meshingress.security.policy.v1` | `SecurityPolicyContent` |
| `security.approval` | `meshingress.security.approval.v1` | `SecurityApprovalContent` |
| `audit.records` | `meshingress.audit.records.v1` | `AuditRecordsContent` |
| `location.point` | `meshingress.location.point.v1` | `LocationPointContent` |
| `location.place` | `meshingress.location.place.v1` | `LocationPlaceContent` |
| `location.route` | `meshingress.location.route.v1` | `LocationRouteContent` |
| `location.map` | `meshingress.location.map.v1` | `LocationMapContent` |
| `location.region` | `meshingress.location.region.v1` | `LocationRegionContent` |
| `network.http.request` | `meshingress.network.http.request.v1` | `HttpRequestContent` |
| `network.websocket.event` | `meshingress.network.websocket.event.v1` | `WebSocketEventContent` |
| `api.response` | `meshingress.api.response.v1` | `ApiResponseContent` |
| `api.endpoint` | `meshingress.api.endpoint.v1` | `ApiEndpointContent` |
| `api.schema` | `meshingress.api.schema.v1` | `ApiSchemaContent` |
| `diagnostic.result` | `meshingress.diagnostic.result.v1` | `DiagnosticResultContent` |
| `diagnostic.issue` | `meshingress.diagnostic.issue.v1` | `DiagnosticIssueContent` |
| `report.findings` | `meshingress.report.findings.v1` | `ReportFindingsContent` |
| `report.assessment` | `meshingress.report.assessment.v1` | `ReportAssessmentContent` |

## Class Implementation

### `StructuredContent`

Use abstract methods for `kind`, `schema`, and `version` so subclasses define immutable schema identity.

```java
package dev.mrk.meshingress.api.result.structured;

import tools.jackson.databind.node.ObjectNode;

import java.util.Objects;
import java.util.Optional;

public abstract class StructuredContent {

    private ObjectNode extra;

    public abstract String kind();

    public abstract String schema();

    public abstract int version();

    public final Optional<ObjectNode> extra() {
        return Optional.ofNullable(extra);
    }

    public final StructuredContent extra(ObjectNode extra) {
        this.extra = Objects.requireNonNull(extra, "extra must not be null");
        return this;
    }

    public final boolean hasExtra() {
        return extra != null && !extra.isEmpty();
    }
}
```

### `StructuredContentMapper`

```java
package dev.mrk.meshingress.api.result.structured;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Objects;

public final class StructuredContentMapper {

    private static final List<String> RESERVED_FIELDS = List.of(
            "kind",
            "schema",
            "version",
            "extra"
    );

    private StructuredContentMapper() {
    }

    public static ObjectNode toJson(ObjectMapper objectMapper, StructuredContent content) {
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        Objects.requireNonNull(content, "content must not be null");

        ObjectNode root = objectMapper.createObjectNode();
        root.put("kind", content.kind());
        root.put("schema", content.schema());
        root.put("version", content.version());

        JsonNode dataNode = objectMapper.valueToTree(content);
        if (dataNode instanceof ObjectNode dataObject) {
            dataObject.remove(RESERVED_FIELDS);
            root.set("data", dataObject);
        } else {
            root.set("data", dataNode);
        }

        content.extra().ifPresent(extra -> {
            if (!extra.isEmpty()) {
                root.set("extra", extra);
            }
        });

        return root;
    }
}
```

### `StructuredContentKind`

```java
package dev.mrk.meshingress.api.result.structured;

public final class StructuredContentKind {

    private StructuredContentKind() {
    }

    public static final String TEXT_PLAIN = "text.plain";
    public static final String TEXT_MARKDOWN = "text.markdown";
    public static final String TEXT_HTML = "text.html";

    public static final String MEDIA_VIDEO = "media.video";
    public static final String MEDIA_IMAGE = "media.image";
    public static final String MEDIA_AUDIO = "media.audio";
    public static final String MEDIA_GALLERY = "media.gallery";

    public static final String FILE_GENERIC = "file.generic";
    public static final String FILE_ARCHIVE = "file.archive";
    public static final String FILE_DIRECTORY = "file.directory";
    public static final String FILE_MANIFEST = "file.manifest";

    public static final String WEB_PAGE = "web.page";
    public static final String WEB_HTML = "web.html";
    public static final String WEB_LINK = "web.link";
    public static final String WEB_METADATA = "web.metadata";
    public static final String WEB_SCREENSHOT = "web.screenshot";
    public static final String WEB_ELEMENT = "web.element";

    public static final String PROCESS_EXECUTION = "process.execution";
    public static final String PROCESS_DIAGNOSTIC = "process.diagnostic";

    public static final String DATA_RECORD = "data.record";
    public static final String DATA_RECORDS = "data.records";
    public static final String DATA_TABLE = "data.table";
    public static final String DATA_TREE = "data.tree";
    public static final String DATA_TIMELINE = "data.timeline";

    public static final String SEARCH_RESULTS = "search.results";
    public static final String RETRIEVAL_CONTEXT = "retrieval.context";

    public static final String MESSAGE_NOTIFICATION = "message.notification";

    public static final String IDENTITY_USER = "identity.user";
    public static final String IDENTITY_ACCOUNT = "identity.account";

    public static final String TOOL_DESCRIPTOR = "tool.descriptor";
    public static final String TOOL_LIST = "tool.list";

    public static final String RUNTIME_HEALTH = "runtime.health";

    public static final String SECURITY_DECISION = "security.decision";
    public static final String AUDIT_RECORD = "audit.record";

    public static final String CALENDAR_EVENT = "calendar.event";
    public static final String CALENDAR_EVENTS = "calendar.events";

    public static final String NETWORK_HTTP_RESPONSE = "network.http.response";

    public static final String DIAGNOSTIC_REPORT = "diagnostic.report";
    public static final String REPORT_SUMMARY = "report.summary";
}
```

### `StructuredContentSchemas`

```java
package dev.mrk.meshingress.api.result.structured;

public final class StructuredContentSchemas {

    private StructuredContentSchemas() {
    }

    public static final String MEDIA_VIDEO_V1 = "meshingress.media.video.v1";
    public static final String MEDIA_IMAGE_V1 = "meshingress.media.image.v1";
    public static final String MEDIA_AUDIO_V1 = "meshingress.media.audio.v1";
    public static final String FILE_ARCHIVE_V1 = "meshingress.file.archive.v1";
    public static final String WEB_PAGE_V1 = "meshingress.web.page.v1";
    public static final String PROCESS_EXECUTION_V1 = "meshingress.process.execution.v1";
    public static final String DATA_TABLE_V1 = "meshingress.data.table.v1";
    public static final String SEARCH_RESULTS_V1 = "meshingress.search.results.v1";
    public static final String RUNTIME_HEALTH_V1 = "meshingress.runtime.health.v1";
    public static final String DIAGNOSTIC_REPORT_V1 = "meshingress.diagnostic.report.v1";
}
```

## Generic `DispatchExecutionResult<S extends StructuredContent>`

### Target API

```java
package dev.mrk.meshingress.api.result;

import dev.mrk.meshingress.api.result.structured.StructuredContent;
import dev.mrk.meshingress.api.result.structured.StructuredContentMapper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DispatchExecutionResult<S extends StructuredContent> {

    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();

    private final List<ResultContent> content;
    private S structuredContent;
    private JsonNode legacyStructuredContent;
    private boolean error;
    private String status;
    private String summary;
    private String errorCode;
    private String errorMessage;
    private ObjectNode meta;

    private DispatchExecutionResult() {
        this.content = new ArrayList<>();
    }

    @Contract(value = " -> new", pure = true)
    public static @NonNull DispatchExecutionResult<StructuredContent> create() {
        return new DispatchExecutionResult<>();
    }

    @Contract(" -> new")
    public static @NonNull Builder<StructuredContent> builder() {
        return new Builder<>();
    }

    @Contract(" -> new")
    public static <S extends StructuredContent> @NonNull Builder<S> builder(Class<S> structuredType) {
        Objects.requireNonNull(structuredType, "structuredType must not be null");
        return new Builder<>();
    }

    public DispatchExecutionResult<S> appendContent(ResultContent content) {
        this.content.add(Objects.requireNonNull(content, "content must not be null"));
        return this;
    }

    public DispatchExecutionResult<S> setStructuredContent(S structuredContent) {
        this.structuredContent = Objects.requireNonNull(structuredContent, "structuredContent must not be null");
        this.legacyStructuredContent = null;
        return this;
    }

    /**
     * Compatibility escape hatch. Prefer setStructuredContent(S) for stable contracts.
     */
    @Deprecated(forRemoval = false)
    public DispatchExecutionResult<S> setStructuredContent(JsonNode structuredContent) {
        this.legacyStructuredContent = Objects.requireNonNull(structuredContent, "structuredContent must not be null");
        this.structuredContent = null;
        return this;
    }

    public Optional<S> structuredContent() {
        return Optional.ofNullable(structuredContent);
    }

    public Optional<JsonNode> legacyStructuredContent() {
        return Optional.ofNullable(legacyStructuredContent);
    }

    public @NonNull @Unmodifiable List<ResultContent> content() {
        return List.copyOf(content);
    }

    public ObjectNode toJson() {
        return toJson(DEFAULT_OBJECT_MAPPER);
    }

    public ObjectNode toJson(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");

        ObjectNode root = objectMapper.createObjectNode();

        ArrayNode contentArray = objectMapper.createArrayNode();
        for (ResultContent item : content) {
            contentArray.add(item.toJson(objectMapper));
        }
        root.set("content", contentArray);

        if (structuredContent != null) {
            root.set("structuredContent", StructuredContentMapper.toJson(objectMapper, structuredContent));
        } else if (legacyStructuredContent != null) {
            root.set("structuredContent", legacyStructuredContent);
        }

        if (error) {
            root.put("isError", true);
        }

        ObjectNode metaNode = buildMeta(objectMapper);
        if (!metaNode.isEmpty()) {
            root.set("_meta", metaNode);
        }

        return root;
    }

    private ObjectNode buildMeta(ObjectMapper objectMapper) {
        ObjectNode metaNode = meta == null ? objectMapper.createObjectNode() : meta.deepCopy();
        if (status != null && !status.isBlank()) { metaNode.put("status", status); }
        if (summary != null && !summary.isBlank()) { metaNode.put("summary", summary); }
        if (errorCode != null && !errorCode.isBlank()) { metaNode.put("errorCode", errorCode); }
        if (errorMessage != null && !errorMessage.isBlank()) { metaNode.put("errorMessage", errorMessage); }
        if (!metaNode.has("generatedAt")) { metaNode.put("generatedAt", Instant.now().toString()); }
        return metaNode;
    }

    public static final class Builder<S extends StructuredContent> {
        private final DispatchExecutionResult<S> result = new DispatchExecutionResult<>();

        private Builder() {
        }

        public Builder<S> appendContent(ResultContent content) {
            result.appendContent(content);
            return this;
        }

        public Builder<S> text(String value) {
            result.appendContent(ResultContent.text(value));
            return this;
        }

        public Builder<S> structuredContent(S structuredContent) {
            result.setStructuredContent(structuredContent);
            return this;
        }

        /**
         * Compatibility escape hatch. Prefer structuredContent(S).
         */
        @Deprecated(forRemoval = false)
        public Builder<S> structuredContent(JsonNode structuredContent) {
            result.setStructuredContent(structuredContent);
            return this;
        }

        public Builder<S> error(boolean error) {
            result.error = error;
            return this;
        }

        public Builder<S> error(String errorCode, String errorMessage) {
            result.error = true;
            result.errorCode = errorCode;
            result.errorMessage = errorMessage;
            return this;
        }

        public Builder<S> status(String status) {
            result.status = status;
            return this;
        }

        public Builder<S> summary(String summary) {
            result.summary = summary;
            return this;
        }

        public Builder<S> meta(ObjectNode meta) {
            result.meta = Objects.requireNonNull(meta, "meta must not be null");
            return this;
        }

        public DispatchExecutionResult<S> build() {
            return result;
        }
    }
}
```

### Tool Handler Compatibility

Existing handlers can continue returning a broad result:

```java
DispatchExecutionResult<? extends StructuredContent>
```

Recommended interface update:

```java
public interface McpToolHandler<S extends StructuredContent>
        extends McpDispatchHandler<DispatchExecutionResult<S>> {

    McpToolDescriptor descriptor();
}
```

Compatibility bridge option:

```java
public interface McpToolHandler
        extends McpDispatchHandler<DispatchExecutionResult<? extends StructuredContent>> {

    McpToolDescriptor descriptor();
}
```

Use the generic version for new code. Use the wildcard bridge if the migration touches too many existing call sites at once.

## Example: `VideoContent`

```java
package dev.mrk.meshingress.api.result.structured.media;

import dev.mrk.meshingress.api.result.structured.StructuredContent;
import dev.mrk.meshingress.api.result.structured.StructuredContentKind;
import dev.mrk.meshingress.api.result.structured.StructuredContentSchemas;
import dev.mrk.meshingress.api.result.structured.common.ImageRef;
import dev.mrk.meshingress.api.result.structured.common.OriginRef;
import dev.mrk.meshingress.api.result.structured.common.PersonRef;

import java.util.ArrayList;
import java.util.List;

public final class VideoContent extends StructuredContent {

    private String id;
    private String title;
    private String description;
    private Long durationMs;
    private Integer width;
    private Integer height;
    private List<MediaSource> sources = new ArrayList<>();
    private ImageRef poster;
    private List<ImageRef> thumbnails = new ArrayList<>();
    private PersonRef author;
    private OriginRef origin;
    private MediaStats stats;

    @Override
    public String kind() {
        return StructuredContentKind.MEDIA_VIDEO;
    }

    @Override
    public String schema() {
        return StructuredContentSchemas.MEDIA_VIDEO_V1;
    }

    @Override
    public int version() {
        return 1;
    }

    // getters/setters omitted for brevity
}
```

### Supporting Media Types

```java
public record MediaSource(
        String url,
        String mimeType,
        String quality,
        Integer width,
        Integer height,
        Long bitrate,
        Long sizeBytes
) {
}
```

```java
public record MediaStats(
        Long views,
        Long likes,
        Long comments,
        Long shares
) {
}
```

```java
public record ImageRef(
        String url,
        String mimeType,
        Integer width,
        Integer height,
        String alt
) {
}
```

```java
public record OriginRef(
        String platform,
        String url,
        String externalId
) {
}
```

```java
public record PersonRef(
        String id,
        String username,
        String displayName,
        String avatarUrl
) {
}
```

### Tool Usage

```java
public DispatchExecutionResult<VideoContent> fetchVideo(VideoArgs args, McpCallContext context) {
    VideoContent video = new VideoContent();
    video.setId("abc123");
    video.setTitle("Example Video");
    video.setDurationMs(154000L);
    video.setSources(List.of(
            new MediaSource(
                    "https://example.com/video.mp4",
                    "video/mp4",
                    "1080p",
                    1920,
                    1080,
                    null,
                    null
            )
    ));

    ObjectNode extra = objectMapper.createObjectNode();
    extra.putObject("instagram")
            .put("shortcode", "ABC123")
            .put("productType", "clips");
    video.extra(extra);

    return DispatchExecutionResult.builder(VideoContent.class)
            .text("Fetched video: Example Video")
            .structuredContent(video)
            .status("ok")
            .summary("Fetched 1 video")
            .build();
}
```

### Serialized Result

```json
{
  "content": [
    {
      "type": "text",
      "text": "Fetched video: Example Video"
    }
  ],
  "structuredContent": {
    "kind": "media.video",
    "schema": "meshingress.media.video.v1",
    "version": 1,
    "data": {
      "id": "abc123",
      "title": "Example Video",
      "durationMs": 154000,
      "sources": [
        {
          "url": "https://example.com/video.mp4",
          "mimeType": "video/mp4",
          "quality": "1080p",
          "width": 1920,
          "height": 1080
        }
      ]
    },
    "extra": {
      "instagram": {
        "shortcode": "ABC123",
        "productType": "clips"
      }
    }
  },
  "_meta": {
    "status": "ok",
    "summary": "Fetched 1 video",
    "generatedAt": "2026-06-29T00:00:00Z"
  }
}
```

## Example: `ProcessExecutionContent`

```java
public final class ProcessExecutionContent extends StructuredContent {

    private String command;
    private List<String> args = new ArrayList<>();
    private Integer exitCode;
    private String stdout;
    private String stderr;
    private Boolean timedOut;
    private Long durationMs;

    @Override
    public String kind() {
        return StructuredContentKind.PROCESS_EXECUTION;
    }

    @Override
    public String schema() {
        return "meshingress.process.execution.v1";
    }

    @Override
    public int version() {
        return 1;
    }

    // getters/setters omitted
}
```

## Example: `TableContent`

```java
public final class TableContent extends StructuredContent {

    private List<TableColumn> columns = new ArrayList<>();
    private List<ObjectNode> rows = new ArrayList<>();

    @Override
    public String kind() {
        return StructuredContentKind.DATA_TABLE;
    }

    @Override
    public String schema() {
        return StructuredContentSchemas.DATA_TABLE_V1;
    }

    @Override
    public int version() {
        return 1;
    }
}

public record TableColumn(
        String key,
        String label,
        String type,
        boolean sortable,
        boolean filterable
) {
}
```

## Frontend Routing Contract

Frontend renderers should route by `schema`, then fall back to `kind`, then fall back to a generic JSON renderer.

```ts
function renderStructuredContent(content: StructuredContentEnvelope) {
  switch (content.schema) {
    case "meshingress.media.video.v1":
      return <VideoResultCard data={content.data} extra={content.extra} />;

    case "meshingress.file.archive.v1":
      return <ArchiveResultCard data={content.data} extra={content.extra} />;

    case "meshingress.process.execution.v1":
      return <ProcessExecutionCard data={content.data} extra={content.extra} />;

    default:
      return <GenericStructuredContentViewer content={content} />;
  }
}
```

Renderers must ignore unknown `extra` nodes unless explicitly designed to read them.

## Migration Plan

### Phase 1: Add typed structured content API

- Add `StructuredContent` base class.
- Add `StructuredContentMapper`.
- Add `StructuredContentKind` and `StructuredContentSchemas` constants.
- Add MVP structure classes.
- Add `DispatchExecutionResult<S extends StructuredContent>`.
- Keep deprecated `structuredContent(JsonNode)` builder overload.

### Phase 2: Migrate first-party tools

- Convert media/Instagram tools to `DispatchExecutionResult<VideoContent>` or `DispatchExecutionResult<GalleryContent>`.
- Convert PowerShell/CLI tools to `DispatchExecutionResult<ProcessExecutionContent>`.
- Convert file tools to `ArchiveContent`, `DirectoryContent`, or `FileManifestContent`.
- Convert diagnostics/report tools to `DiagnosticReportContent`.

### Phase 3: Frontend renderer registry

- Add frontend registry keyed by `schema`.
- Add generic fallback JSON viewer.
- Add initial renderers for:
  - `media.video`
  - `media.image`
  - `file.archive`
  - `process.execution`
  - `data.table`
  - `diagnostic.report`

### Phase 4: Enforce schema discipline

- Add tests that serialize each structure kind and verify envelope shape.
- Add validation that `kind`, `schema`, and `version` are present.
- Add optional development warning when raw `JsonNode` structured content is used.
- Add documentation for tool authors.

## Acceptance Criteria

- `DispatchExecutionResult<S extends StructuredContent>` exists and supports typed structured content.
- Existing `content` and `_meta` behavior remains intact.
- `structuredContent` serializes as `{ kind, schema, version, data, extra? }`.
- At least these classes exist:
  - `StructuredContent`
  - `StructuredContentMapper`
  - `VideoContent`
  - `ImageContent`
  - `AudioContent`
  - `ArchiveContent`
  - `DirectoryContent`
  - `WebPageContent`
  - `ProcessExecutionContent`
  - `TableContent`
  - `DiagnosticReportContent`
- `extra` is supported and omitted when empty.
- Tool code can return `DispatchExecutionResult<VideoContent>` without casting.
- Frontend can route by `structuredContent.schema`.
- Legacy `JsonNode` structured content remains temporarily available but deprecated.

## Risks

### Generic result type causes broad call-site churn

`McpDispatchHandler` and `McpToolHandler` currently expect `DispatchExecutionResult`. Making the result generic may cause many signatures to change.

Mitigation:

```java
DispatchExecutionResult<? extends StructuredContent>
```

can be used at registry/dispatcher boundaries while individual tools use concrete generic types.

### `extra` becomes a dumping ground

Mitigation:

- Document rules.
- Namescape extra data by source/tool/vendor key.
- Promote common fields into formal schema versions.

### Schema constants drift from class implementations

Mitigation:

- Each class defines immutable `kind()`, `schema()`, and `version()`.
- Add unit tests per structure class.

### Frontend overfits to tool IDs

Mitigation:

- Frontend must route by `schema`, not tool ID.
- Tool-specific metadata belongs in `extra`.

## Verification Plan

Add unit tests for:

- `StructuredContentMapper.toJson()` envelope shape.
- `extra` omitted when absent.
- `extra` emitted when non-empty.
- Reserved fields not duplicated in `data`.
- `DispatchExecutionResult<VideoContent>.toJson()` emits typed structured content.
- Deprecated `structuredContent(JsonNode)` still serializes legacy JSON.
- `_meta.generatedAt` still auto-populates.

Example test assertion:

```java
ObjectNode json = DispatchExecutionResult.builder(VideoContent.class)
        .structuredContent(video)
        .text("Fetched video")
        .status("ok")
        .build()
        .toJson(objectMapper);

assertEquals("media.video", json.at("/structuredContent/kind").asText());
assertEquals("meshingress.media.video.v1", json.at("/structuredContent/schema").asText());
assertEquals("abc123", json.at("/structuredContent/data/id").asText());
assertTrue(json.has("_meta"));
```

## Documentation Updates

Update tool author docs to state:

- Prefer `DispatchExecutionResult<S extends StructuredContent>`.
- Pick the closest generic structure kind.
- Put tool-specific data under `extra`.
- Do not create niche schemas until a generic schema cannot represent the result cleanly.
- Do not return raw `ObjectNode` structured content for first-party tools unless no structure kind exists yet.

## Final Rule

`structuredContent` is a typed frontend contract.

Every stable structure kind gets a Java class. Every typed tool result should use:

```java
DispatchExecutionResult<S extends StructuredContent>
```

The frontend should render by `schema`, tolerate unknown `extra`, and fall back to generic JSON when no renderer exists.

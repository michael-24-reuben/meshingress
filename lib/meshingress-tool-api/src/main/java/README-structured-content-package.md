# Meshingress Structured Content Package

Package root:

```txt
dev.mrk.meshingress.api.result.structured
```

## Intent

This package defines typed `structuredContent` payload classes for Meshingress tool results. Each class serializes to the shared envelope:

```json
{
  "kind": "media.video",
  "schema": "meshingress.media.video.v1",
  "version": 1,
  "data": {},
  "extra": {}
}
```

`data` is generated from the concrete Java content class. `extra` is optional and reserved for source/tool/vendor-specific metadata.

## Dispatch integration

Add a builder overload in `DispatchExecutionResult.Builder`:

```java
public Builder structuredContent(StructuredContent content) {
    result.setStructuredContent(StructuredContentMapper.toJson(DEFAULT_OBJECT_MAPPER, content));
    return this;
}
```

Keep the existing `structuredContent(JsonNode)` overload as a raw escape hatch.

## Example

```java
VideoContent video = new VideoContent();
video.setId("abc123");
video.setTitle("Video title");
video.setSources(List.of(new MediaSource("https://example.com/video.mp4", "video/mp4", "1080p")));

ObjectNode extra = objectMapper.createObjectNode();
extra.putObject("instagram").put("shortcode", "ABC123");
video.extra(extra);

return DispatchExecutionResult.builder()
        .text("Fetched video: Video title")
        .structuredContent(video)
        .status("ok")
        .summary("Fetched 1 video")
        .build();
```

## Rule

Do not put standard fields in `extra`. Promote repeated vendor fields into the formal schema in a future version.

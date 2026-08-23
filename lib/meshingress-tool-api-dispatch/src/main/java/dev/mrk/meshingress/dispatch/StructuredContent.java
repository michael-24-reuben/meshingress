package dev.mrk.meshingress.dispatch;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Objects;

/**
 * Base type for schema-bound structured tool output.
 * <p>
 * Implementations represent stable frontend-consumable content kinds such as
 * media.video, file.archive, process.execution, or web.page. The common wire
 * shape is produced by {@link StructuredContentMapper}:
 *
 * <pre>
 * {
 *   "kind": "media.video",
 *   "schema": "meshingress.media.video.v1",
 *   "version": 1,
 *   "data": { ... },
 *   "extra": { ... }
 * }
 * </pre>
 *
 * {@code extra} is reserved for source/tool/vendor-specific data. It must not
 * override or replace standard fields in the formal schema.
 */
public abstract class StructuredContent {

    private final StructuredContentKind kind;

    protected StructuredContent(StructuredContentKind kind) {
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
    }

    public final String kind() {
        return kind.value();
    }

    public final String schema() {
        return kind.schema();
    }

    public final int version() {
        return kind.version();
    }

    public final StructuredContentKind contentKind() {
        return kind;
    }

    /**
     * Produces the value placed beneath the structured-content envelope's {@code data} member.
     * <p>
     * Ordinary typed content is serialized from its public properties. Content whose payload is
     * already JSON may override this method so that the payload itself, rather than a wrapper
     * object containing it, becomes {@code data}.
     */
    public JsonNode data(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");

        JsonNode data = objectMapper.valueToTree(this);
        if (data instanceof ObjectNode dataObject) {
            dataObject.remove("kind");
            dataObject.remove("schema");
            dataObject.remove("version");
        }
        return data;
    }
}

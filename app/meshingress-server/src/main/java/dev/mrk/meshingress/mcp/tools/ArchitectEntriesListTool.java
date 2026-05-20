package dev.mrk.meshingress.mcp.tools;

import dev.mrk.meshingress.api.tools.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import dev.mrk.meshingress.api.McpCallContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class ArchitectEntriesListTool implements McpToolHandler {

    private static final List<String> STATUSES = List.of("pending", "active", "blocked", "resolved", "archived");

    private final ObjectMapper objectMapper;
    private final Path architectRoot;

    public ArchitectEntriesListTool(
            ObjectMapper objectMapper,
            @Value("${meshingress.architect.root:architect}") String architectRoot
    ) {
        this.objectMapper = objectMapper;
        this.architectRoot = Path.of(architectRoot).normalize();
    }

    @Override
    public McpToolDescriptor descriptor() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = objectMapper.createObjectNode();

        ObjectNode status = objectMapper.createObjectNode();
        status.put("type", "string");
        ArrayNode statusEnum = objectMapper.createArrayNode();
        STATUSES.forEach(statusEnum::add);
        status.set("enum", statusEnum);
        properties.set("status", status);

        ObjectNode tag = objectMapper.createObjectNode();
        tag.put("type", "string");
        properties.set("tag", tag);

        ObjectNode query = objectMapper.createObjectNode();
        query.put("type", "string");
        properties.set("query", query);

        schema.set("properties", properties);
        schema.put("additionalProperties", false);

        ObjectNode annotations = objectMapper.createObjectNode();
        annotations.put("readOnlyHint", true);
        annotations.put("destructiveHint", false);
        annotations.put("idempotentHint", true);

        return new McpToolDescriptor(
                "architect.entries.list",
                "List Architect Entries",
                "List structured engineering memory entries by status, tag, or text query.",
                1,
                true,
                ToolVisibility.PUBLIC,
                "architect.entries.list",
                schema,
                null,
                annotations,
                false
        );
    }

    @Override
    public ToolExecutionResult call(ObjectNode arguments, McpCallContext context) {
        String statusFilter = arguments.path("status").asString("");
        String tagFilter = arguments.path("tag").asString("");
        String queryFilter = arguments.path("query").asString("").toLowerCase();

        ArrayNode entries = objectMapper.createArrayNode();
        for (String status : STATUSES) {
            if (!statusFilter.isBlank() && !statusFilter.equals(status)) {
                continue;
            }
            collectStatusEntries(architectRoot.resolve(status), status, tagFilter, queryFilter, entries);
        }

        ObjectNode structured = objectMapper.createObjectNode();
        structured.set("entries", entries);
        return ToolExecutionResult.text(
                objectMapper,
                "Found " + entries.size() + " architect entr" + (entries.size() == 1 ? "y." : "ies."),
                structured
        );
    }

    private void collectStatusEntries(Path statusPath, String fallbackStatus, String tagFilter, String queryFilter, ArrayNode entries) {
        if (!Files.isDirectory(statusPath)) {
            return;
        }
        try (var children = Files.list(statusPath)) {
            children
                    .filter(Files::isDirectory)
                    .sorted()
                    .forEach(entryPath -> addEntry(entryPath, fallbackStatus, tagFilter, queryFilter, entries));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read architect entries: " + exception.getMessage(), exception);
        }
    }

    private void addEntry(Path entryPath, String fallbackStatus, String tagFilter, String queryFilter, ArrayNode entries) {
        ObjectNode entry = objectMapper.createObjectNode();
        entry.put("id", entryPath.getFileName().toString());
        entry.put("status", fallbackStatus);
        entry.put("path", entryPath.toString());

        Path metaPath = entryPath.resolve("meta.json");
        if (Files.isRegularFile(metaPath)) {
            try {
                JsonNode meta = objectMapper.readTree(Files.readString(metaPath));
                entry.put("id", meta.path("id").asString(entry.path("id").asString()));
                entry.put("title", meta.path("title").asString(entry.path("id").asString()));
                entry.put("status", meta.path("status").asString(fallbackStatus));
                entry.set("tags", meta.path("tags").isArray() ? meta.path("tags") : objectMapper.createArrayNode());
            } catch (IOException exception) {
                entry.put("title", entry.path("id").asString());
                entry.put("metaError", exception.getMessage());
            }
        } else {
            entry.put("title", entry.path("id").asString());
            entry.set("tags", objectMapper.createArrayNode());
        }

        if (!tagFilter.isBlank() && !hasTag(entry.path("tags"), tagFilter)) {
            return;
        }
        if (!queryFilter.isBlank() && !entry.toString().toLowerCase().contains(queryFilter)) {
            return;
        }
        entries.add(entry);
    }

    private boolean hasTag(JsonNode tags, String tagFilter) {
        for (JsonNode tag : tags) {
            if (tagFilter.equals(tag.asString())) {
                return true;
            }
        }
        return false;
    }
}

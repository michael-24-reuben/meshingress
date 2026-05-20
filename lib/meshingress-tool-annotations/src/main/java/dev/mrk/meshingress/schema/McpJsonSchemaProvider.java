package dev.mrk.meshingress.schema;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public interface McpJsonSchemaProvider {

    ObjectNode schema(ObjectMapper objectMapper);
}

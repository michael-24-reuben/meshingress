package dev.mrk.meshingress.documentation.dispatch;

import dev.mrk.meshingress.documentation.MeshingressDocumentationProperties;
import dev.mrk.meshingress.config.MeshingressProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DispatchDocumentationBuilder {

    private final DispatchAnnotationScanner scanner;
    private final MeshingressDocumentationProperties properties;
    private final MeshingressProperties meshingressProperties;

    public DispatchDocumentationBuilder(
            DispatchAnnotationScanner scanner,
            MeshingressDocumentationProperties properties,
            MeshingressProperties meshingressProperties
    ) {
        this.scanner = scanner;
        this.properties = properties;
        this.meshingressProperties = meshingressProperties;
    }

    public Map<String, Object> build() {
        List<DispatchMethodDescriptor> methods = scanner.scan();
        Set<Class<?>> paramTypes = methods.stream()
                .map(DispatchMethodDescriptor::paramsType)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("openapi", "3.1.0");
        root.put("info", infoSection());
        root.put("servers", List.of(serverSection()));
        root.put("tags", tagsSection());
        root.put("x-dispatch-map", dispatchMapSection(methods));
        root.put("paths", pathsSection(methods));
        root.put("components", componentsSection(methods, paramTypes));
        return root;
    }

    private Map<String, Object> infoSection() {
        return mapOf(
                "title", properties.dispatch().title(),
                "version", properties.dispatch().version(),
                "description", "Reflection-scraped MCP dispatch methods discovered from @McpDispatchMapping classes."
        );
    }

    private Map<String, Object> serverSection() {
        return mapOf(
                "url", meshingressProperties.identity().publicBaseUrl().toString(),
                "description", "Generated server url"
        );
    }

    private List<Map<String, Object>> tagsSection() {
        return List.of(
                mapOf("name", "Internal MCP", "description", "Core MCP lifecycle and transport control methods."),
                mapOf("name", "Roles MCP", "description", "Role-gated registry and publication management methods."),
                mapOf("name", "Storage MCP", "description", "MCP workspace publication state lookup methods.")
        );
    }

    private Map<String, Object> dispatchMapSection(List<DispatchMethodDescriptor> methods) {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (DispatchMethodDescriptor method : methods) {
            entries.add(mapOf(
                    "method", method.method(),
                    "className", method.className(),
                    "javaMethod", method.javaMethod(),
                    "paramsType", method.paramsType().getName(),
                    "returnType", method.returnType().getName()
            ));
        }
        return mapOf(
                "targetModules", properties.dispatch().targetModules(),
                "scanBasePackages", properties.dispatch().scanBasePackages(),
                "methods", entries
        );
    }

    private Map<String, Object> pathsSection(List<DispatchMethodDescriptor> methods) {
        List<Map<String, Object>> oneOf = methods.stream()
                .map(method -> mapOf("$ref", "#/components/schemas/" + requestSchemaName(method.method())))
                .toList();

        Map<String, Object> post = mapOf(
                "tags", List.of("mcp-controller"),
                "summary", "Dispatch annotated MCP JSON-RPC requests",
                "description", "JSON-RPC 2.0 requests routed through methods discovered by @McpDispatchMapping and @McpDispatchMethod.",
                "operationId", "postDispatchAnnotatedMethods",
                "parameters", standardHeaders(),
                "requestBody", mapOf(
                        "required", true,
                        "content", mapOf(
                                "application/json", mapOf(
                                        "schema", mapOf("oneOf", oneOf)
                                )
                        )
                ),
                "responses", mapOf(
                        "200", mapOf(
                                "description", "JSON-RPC response object or response batch.",
                                "content", mapOf(
                                        "application/json", mapOf(
                                                "schema", mapOf(
                                                        "type", "object",
                                                        "additionalProperties", true
                                                )
                                        )
                                )
                        ),
                        "204", mapOf("description", "Notification-only request produced no response body.")
                )
        );
        return mapOf("/mcp", mapOf("post", post));
    }

    private List<Map<String, Object>> standardHeaders() {
        return List.of(
                parameter("Authorization", "Bearer dev-admin"),
                parameter("X-Mcp-Role", "admin"),
                parameter("X-Mcp-Admin", "true"),
                parameter("Mcp-Session-Id", "session-123"),
                parameter("X-Request-Id", "req-123")
        );
    }

    private Map<String, Object> parameter(String name, String example) {
        return mapOf(
                "name", name,
                "in", "header",
                "required", false,
                "schema", mapOf("type", "string"),
                "example", example
        );
    }

    private Map<String, Object> componentsSection(List<DispatchMethodDescriptor> methods, Set<Class<?>> paramTypes) {
        Map<String, Object> schemas = new LinkedHashMap<>();
        for (DispatchMethodDescriptor method : methods) {
            schemas.put(requestSchemaName(method.method()), requestSchema(method));
        }
        for (Class<?> paramType : paramTypes) {
            if (isInlineSchema(paramType)) {
                continue;
            }
            schemas.putIfAbsent(typeSchemaName(paramType), schemaForType(paramType));
        }
        return mapOf("schemas", schemas);
    }

    private Map<String, Object> requestSchema(DispatchMethodDescriptor descriptor) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("jsonrpc", mapOf("type", "string", "enum", List.of("2.0"), "example", "2.0"));
        properties.put("id", mapOf("description", "Client-supplied request identifier.", "example", 1));
        properties.put("method", mapOf("type", "string", "enum", List.of(descriptor.method()), "example", descriptor.method()));
        if (isInlineSchema(descriptor.paramsType())) {
            properties.put("params", mapOf("type", "object", "additionalProperties", true));
        } else {
            properties.put("params", mapOf("$ref", "#/components/schemas/" + typeSchemaName(descriptor.paramsType())));
        }

        List<String> required = new ArrayList<>(List.of("jsonrpc", "id", "method"));
        if (!isInlineSchema(descriptor.paramsType())) {
            required.add("params");
        }
        return mapOf(
                "type", "object",
                "description", "MCP request for " + descriptor.method() + ".",
                "properties", properties,
                "required", required,
                "x-dispatch-class", descriptor.className(),
                "x-dispatch-java-method", descriptor.javaMethod()
        );
    }

    private Map<String, Object> schemaForType(Class<?> type) {
        if (type == null || Object.class.equals(type) || JsonNode.class.isAssignableFrom(type)) {
            return mapOf("type", "object", "additionalProperties", true);
        }
        if (String.class.equals(type)) {
            return mapOf("type", "string");
        }
        if (Integer.class.equals(type) || int.class.equals(type)) {
            return mapOf("type", "integer", "format", "int32");
        }
        if (Long.class.equals(type) || long.class.equals(type)) {
            return mapOf("type", "integer", "format", "int64");
        }
        if (Double.class.equals(type) || double.class.equals(type)) {
            return mapOf("type", "number", "format", "double");
        }
        if (Float.class.equals(type) || float.class.equals(type)) {
            return mapOf("type", "number", "format", "float");
        }
        if (Boolean.class.equals(type) || boolean.class.equals(type)) {
            return mapOf("type", "boolean");
        }
        if (isDateTime(type)) {
            return mapOf("type", "string", "format", "date-time");
        }
        if (type.isEnum()) {
            List<String> values = List.of(type.getEnumConstants()).stream()
                    .map(Object::toString)
                    .toList();
            return mapOf("type", "string", "enum", values);
        }
        if (type.isRecord()) {
            return recordSchema(type);
        }
        return mapOf("type", "object", "additionalProperties", true);
    }

    private Map<String, Object> recordSchema(Class<?> type) {
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        for (RecordComponent component : type.getRecordComponents()) {
            properties.put(component.getName(), schemaForType(component.getType()));
            if (component.getType().isPrimitive()) {
                required.add(component.getName());
            }
        }
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("description", "Schema for " + type.getName() + ".");
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    private boolean isInlineSchema(Class<?> type) {
        return type == null || Object.class.equals(type) || JsonNode.class.isAssignableFrom(type);
    }

    private boolean isDateTime(Class<?> type) {
        return OffsetDateTime.class.equals(type)
                || Instant.class.equals(type)
                || LocalDateTime.class.equals(type)
                || ZonedDateTime.class.equals(type);
    }

    private String requestSchemaName(String method) {
        return "Mcp" + toPascal(method) + "Request";
    }

    private String typeSchemaName(Class<?> type) {
        String simple = type.getSimpleName();
        return simple.isBlank() ? "AnonymousDispatchType" : simple;
    }

    private String toPascal(String value) {
        String cleaned = value.replaceAll("[^a-zA-Z0-9]+", " ").trim();
        if (cleaned.isEmpty()) {
            return "DispatchMethod";
        }
        StringBuilder builder = new StringBuilder();
        for (String token : cleaned.split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) {
                builder.append(token.substring(1));
            }
        }
        return builder.toString();
    }

    private Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put((String) entries[i], entries[i + 1]);
        }
        return map;
    }
}

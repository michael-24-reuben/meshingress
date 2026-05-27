package dev.mrk.meshingress.api.tools.annotation.introspect;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;

import java.lang.annotation.Annotation;
import java.util.List;

public final class AnnotationCatalogBuilder {

    private final ObjectMapper objectMapper;
    private final AnnotationSchemaIntrospector introspector;

    public AnnotationCatalogBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.introspector = new AnnotationSchemaIntrospector(objectMapper);
    }

    public ArrayNode describeAnnotations(List<Class<? extends Annotation>> annotationTypes) {
        ArrayNode array = objectMapper.createArrayNode();

        for (Class<? extends Annotation> annotationType : annotationTypes) {
            array.add(introspector.describeAnnotation(annotationType));
        }

        return array;
    }
}
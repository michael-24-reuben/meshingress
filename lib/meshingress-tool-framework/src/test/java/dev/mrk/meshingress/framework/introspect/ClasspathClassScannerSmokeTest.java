package dev.mrk.meshingress.framework.introspect;

import dev.mrk.meshingress.api.tools.annotation.*;
import dev.mrk.meshingress.api.tools.annotation.introspect.AnnotationCatalogBuilder;
import dev.mrk.meshingress.tools.framework.scanning.ClasspathClassScanner;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.stream.Collectors;

public class ClasspathClassScannerSmokeTest {
    static void main() {
        ClasspathClassScanner.ScanOptions options = ClasspathClassScanner.ScanOptions.builder()
                .includeKinds(ClasspathClassScanner.ClassKind.ANNOTATION)
                .build();

        List<Class<?>> classes = ClasspathClassScanner.classesInPackageOf(McpTool.class, options);

        List<Class<? extends Annotation>> annotationClasses = classes.stream()
                .filter(Class::isAnnotation)
                .map(ClasspathClassScannerSmokeTest::asAnnotationClass)
                .collect(Collectors.toList());

        AnnotationCatalogBuilder catalogBuilder = new AnnotationCatalogBuilder(new ObjectMapper());
        ArrayNode annotations = catalogBuilder.describeAnnotations(annotationClasses);

        System.out.println(annotations.toPrettyString());
    }

    private static Class<? extends Annotation> asAnnotationClass(Class<?> type) {
        if (!type.isAnnotation()) {
            throw new IllegalArgumentException("Class is not an annotation type: " + type.getName());
        }

        return type.asSubclass(Annotation.class);
    }
}

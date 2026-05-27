package dev.mrk.meshingress.tools.framework.scanning;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Predicate;

public final class ClasspathClassScanner {

    private ClasspathClassScanner() {
    }

    public enum ClassKind {
        CONCRETE_CLASS,
        ABSTRACT_CLASS,
        INTERFACE,
        ANNOTATION,
        ENUM,
        RECORD
    }

    public static List<Class<?>> classesInPackageOf(Class<?> anchorClass) {
        return classesInPackageOf(anchorClass, ScanOptions.defaults());
    }

    public static List<Class<?>> classesInPackageOf(Class<?> anchorClass, ScanOptions options) {
        Objects.requireNonNull(anchorClass, "anchorClass must not be null");
        return classesInPackage(anchorClass.getPackageName(), options);
    }

    public static List<Class<?>> classesInPackage(String basePackage) {
        return classesInPackage(basePackage, ScanOptions.defaults());
    }

    public static List<Class<?>> classesInPackage(String basePackage, ScanOptions options) {
        Objects.requireNonNull(basePackage, "basePackage must not be null");
        Objects.requireNonNull(options, "options must not be null");

        ClassLoader classLoader = options.classLoader() == null
                ? ClassUtils.getDefaultClassLoader()
                : options.classLoader();

        ClassPathScanningCandidateComponentProvider scanner = getCandidateComponentProvider(options, classLoader);

        List<Class<?>> result = new ArrayList<>();

        for (BeanDefinition beanDefinition : scanner.findCandidateComponents(basePackage)) {
            String className = beanDefinition.getBeanClassName();

            if (className == null || className.isBlank()) {
                continue;
            }

            Class<?> candidate = loadClass(className, classLoader, options);

            if (candidate == null) {
                continue;
            }

            if (options.matches(candidate)) {
                result.add(candidate);
            }
        }

        return List.copyOf(result);
    }

    private static @NonNull ClassPathScanningCandidateComponentProvider getCandidateComponentProvider(ScanOptions options, ClassLoader classLoader) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                        AnnotationMetadata metadata = beanDefinition.getMetadata();

                        if (!options.includeNestedClasses() && !metadata.isIndependent()) {
                            return false;
                        }

                        // Critical: allow interfaces, annotations, and abstract classes.
                        return true;
                    }
                };

        scanner.setResourceLoader(new org.springframework.core.io.DefaultResourceLoader(classLoader));
        scanner.addIncludeFilter(new MatchEverythingFilter());
        return scanner;
    }

    private static Class<?> loadClass(String className, ClassLoader classLoader, ScanOptions options) {
        try {
            if (options.initializeClasses()) {
                return Class.forName(className, true, classLoader);
            }

            return ClassUtils.forName(className, classLoader);
        } catch (ClassNotFoundException | LinkageError e) {
            if (options.failOnClassLoadError()) {
                throw new IllegalStateException("Could not load class: " + className, e);
            }

            return null;
        }
    }

    private static final class MatchEverythingFilter implements TypeFilter {
        @Override
        public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
            return true;
        }
    }

    public record ScanOptions(
            Set<ClassKind> includedKinds,
            Set<Class<? extends Annotation>> annotatedWith,
            Set<Class<?>> assignableTo,
            List<Predicate<Class<?>>> predicates,
            boolean includeNestedClasses,
            boolean initializeClasses,
            boolean failOnClassLoadError,
            ClassLoader classLoader
    ) {
        public ScanOptions {
            includedKinds = Set.copyOf(includedKinds);
            annotatedWith = Set.copyOf(annotatedWith);
            assignableTo = Set.copyOf(assignableTo);
            predicates = List.copyOf(predicates);
        }

        public static ScanOptions defaults() {
            return builder().build();
        }

        public static Builder builder() {
            return new Builder();
        }

        private boolean matches(Class<?> candidate) {
            return matchesKind(candidate)
                    && matchesAnnotations(candidate)
                    && matchesAssignableTypes(candidate)
                    && matchesPredicates(candidate);
        }

        private boolean matchesKind(Class<?> candidate) {
            if (includedKinds.isEmpty()) {
                return true;
            }

            for (ClassKind kind : includedKinds) {
                if (matchesKind(candidate, kind)) {
                    return true;
                }
            }

            return false;
        }

        private static boolean matchesKind(Class<?> candidate, ClassKind kind) {
            return switch (kind) {
                case CONCRETE_CLASS -> !candidate.isInterface()
                        && !candidate.isAnnotation()
                        && !candidate.isEnum()
                        && !candidate.isRecord()
                        && !Modifier.isAbstract(candidate.getModifiers());

                case ABSTRACT_CLASS -> !candidate.isInterface()
                        && !candidate.isAnnotation()
                        && Modifier.isAbstract(candidate.getModifiers());

                case INTERFACE -> candidate.isInterface() && !candidate.isAnnotation();

                case ANNOTATION -> candidate.isAnnotation();

                case ENUM -> candidate.isEnum();

                case RECORD -> candidate.isRecord();
            };
        }

        private boolean matchesAnnotations(Class<?> candidate) {
            for (Class<? extends Annotation> annotationType : annotatedWith) {
                if (!candidate.isAnnotationPresent(annotationType)) {
                    return false;
                }
            }

            return true;
        }

        private boolean matchesAssignableTypes(Class<?> candidate) {
            for (Class<?> type : assignableTo) {
                if (!type.isAssignableFrom(candidate)) {
                    return false;
                }
            }

            return true;
        }

        private boolean matchesPredicates(Class<?> candidate) {
            for (Predicate<Class<?>> predicate : predicates) {
                if (!predicate.test(candidate)) {
                    return false;
                }
            }

            return true;
        }

        public static final class Builder {
            private final Set<ClassKind> includedKinds = EnumSet.of(ClassKind.CONCRETE_CLASS);
            private final Set<Class<? extends Annotation>> annotatedWith = new LinkedHashSet<>();
            private final Set<Class<?>> assignableTo = new LinkedHashSet<>();
            private final List<Predicate<Class<?>>> predicates = new ArrayList<>();

            private boolean includeNestedClasses;
            private boolean initializeClasses;
            private boolean failOnClassLoadError = true;
            private ClassLoader classLoader;

            private Builder() {
            }

            public Builder includeKinds(ClassKind first, ClassKind... rest) {
                Objects.requireNonNull(first, "first kind must not be null");

                includedKinds.clear();
                includedKinds.add(first);

                if (rest != null) {
                    for (ClassKind kind : rest) {
                        includedKinds.add(Objects.requireNonNull(kind, "kind must not be null"));
                    }
                }

                return this;
            }

            public Builder includeAllKinds() {
                includedKinds.clear();
                includedKinds.addAll(EnumSet.allOf(ClassKind.class));
                return this;
            }

            public Builder includeConcreteClasses() {
                includedKinds.add(ClassKind.CONCRETE_CLASS);
                return this;
            }

            public Builder includeAbstractClasses() {
                includedKinds.add(ClassKind.ABSTRACT_CLASS);
                return this;
            }

            public Builder includeInterfaces() {
                includedKinds.add(ClassKind.INTERFACE);
                return this;
            }

            public Builder includeAnnotationTypes() {
                includedKinds.add(ClassKind.ANNOTATION);
                return this;
            }

            public Builder includeEnums() {
                includedKinds.add(ClassKind.ENUM);
                return this;
            }

            public Builder includeRecords() {
                includedKinds.add(ClassKind.RECORD);
                return this;
            }

            public Builder annotatedWith(Class<? extends Annotation> annotationType) {
                annotatedWith.add(Objects.requireNonNull(annotationType, "annotationType must not be null"));
                return this;
            }

            public Builder assignableTo(Class<?> type) {
                assignableTo.add(Objects.requireNonNull(type, "type must not be null"));
                return this;
            }

            public Builder matching(Predicate<Class<?>> predicate) {
                predicates.add(Objects.requireNonNull(predicate, "predicate must not be null"));
                return this;
            }

            public Builder includeNestedClasses(boolean includeNestedClasses) {
                this.includeNestedClasses = includeNestedClasses;
                return this;
            }

            public Builder initializeClasses(boolean initializeClasses) {
                this.initializeClasses = initializeClasses;
                return this;
            }

            public Builder failOnClassLoadError(boolean failOnClassLoadError) {
                this.failOnClassLoadError = failOnClassLoadError;
                return this;
            }

            public Builder classLoader(ClassLoader classLoader) {
                this.classLoader = classLoader;
                return this;
            }

            public ScanOptions build() {
                return new ScanOptions(
                        includedKinds,
                        annotatedWith,
                        assignableTo,
                        predicates,
                        includeNestedClasses,
                        initializeClasses,
                        failOnClassLoadError,
                        classLoader
                );
            }
        }
    }
}
package dev.mrk.meshingress.runtime.spring;

import dev.mrk.meshingress.runtime.artifacts.ResolvedToolArtifact;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class SpringToolModuleApplicationContextFactory implements ToolModuleApplicationContextFactory {

    @Override
    public AnnotationConfigApplicationContext create(
            ResolvedToolArtifact artifact,
            ClassLoader classLoader,
            ApplicationContext parentContext
    ) {
        return create(artifact, classLoader, parentContext, Map.of());
    }

    @Override
    public AnnotationConfigApplicationContext create(
            ResolvedToolArtifact artifact,
            ClassLoader classLoader,
            ApplicationContext parentContext,
            Map<String, String> runtimeProperties
    ) {
        List<String> autoConfigurationClasses = AutoConfigurationImports.read(classLoader);
        if (autoConfigurationClasses.isEmpty()) {
            throw new IllegalStateException("Tool module has no Spring auto-configuration imports: " + artifact.mainJar());
        }

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.setClassLoader(classLoader);
        context.setDisplayName("meshingress-tool-module:" + artifact.moduleId().value());
        if (parentContext != null) {
            context.setParent(parentContext);
        }
        if (runtimeProperties != null && !runtimeProperties.isEmpty()) {
            context.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("meshingress-tool-provisioning", new LinkedHashMap<>(runtimeProperties))
            );
        }
        for (String className : autoConfigurationClasses) {
            context.register(loadClass(classLoader, className));
        }
        context.refresh();
        return context;
    }

    private Class<?> loadClass(ClassLoader classLoader, String className) {
        try {
            return Class.forName(className, true, classLoader);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Tool module auto-configuration class is missing: " + className, exception);
        }
    }
}

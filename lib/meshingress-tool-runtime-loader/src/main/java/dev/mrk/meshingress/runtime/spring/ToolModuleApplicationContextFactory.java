package dev.mrk.meshingress.runtime.spring;

import dev.mrk.meshingress.runtime.artifacts.ResolvedToolArtifact;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

public interface ToolModuleApplicationContextFactory {
    ConfigurableApplicationContext create(
            ResolvedToolArtifact artifact,
            ClassLoader classLoader,
            ApplicationContext parentContext
    );

    default ConfigurableApplicationContext create(
            ResolvedToolArtifact artifact,
            ClassLoader classLoader,
            ApplicationContext parentContext,
            Map<String, String> runtimeProperties
    ) {
        return create(artifact, classLoader, parentContext);
    }
}

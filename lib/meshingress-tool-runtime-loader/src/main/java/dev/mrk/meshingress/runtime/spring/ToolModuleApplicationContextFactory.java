package dev.mrk.meshingress.runtime.spring;

import dev.mrk.meshingress.runtime.artifacts.ResolvedToolArtifact;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

public interface ToolModuleApplicationContextFactory {
    ConfigurableApplicationContext create(
            ResolvedToolArtifact artifact,
            ClassLoader classLoader,
            ApplicationContext parentContext
    );
}

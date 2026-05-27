package dev.mrk.meshingress.mcp.tools.runtime;

import dev.mrk.meshingress.runtime.artifacts.LocalJarArtifactResolver;
import dev.mrk.meshingress.runtime.artifacts.LocalMavenRepositoryArtifactResolver;
import dev.mrk.meshingress.runtime.artifacts.ToolArtifactResolutionContext;
import dev.mrk.meshingress.runtime.artifacts.ToolArtifactResolver;
import dev.mrk.meshingress.runtime.artifacts.ToolArtifactResolverChain;
import dev.mrk.meshingress.runtime.loader.DefaultToolRuntimeLoader;
import dev.mrk.meshingress.runtime.loader.ToolModuleHandlerFactory;
import dev.mrk.meshingress.runtime.loader.ToolRuntimeLoader;
import dev.mrk.meshingress.runtime.registry.ToolRegistrationBridge;
import dev.mrk.meshingress.runtime.spring.SpringToolModuleApplicationContextFactory;
import dev.mrk.meshingress.runtime.spring.ToolModuleApplicationContextFactory;
import dev.mrk.meshingress.runtime.spring.ToolModuleClassLoaderFactory;
import dev.mrk.meshingress.runtime.spring.UrlToolModuleClassLoaderFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ToolRuntimeLoaderConfiguration {

    @Bean
    ToolArtifactResolutionContext toolArtifactResolutionContext() {
        return ToolArtifactResolutionContext.defaults();
    }

    @Bean
    ToolArtifactResolver toolArtifactResolver() {
        return new ToolArtifactResolverChain(List.of(
                new LocalMavenRepositoryArtifactResolver(),
                new LocalJarArtifactResolver()
        ));
    }

    @Bean
    ToolModuleClassLoaderFactory toolModuleClassLoaderFactory() {
        return new UrlToolModuleClassLoaderFactory();
    }

    @Bean
    ToolModuleApplicationContextFactory toolModuleApplicationContextFactory() {
        return new SpringToolModuleApplicationContextFactory();
    }

    @Bean
    ToolRuntimeLoader toolRuntimeLoader(
            ToolArtifactResolver toolArtifactResolver,
            ToolArtifactResolutionContext resolutionContext,
            ToolModuleClassLoaderFactory classLoaderFactory,
            ToolModuleApplicationContextFactory applicationContextFactory,
            ToolModuleHandlerFactory handlerFactory,
            ToolRegistrationBridge registrationBridge,
            ApplicationContext applicationContext
    ) {
        return new DefaultToolRuntimeLoader(
                toolArtifactResolver,
                resolutionContext,
                classLoaderFactory,
                applicationContextFactory,
                handlerFactory,
                registrationBridge,
                applicationContext
        );
    }
}

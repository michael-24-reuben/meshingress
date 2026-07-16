package dev.mrk.meshingress.mcp.tools.runtime;

import dev.mrk.meshingress.artifact.storage.FileSystemArtifactStorage;
import dev.mrk.meshingress.provisioning.ToolProvisioningService;
import dev.mrk.meshingress.provisioning.git.GitSourceProvisioner;
import dev.mrk.meshingress.provisioning.python.PythonVenvProvisioner;
import dev.mrk.meshingress.runtime.artifacts.LocalJarArtifactResolver;
import dev.mrk.meshingress.runtime.artifacts.LocalMavenRepositoryArtifactResolver;
import dev.mrk.meshingress.runtime.artifacts.ToolArtifactResolutionContext;
import dev.mrk.meshingress.runtime.artifacts.ToolArtifactResolver;
import dev.mrk.meshingress.runtime.artifacts.ToolArtifactResolverChain;
import dev.mrk.meshingress.runtime.loader.DefaultToolRuntimeLoader;
import dev.mrk.meshingress.runtime.loader.ToolModuleHandlerFactory;
import dev.mrk.meshingress.runtime.loader.ToolRuntimeLoader;
import dev.mrk.meshingress.runtime.provisioning.StaticManifestToolProvisioningGate;
import dev.mrk.meshingress.runtime.provisioning.ToolProvisioningGate;
import dev.mrk.meshingress.runtime.registry.ToolRegistrationBridge;
import dev.mrk.meshingress.runtime.spring.SpringToolModuleApplicationContextFactory;
import dev.mrk.meshingress.runtime.spring.ToolModuleApplicationContextFactory;
import dev.mrk.meshingress.runtime.spring.ToolModuleClassLoaderFactory;
import dev.mrk.meshingress.runtime.spring.UrlToolModuleClassLoaderFactory;
import dev.mrk.meshingress.toolmetadata.McpToolMetadata;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ToolRuntimeLoaderConfiguration {

    @Bean
    ToolArtifactResolutionContext toolArtifactResolutionContext(FileSystemArtifactStorage artifactStorage) {
        return new ToolArtifactResolutionContext(
                artifactStorage.layout().artifactsRoot(),
                null,
                false,
                List.of()
        );
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
    ToolProvisioningService toolProvisioningService() {
        return new ToolProvisioningService(List.of(new GitSourceProvisioner(), new PythonVenvProvisioner()));
    }

    @Bean
    ToolProvisioningGate toolProvisioningGate(
            FileSystemArtifactStorage artifactStorage,
            ToolProvisioningService provisioningService
    ) {
        return new StaticManifestToolProvisioningGate(artifactStorage.layout().root(), provisioningService);
    }

    @Bean
    McpToolMetadata mcpToolMetadata() {
        return new McpToolMetadata();
    }

    @Bean
    ToolRuntimeLoader toolRuntimeLoader(
            ToolArtifactResolver toolArtifactResolver,
            ToolArtifactResolutionContext resolutionContext,
            ToolModuleClassLoaderFactory classLoaderFactory,
            ToolModuleApplicationContextFactory applicationContextFactory,
            ToolModuleHandlerFactory handlerFactory,
            ToolRegistrationBridge registrationBridge,
            ApplicationContext applicationContext,
            ToolProvisioningGate provisioningGate
    ) {
        return new DefaultToolRuntimeLoader(
                toolArtifactResolver,
                resolutionContext,
                classLoaderFactory,
                applicationContextFactory,
                handlerFactory,
                registrationBridge,
                applicationContext,
                provisioningGate
        );
    }
}

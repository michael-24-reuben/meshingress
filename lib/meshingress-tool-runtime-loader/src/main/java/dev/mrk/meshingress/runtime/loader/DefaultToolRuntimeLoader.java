package dev.mrk.meshingress.runtime.loader;

import dev.mrk.meshingress.api.tools.McpToolHandler;
import dev.mrk.meshingress.runtime.artifacts.ResolvedToolArtifact;
import dev.mrk.meshingress.runtime.artifacts.ToolArtifactResolutionContext;
import dev.mrk.meshingress.runtime.artifacts.ToolArtifactResolver;
import dev.mrk.meshingress.runtime.artifacts.ToolArtifactSource;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleHandle;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleId;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleState;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleStatus;
import dev.mrk.meshingress.runtime.registry.ToolModuleRegistration;
import dev.mrk.meshingress.runtime.registry.ToolRegistrationBridge;
import dev.mrk.meshingress.runtime.spring.ToolModuleApplicationContextFactory;
import dev.mrk.meshingress.runtime.spring.ToolModuleClassLoaderFactory;
import dev.mrk.meshingress.runtime.provisioning.ToolProvisioningGate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.net.URLClassLoader;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DefaultToolRuntimeLoader implements ToolRuntimeLoader {

    private final ToolArtifactResolver artifactResolver;
    private final ToolArtifactResolutionContext resolutionContext;
    private final ToolModuleClassLoaderFactory classLoaderFactory;
    private final ToolModuleApplicationContextFactory applicationContextFactory;
    private final ToolModuleHandlerFactory handlerFactory;
    private final ToolRegistrationBridge registrationBridge;
    private final ApplicationContext parentContext;
    private final ToolProvisioningGate provisioningGate;
    private final Map<ToolModuleId, LoadedToolModule> loadedModules = new LinkedHashMap<>();

    public DefaultToolRuntimeLoader(
            ToolArtifactResolver artifactResolver,
            ToolArtifactResolutionContext resolutionContext,
            ToolModuleClassLoaderFactory classLoaderFactory,
            ToolModuleApplicationContextFactory applicationContextFactory,
            ToolModuleHandlerFactory handlerFactory,
            ToolRegistrationBridge registrationBridge,
            ApplicationContext parentContext
    ) {
        this(
                artifactResolver,
                resolutionContext,
                classLoaderFactory,
                applicationContextFactory,
                handlerFactory,
                registrationBridge,
                parentContext,
                ToolProvisioningGate.none()
        );
    }

    public DefaultToolRuntimeLoader(
            ToolArtifactResolver artifactResolver,
            ToolArtifactResolutionContext resolutionContext,
            ToolModuleClassLoaderFactory classLoaderFactory,
            ToolModuleApplicationContextFactory applicationContextFactory,
            ToolModuleHandlerFactory handlerFactory,
            ToolRegistrationBridge registrationBridge,
            ApplicationContext parentContext,
            ToolProvisioningGate provisioningGate
    ) {
        this.artifactResolver = artifactResolver;
        this.resolutionContext = resolutionContext;
        this.classLoaderFactory = classLoaderFactory;
        this.applicationContextFactory = applicationContextFactory;
        this.handlerFactory = handlerFactory;
        this.registrationBridge = registrationBridge;
        this.parentContext = parentContext;
        this.provisioningGate = provisioningGate == null ? ToolProvisioningGate.none() : provisioningGate;
    }

    @Override
    public synchronized ToolModuleHandle activate(ToolArtifactSource source) {
        ResolvedToolArtifact artifact = artifactResolver.resolve(source, resolutionContext);
        return activate(artifact);
    }

    @Override
    public synchronized ToolModuleHandle activate(ResolvedToolArtifact artifact) {
        if (loadedModules.containsKey(artifact.moduleId())) {
            throw new IllegalStateException("Tool module is already active: " + artifact.moduleId().value());
        }

        OffsetDateTime now = OffsetDateTime.now();
        URLClassLoader classLoader = null;
        ConfigurableApplicationContext moduleContext = null;
        try {
            Map<String, String> runtimeProperties = provisioningGate.requireReady(artifact);
            classLoader = classLoaderFactory.create(artifact);
            moduleContext = applicationContextFactory.create(artifact, classLoader, parentContext, runtimeProperties);
            List<McpToolHandler> handlers = handlerFactory.handlers(moduleContext, artifact.moduleId());
            if (handlers.isEmpty()) {
                throw new IllegalStateException("Tool module did not expose any MCP tool handlers: " + artifact.moduleId().value());
            }
            ToolModuleRegistration registration = registrationBridge.register(artifact.moduleId(), handlers);
            LoadedToolModule loaded = new LoadedToolModule(
                    artifact,
                    classLoader,
                    moduleContext,
                    registration,
                    ToolModuleState.ACTIVE,
                    now,
                    now,
                    null
            );
            loadedModules.put(artifact.moduleId(), loaded);
            return new ToolModuleHandle(artifact.moduleId(), ToolModuleState.ACTIVE, now, registration.registeredFunctions());
        } catch (RuntimeException exception) {
            closeContext(moduleContext);
            closeClassLoader(classLoader);
            loadedModules.put(artifact.moduleId(), new LoadedToolModule(
                    artifact,
                    classLoader,
                    moduleContext,
                    new ToolModuleRegistration(List.of()),
                    ToolModuleState.FAILED,
                    null,
                    OffsetDateTime.now(),
                    exception.getMessage()
            ));
            throw exception;
        }
    }

    @Override
    public synchronized void deactivate(ToolModuleId moduleId) {
        LoadedToolModule loaded = loadedModules.get(moduleId);
        if (loaded == null || loaded.state() != ToolModuleState.ACTIVE) {
            throw new IllegalArgumentException("Tool module is not active: " + moduleId.value());
        }
        registrationBridge.unregister(moduleId);
        closeContext(loaded.context());
        closeClassLoader(loaded.classLoader());
        loadedModules.put(moduleId, loaded.withState(ToolModuleState.UNLOADED, OffsetDateTime.now(), null));
    }

    @Override
    public synchronized Optional<ToolModuleStatus> status(ToolModuleId moduleId) {
        return Optional.ofNullable(loadedModules.get(moduleId)).map(LoadedToolModule::status);
    }

    @Override
    public synchronized List<ToolModuleStatus> list() {
        return loadedModules.values().stream()
                .map(LoadedToolModule::status)
                .toList();
    }

    private void closeContext(ConfigurableApplicationContext context) {
        if (context != null) {
            context.close();
        }
    }

    private void closeClassLoader(URLClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            classLoader.close();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to close tool module classloader", exception);
        }
    }

    private record LoadedToolModule(
            ResolvedToolArtifact artifact,
            URLClassLoader classLoader,
            ConfigurableApplicationContext context,
            ToolModuleRegistration registration,
            ToolModuleState state,
            OffsetDateTime activatedAt,
            OffsetDateTime updatedAt,
            String message
    ) {
        ToolModuleStatus status() {
            return new ToolModuleStatus(
                    artifact.moduleId(),
                    state,
                    activatedAt,
                    updatedAt,
                    registration.registeredFunctions(),
                    message
            );
        }

        LoadedToolModule withState(ToolModuleState nextState, OffsetDateTime updatedAt, String message) {
            return new LoadedToolModule(artifact, classLoader, context, registration, nextState, activatedAt, updatedAt, message);
        }
    }
}

package dev.mrk.meshingress.mcp.services;

import dev.mrk.meshingress.api.services.McpServiceResolver;
import dev.mrk.meshingress.api.services.McpServiceDescriptor;
import dev.mrk.meshingress.api.services.McpServiceLifecycle;
import dev.mrk.meshingress.api.services.McpServiceRequirementDescriptor;
import dev.mrk.meshingress.api.tools.annotation.McpService;
import dev.mrk.meshingress.tools.framework.scanning.McpToolAnnotationScanner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.lang.reflect.Method;

/** Registry for typed, non-callable provider-node capabilities. */
@Component
public final class McpServiceRegistry implements McpServiceResolver {
    private final Map<Class<?>, List<Object>> providers;
    private final Map<String, List<RegisteredProvider>> providersByContractName;
    private final List<McpServiceDescriptor> descriptors;

    public McpServiceRegistry(ApplicationContext applicationContext, ObjectMapper objectMapper) {
        Map<Class<?>, List<Object>> discovered = new LinkedHashMap<>();
        Map<String, List<RegisteredProvider>> discoveredByContractName = new LinkedHashMap<>();
        Map<String, McpServiceDescriptor> discoveredDescriptors = new LinkedHashMap<>();
        McpToolAnnotationScanner schemaScanner = new McpToolAnnotationScanner(objectMapper, "service");
        for (Object bean : applicationContext.getBeansWithAnnotation(McpService.class).values()) {
            McpService service = ClassUtils.getUserClass(bean).getAnnotation(McpService.class);
            if (service == null || !service.enabled()) continue;
            if (service.provides().length == 0) throw new IllegalStateException("MCP service must declare at least one provided contract: " + bean.getClass().getName());
            Class<?> serviceType = ClassUtils.getUserClass(bean);
            String serviceName = service.value().isBlank() ? serviceType.getName() : service.value();
            for (Class<?> contract : service.provides()) {
                if (!McpServiceLifecycle.class.isAssignableFrom(contract)) {
                    throw new IllegalStateException("MCP service contract must implement "
                            + McpServiceLifecycle.class.getName() + ": " + contract.getName());
                }
            }
            if (!(bean instanceof McpServiceLifecycle)) {
                throw new IllegalStateException("MCP service node must implement "
                        + McpServiceLifecycle.class.getName() + ": " + bean.getClass().getName());
            }
            Optional<Method> initializer = schemaScanner.serviceInitializationFor(serviceType);
            McpServiceDescriptor descriptor = new McpServiceDescriptor(
                    serviceName,
                    service.title(),
                    service.description(),
                    service.version(),
                    service.enabled(),
                    service.visibility(),
                    java.util.Arrays.stream(service.provides()).map(Class::getName).toList(),
                    java.util.Arrays.stream(service.requires())
                            .map(requirement -> new McpServiceRequirementDescriptor(
                                    requirement.type().getName(),
                                    java.util.Arrays.stream(requirement.oneOf()).map(Class::getName).toList()
                            ))
                            .toList(),
                    initializer.map(schemaScanner::inputSchemaForServiceInitialization)
                            .orElseGet(objectMapper::createObjectNode)
            );
            if (discoveredDescriptors.putIfAbsent(serviceName, descriptor) != null) {
                throw new IllegalStateException("Duplicate MCP service name: " + serviceName);
            }
            for (Class<?> contract : service.provides()) {
                if (!contract.isInstance(bean)) throw new IllegalStateException("MCP service node does not implement its declared contract: " + bean.getClass().getName());
                List<Object> contractProviders = discovered.computeIfAbsent(contract, ignored -> new java.util.ArrayList<>());
                if (!contractProviders.contains(bean)) {
                    contractProviders.add(bean);
                }
                discoveredByContractName.computeIfAbsent(contract.getName(), ignored -> new java.util.ArrayList<>())
                        .add(new RegisteredProvider(serviceType.getName(), descriptor));
            }
        }
        Map<Class<?>, List<Object>> immutableProviders = new LinkedHashMap<>();
        discovered.forEach((contract, contractProviders) -> immutableProviders.put(contract, List.copyOf(contractProviders)));
        providers = Map.copyOf(immutableProviders);
        Map<String, List<RegisteredProvider>> immutableProvidersByContractName = new LinkedHashMap<>();
        discoveredByContractName.forEach((contract, values) -> immutableProvidersByContractName.put(contract, List.copyOf(values)));
        providersByContractName = Map.copyOf(immutableProvidersByContractName);
        descriptors = discoveredDescriptors.values().stream().toList();
    }

    @Override
    public <T> Optional<T> find(Class<T> contract) {
        List<T> matches = findAll(contract);
        if (matches.size() > 1) {
            throw new IllegalStateException("Multiple MCP service providers are available for " + contract.getName()
                    + "; declare oneOf to select a concrete provider");
        }
        return matches.stream().findFirst();
    }

    @Override
    public <T> List<T> findAll(Class<T> contract) {
        return providers.getOrDefault(contract, List.of()).stream().map(contract::cast).toList();
    }

    public List<McpServiceDescriptor> listPublicEnabledServices() {
        return descriptors.stream()
                .filter(McpServiceDescriptor::enabled)
                .filter(descriptor -> descriptor.visibility() == dev.mrk.meshingress.api.tools.ToolVisibility.PUBLIC)
                .toList();
    }

    /** Adds client-facing provider requirements to one public tool descriptor JSON object. */
    public void addRequirements(ObjectNode function, ObjectMapper objectMapper) {
        JsonNode declared = function.path("annotations").path("requires");
        if (!declared.isArray() || declared.isEmpty()) {
            return;
        }
        ObjectNode requirements = objectMapper.createObjectNode();
        for (JsonNode requirement : declared) {
            String type = requirement.path("type").asString("");
            if (type.isBlank()) {
                continue;
            }
            String key = requirement.path("value").asString("");
            if (key.isBlank()) {
                continue;
            }
            if (requirements.has(key)) {
                throw new IllegalStateException("Duplicate MCP service requirement key for function "
                        + function.path("name").asString("<unnamed>") + ": " + key);
            }
            ObjectNode published = objectMapper.createObjectNode();
            published.put("type", type);
            String title = requirement.path("title").asString("");
            if (!title.isBlank()) published.put("title", title);
            String description = requirement.path("description").asString("");
            if (!description.isBlank()) published.put("description", description);
            published.set("inputSchema", requirementInputSchema(requirement, objectMapper));
            requirements.set(key, published);
        }
        function.set("requirements", requirements);
    }

    private JsonNode requirementInputSchema(JsonNode declared, ObjectMapper objectMapper) {
        List<String> selectedTypes = new java.util.ArrayList<>();
        declared.path("oneOf").forEach(node -> selectedTypes.add(node.asString()));
        List<RegisteredProvider> candidates = providersByContractName
                .getOrDefault(declared.path("type").asString(""), List.of())
                .stream()
                .filter(provider -> selectedTypes.isEmpty() || selectedTypes.contains(provider.implementationType()))
                .toList();
        if (candidates.size() == 1) {
            return candidates.getFirst().descriptor().inputSchema().deepCopy();
        }
        if (candidates.isEmpty()) {
            return objectMapper.createObjectNode();
        }
        ObjectNode schema = objectMapper.createObjectNode();
        ArrayNode alternatives = objectMapper.createArrayNode();
        candidates.forEach(provider -> alternatives.add(provider.descriptor().inputSchema()));
        schema.set("oneOf", alternatives);
        return schema;
    }

    private record RegisteredProvider(String implementationType, McpServiceDescriptor descriptor) {
    }
}

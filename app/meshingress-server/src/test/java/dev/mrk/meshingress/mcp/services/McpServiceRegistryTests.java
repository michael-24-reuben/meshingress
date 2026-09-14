package dev.mrk.meshingress.mcp.services;

import dev.mrk.meshingress.api.services.McpServiceLifecycle;
import dev.mrk.meshingress.api.tools.annotation.McpService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpServiceRegistryTests {

    @Test
    void selectsOneOfProviderWhenSeveralProvidersShareAContract() {
        try (AnnotationConfigApplicationContext context = contextWithProviders()) {
            McpServiceRegistry registry = new McpServiceRegistry(context, new tools.jackson.databind.ObjectMapper());

            GreetingService provider = registry.require(GreetingService.class, PreferredGreetingService.class);

            assertInstanceOf(PreferredGreetingService.class, provider);
        }
    }

    @Test
    void rejectsAnAmbiguousUnqualifiedProviderRequest() {
        try (AnnotationConfigApplicationContext context = contextWithProviders()) {
            McpServiceRegistry registry = new McpServiceRegistry(context, new tools.jackson.databind.ObjectMapper());

            assertThrows(IllegalStateException.class, () -> registry.require(GreetingService.class));
        }
    }

    @Test
    void listsPublicProviderMetadataWithoutExposingAnInstance() {
        try (AnnotationConfigApplicationContext context = contextWithProviders()) {
            McpServiceRegistry registry = new McpServiceRegistry(context, new tools.jackson.databind.ObjectMapper());

            var descriptor = registry.listPublicEnabledServices().stream()
                    .filter(service -> service.name().equals("preferred-greeting"))
                    .findFirst()
                    .orElseThrow();

            assertEquals("Preferred greeting", descriptor.title());
            assertEquals(GreetingService.class.getName(), descriptor.provides().getFirst());
        }
    }

    @Test
    void rejectsAProviderContractThatDoesNotParticipateInTheServiceLifecycle() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(NonLifecycleService.class);
            context.refresh();

            IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new McpServiceRegistry(context, new tools.jackson.databind.ObjectMapper()));

            assertEquals("MCP service contract must implement dev.mrk.meshingress.api.services.McpServiceLifecycle: "
                    + NonLifecycleContract.class.getName(), exception.getMessage());
        }
    }

    private static AnnotationConfigApplicationContext contextWithProviders() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(AlternateGreetingService.class);
        context.registerBean(PreferredGreetingService.class);
        context.refresh();
        return context;
    }

    interface GreetingService extends McpServiceLifecycle {
    }

    @McpService(value = "preferred-greeting", title = "Preferred greeting", provides = GreetingService.class)
    static class PreferredGreetingService implements GreetingService {
    }

    @McpService(provides = GreetingService.class)
    static class AlternateGreetingService implements GreetingService {
    }

    interface NonLifecycleContract {
    }

    @McpService(provides = NonLifecycleContract.class)
    static class NonLifecycleService implements NonLifecycleContract {
    }
}

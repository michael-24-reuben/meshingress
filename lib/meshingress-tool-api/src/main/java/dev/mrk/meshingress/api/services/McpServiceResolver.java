package dev.mrk.meshingress.api.services;

import java.util.Optional;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Resolves provider-node capabilities for internal composition. It is deliberately
 * separate from MCP response objects and never exposes credentials.
 */
public interface McpServiceResolver {
    <T> Optional<T> find(Class<T> contract);

    /**
     * Returns every registered provider for a capability. Implementations that only
     * support one provider can retain the {@link #find(Class)} implementation.
     */
    default <T> List<T> findAll(Class<T> contract) {
        return find(contract).stream().toList();
    }

    /**
     * Resolves a single provider, optionally restricted to concrete provider types.
     * An empty selection means the capability must have exactly one provider.
     */
    default <T> Optional<T> find(Class<T> contract, Class<?>... oneOf) {
        Objects.requireNonNull(contract, "contract must not be null");
        Class<?>[] selectedTypes = oneOf == null ? new Class<?>[0] : oneOf;
        List<T> matches = findAll(contract).stream()
                .filter(provider -> selectedTypes.length == 0
                        || Arrays.stream(selectedTypes).anyMatch(type -> type.isInstance(provider)))
                .toList();
        if (matches.size() > 1) {
            throw new IllegalStateException("Multiple MCP service providers match " + contract.getName()
                    + "; declare oneOf to select a concrete provider");
        }
        return matches.stream().findFirst();
    }

    default <T> T require(Class<T> contract) {
        return find(contract).orElseThrow(() -> new IllegalStateException("No MCP service provider is available for " + contract.getName()));
    }

    default <T> T require(Class<T> contract, Class<?>... oneOf) {
        return find(contract, oneOf).orElseThrow(() -> new IllegalStateException(
                "No MCP service provider is available for " + contract.getName() + selectionSuffix(oneOf)
        ));
    }

    private static String selectionSuffix(Class<?>[] oneOf) {
        if (oneOf == null || oneOf.length == 0) {
            return "";
        }
        return " matching oneOf [" + Arrays.stream(oneOf).map(Class::getName).reduce((left, right) -> left + ", " + right).orElse("") + "]";
    }

    static McpServiceResolver none() {
        return new McpServiceResolver() {
            @Override public <T> Optional<T> find(Class<T> contract) { return Optional.empty(); }
        };
    }
}

package dev.mrk.aegis;

import java.util.Objects;
import java.util.Set;

/** Declares the grants required before a capability can be created. */
public record CapabilityDefinition<C extends Capability>(
        String id,
        Set<String> requiredGrants,
        CapabilityProvider<C> provider
) {
    public CapabilityDefinition {
        id = requireText(id, "id");
        requiredGrants = Set.copyOf(Objects.requireNonNull(requiredGrants, "requiredGrants"));
        if (requiredGrants.stream().anyMatch(grant -> grant == null || grant.isBlank())) {
            throw new IllegalArgumentException("requiredGrants must not contain blank values");
        }
        provider = Objects.requireNonNull(provider, "provider");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

package dev.mrk.meshingress.route.framework;

import dev.mrk.meshingress.route.annotations.McpAvailabilityPolicy;
import dev.mrk.meshingress.route.api.AvailabilityPolicy;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

public class DefaultAvailabilityPolicyRegistry {

    protected final Map<Class<? extends Annotation>, AvailabilityPolicy<? extends Annotation>> policies;

    public DefaultAvailabilityPolicyRegistry() {
        this(defaultPolicies());
    }

    public DefaultAvailabilityPolicyRegistry(Map<Class<? extends Annotation>, AvailabilityPolicy<? extends Annotation>> policies) {
        this.policies = new HashMap<>(Map.copyOf(policies));
    }

    public final Optional<AvailabilityPolicy<? extends Annotation>> findPolicy(Class<? extends Annotation> annotationType) {
        AvailabilityPolicy<? extends Annotation> directPolicy = policies.get(annotationType);
        if (directPolicy != null) {
            return Optional.of(directPolicy);
        }
        McpAvailabilityPolicy mappedPolicy = annotationType.getAnnotation(McpAvailabilityPolicy.class);
        if (mappedPolicy == null) {
            return Optional.empty();
        }
        return Optional.of(instantiate(mappedPolicy.value()));
    }

    public final boolean isAvailabilityAnnotation(Class<? extends Annotation> annotationType) {
        return policies.containsKey(annotationType) || annotationType.isAnnotationPresent(McpAvailabilityPolicy.class);
    }

    public boolean addAvailabilityPolicy(@NotNull Class<? extends Annotation> annotation, @NotNull AvailabilityPolicy<? extends Annotation> availabilityPolicy) {
        if (annotation == null || availabilityPolicy == null) {
            throw new IllegalArgumentException("Annotation and availability policy must not be null");
        }
        if (policies.containsKey(annotation)) {
            return false;
        }
        policies.put(annotation, availabilityPolicy);
        return true;
    }

    public AvailabilityPolicy<? extends Annotation> removeAvailabilityPolicy(@NotNull Class<? extends Annotation> annotation) {
        if (annotation == null) {
            throw new IllegalArgumentException("Annotation must not be null");
        }
        return policies.remove(annotation);
    }


    private @NonNull AvailabilityPolicy<? extends Annotation> instantiate(@NonNull Class<? extends AvailabilityPolicy<?>> policyType) {
        try {
            return policyType.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to create availability policy: " + policyType.getName(), exception);
        }
    }

    @Contract(pure = true)
    private static @NonNull @Unmodifiable Map<Class<? extends Annotation>, AvailabilityPolicy<? extends Annotation>> defaultPolicies() {
        return Map.of();
    }
}

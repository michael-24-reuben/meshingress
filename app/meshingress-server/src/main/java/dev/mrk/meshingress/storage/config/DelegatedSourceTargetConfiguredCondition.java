package dev.mrk.meshingress.storage.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** Matches only when delegated-source support was explicitly configured with a usable target name. */
public final class DelegatedSourceTargetConfiguredCondition implements Condition {
    static final String PROPERTY = "meshingress.storage.external.delegated-target";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String target = context.getEnvironment().getProperty(PROPERTY, "");
        return !target.isBlank();
    }
}

package dev.mrk.meshingress.storage.config;

import org.junit.jupiter.api.Test;
import dev.mrk.meshingress.config.MeshingressProperties;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DelegatedSourceTargetConfiguredConditionTests {
    @Test
    void blankTargetDoesNotCreateDelegatedViewerBeans() {
        assertFalse(matches("   "));
    }

    @Test
    void namedTargetCreatesDelegatedViewerBeans() {
        assertTrue(matches("nextcloud-primary"));
    }

    @Test
    void immutableStoragePropertiesBindTheDelegatedTarget() {
        MeshingressProperties properties = new Binder(new MapConfigurationPropertySource(java.util.Map.of(
                DelegatedSourceTargetConfiguredCondition.PROPERTY, "nextcloud-primary"
        ))).bind("meshingress", Bindable.of(MeshingressProperties.class))
                .orElseThrow(() -> new AssertionError("Meshingress properties were not bound"));

        assertEquals("nextcloud-primary", properties.storage().external().delegatedTarget());
    }

    private boolean matches(String target) {
        MockEnvironment environment = new MockEnvironment().withProperty(DelegatedSourceTargetConfiguredCondition.PROPERTY, target);
        ConditionContext context = mock(ConditionContext.class);
        when(context.getEnvironment()).thenReturn(environment);
        return new DelegatedSourceTargetConfiguredCondition().matches(context, null);
    }
}

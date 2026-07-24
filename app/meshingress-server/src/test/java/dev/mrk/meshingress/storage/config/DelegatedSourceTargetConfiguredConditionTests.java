package dev.mrk.meshingress.storage.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private boolean matches(String target) {
        MockEnvironment environment = new MockEnvironment().withProperty(DelegatedSourceTargetConfiguredCondition.PROPERTY, target);
        ConditionContext context = mock(ConditionContext.class);
        when(context.getEnvironment()).thenReturn(environment);
        return new DelegatedSourceTargetConfiguredCondition().matches(context, null);
    }
}

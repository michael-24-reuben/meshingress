package dev.mrk.aegis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceLimitPolicyTest {
    @Test
    void childPolicyCanNarrowAnExplicitParentWithoutChangingOtherValues() {
        ResourceLimitPolicy parent = new ResourceLimitPolicy(1, ResourceLimitPolicy.Enforcement.ENABLED,
                ResourceLimitPolicy.LimitValue.limited(100, 60), ResourceLimitPolicy.LimitValue.unlimited(),
                ResourceLimitPolicy.LimitValue.limited(10, 0), ResourceLimitPolicy.LimitValue.unlimited());
        ResourceLimitPolicy child = new ResourceLimitPolicy(1, ResourceLimitPolicy.Enforcement.INHERIT,
                ResourceLimitPolicy.LimitValue.limited(5, 60), ResourceLimitPolicy.LimitValue.inherit(),
                ResourceLimitPolicy.LimitValue.inherit(), ResourceLimitPolicy.LimitValue.inherit());

        ResourceLimitPolicy resolved = child.resolveAgainst(parent);

        assertEquals(ResourceLimitPolicy.Enforcement.ENABLED, resolved.enforcement());
        assertEquals(5, resolved.requests().limit());
        assertEquals(10, resolved.concurrentCalls().limit());
    }

    @Test
    void limitedValuesNeedAPositiveLimit() {
        assertThrows(IllegalArgumentException.class, () -> ResourceLimitPolicy.LimitValue.limited(0, 60));
    }
}

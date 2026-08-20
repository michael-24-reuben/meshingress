package dev.mrk.aegis;

import java.util.Objects;

/**
 * Provider-neutral, credential-safe resource limits attached to a profile.
 * Values are deliberately explicit: a child policy inherits, limits, or
 * explicitly declares a resource unlimited; an omitted value never silently
 * changes into an unlimited value.
 */
public record ResourceLimitPolicy(
        int schemaVersion,
        Enforcement enforcement,
        LimitValue requests,
        LimitValue toolExecutions,
        LimitValue concurrentCalls,
        LimitValue storageBytes
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public ResourceLimitPolicy {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported resource limit policy schema version: " + schemaVersion);
        }
        enforcement = Objects.requireNonNullElse(enforcement, Enforcement.INHERIT);
        requests = Objects.requireNonNullElseGet(requests, LimitValue::inherit);
        toolExecutions = Objects.requireNonNullElseGet(toolExecutions, LimitValue::inherit);
        concurrentCalls = Objects.requireNonNullElseGet(concurrentCalls, LimitValue::inherit);
        storageBytes = Objects.requireNonNullElseGet(storageBytes, LimitValue::inherit);
    }

    public static ResourceLimitPolicy inheritAll() {
        return new ResourceLimitPolicy(CURRENT_SCHEMA_VERSION, Enforcement.INHERIT,
                LimitValue.inherit(), LimitValue.inherit(), LimitValue.inherit(), LimitValue.inherit());
    }

    /** Applies this (more specific) policy over a less-specific parent policy. */
    public ResourceLimitPolicy resolveAgainst(ResourceLimitPolicy parent) {
        parent = Objects.requireNonNullElseGet(parent, ResourceLimitPolicy::inheritAll);
        return new ResourceLimitPolicy(CURRENT_SCHEMA_VERSION,
                enforcement == Enforcement.INHERIT ? parent.enforcement : enforcement,
                requests.resolveAgainst(parent.requests),
                toolExecutions.resolveAgainst(parent.toolExecutions),
                concurrentCalls.resolveAgainst(parent.concurrentCalls),
                storageBytes.resolveAgainst(parent.storageBytes));
    }

    public enum Enforcement { INHERIT, ENABLED, DISABLED }

    /**
     * A limited request/execution value uses a positive rolling-window seconds
     * value. Concurrent and storage values must use a zero window.
     */
    public record LimitValue(Mode mode, long limit, long windowSeconds) {
        public LimitValue {
            mode = Objects.requireNonNullElse(mode, Mode.INHERIT);
            if (limit < 0 || windowSeconds < 0) {
                throw new IllegalArgumentException("limit and windowSeconds must not be negative");
            }
            if (mode != Mode.LIMITED && (limit != 0 || windowSeconds != 0)) {
                throw new IllegalArgumentException("only LIMITED values may declare a limit or window");
            }
            if (mode == Mode.LIMITED && limit == 0) {
                throw new IllegalArgumentException("LIMITED values require a positive limit");
            }
        }

        public static LimitValue inherit() { return new LimitValue(Mode.INHERIT, 0, 0); }
        public static LimitValue unlimited() { return new LimitValue(Mode.UNLIMITED, 0, 0); }
        public static LimitValue limited(long limit, long windowSeconds) { return new LimitValue(Mode.LIMITED, limit, windowSeconds); }
        public LimitValue resolveAgainst(LimitValue parent) {
            parent = Objects.requireNonNullElseGet(parent, LimitValue::inherit);
            return mode == Mode.INHERIT ? parent : this;
        }
    }

    public enum Mode { INHERIT, UNLIMITED, LIMITED }
}

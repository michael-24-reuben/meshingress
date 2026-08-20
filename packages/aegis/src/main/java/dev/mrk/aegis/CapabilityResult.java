package dev.mrk.aegis;

import java.util.Objects;
import java.util.Optional;

/** The only outcome of a capability request: an operation surface or a safe denial reason. */
public sealed interface CapabilityResult<C extends Capability>
        permits CapabilityResult.Granted, CapabilityResult.Denied {

    boolean granted();

    Optional<C> capability();

    Optional<DenialReason> denialReason();

    static <C extends Capability> CapabilityResult<C> granted(C capability) {
        return new Granted<>(capability);
    }

    static <C extends Capability> CapabilityResult<C> denied(DenialReason reason) {
        return new Denied<>(reason);
    }

    record Granted<C extends Capability>(C grantedCapability) implements CapabilityResult<C> {
        public Granted {
            grantedCapability = Objects.requireNonNull(grantedCapability, "grantedCapability");
        }

        @Override
        public boolean granted() {
            return true;
        }

        @Override
        public Optional<C> capability() {
            return Optional.of(grantedCapability);
        }

        @Override
        public Optional<DenialReason> denialReason() {
            return Optional.empty();
        }
    }

    record Denied<C extends Capability>(DenialReason reason) implements CapabilityResult<C> {
        public Denied {
            reason = Objects.requireNonNull(reason, "reason");
        }

        @Override
        public boolean granted() {
            return false;
        }

        @Override
        public Optional<C> capability() {
            return Optional.empty();
        }

        @Override
        public Optional<DenialReason> denialReason() {
            return Optional.of(reason);
        }
    }
}

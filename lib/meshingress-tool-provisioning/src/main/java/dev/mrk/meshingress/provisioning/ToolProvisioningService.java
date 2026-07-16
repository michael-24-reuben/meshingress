package dev.mrk.meshingress.provisioning;

import java.util.List;
import java.util.Objects;

/**
 * Selects one provider deterministically. Runtime activation is intentionally outside this boundary.
 */
public final class ToolProvisioningService {
    private final List<ToolProvisioner<?>> provisioners;

    public ToolProvisioningService(List<ToolProvisioner<?>> provisioners) {
        this.provisioners = provisioners == null ? List.of() : List.copyOf(provisioners);
    }

    public ProvisioningResult provision(Object request) {
        Objects.requireNonNull(request, "request must not be null");
        List<ToolProvisioner<?>> matches = provisioners.stream()
                .filter(provider -> provider.requestType().isInstance(request))
                .toList();
        if (matches.size() != 1) {
            return new ProvisioningResult(
                    matches.isEmpty() ? ProvisioningStatus.UNSUPPORTED : ProvisioningStatus.FAILED,
                    null,
                    List.of(),
                    List.of(),
                    matches.isEmpty()
                            ? "No provisioner supports " + request.getClass().getName()
                            : "Multiple provisioners support " + request.getClass().getName()
            );
        }
        return provisionUnchecked(matches.getFirst(), request);
    }

    @SuppressWarnings("unchecked")
    private static <R> ProvisioningResult provisionUnchecked(ToolProvisioner<?> provider, Object request) {
        return ((ToolProvisioner<R>) provider).provision((R) request);
    }
}

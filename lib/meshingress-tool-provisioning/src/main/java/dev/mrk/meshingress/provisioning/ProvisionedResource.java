package dev.mrk.meshingress.provisioning;

import java.nio.file.Path;
import java.util.Map;

public record ProvisionedResource(
        Path sourceRoot,
        Path runtimeRoot,
        Path executable,
        String identity,
        Map<String, String> attributes
) {
    public ProvisionedResource {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        identity = identity == null ? "" : identity.trim();
    }
}

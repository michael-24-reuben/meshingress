package dev.mrk.meshingress.api.services;

import java.util.List;

/** Public metadata for one capability required by a service or callable tool. */
public record McpServiceRequirementDescriptor(String type, List<String> oneOf) {

    public McpServiceRequirementDescriptor {
        oneOf = oneOf == null ? List.of() : List.copyOf(oneOf);
    }
}

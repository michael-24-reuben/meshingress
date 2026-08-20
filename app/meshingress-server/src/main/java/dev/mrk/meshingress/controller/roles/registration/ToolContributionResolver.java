package dev.mrk.meshingress.controller.roles.registration;

import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Resolves function ownership from persisted namespace precedence, never discovery order. */
public final class ToolContributionResolver {

    public Resolution resolve(String namespace, List<ContributionFunctions> candidates) {
        String prefix = namespace == null || namespace.isBlank() ? "" : namespace.strip() + ".";
        Map<String, FunctionOwner> owners = new LinkedHashMap<>();
        List<Conflict> conflicts = new ArrayList<>();
        for (ContributionFunctions candidate : (candidates == null ? List.<ContributionFunctions>of() : candidates).stream()
                .sorted(Comparator.comparingInt(value -> value.contribution().precedence()))
                .toList()) {
            for (McpFunctionDescriptor function : candidate.functions()) {
                if (!function.name().startsWith(prefix)) {
                    throw new IllegalArgumentException("Function " + function.name() + " does not belong to namespace " + namespace);
                }
                FunctionOwner existing = owners.putIfAbsent(function.name(), new FunctionOwner(candidate.contribution(), function));
                if (existing != null) conflicts.add(new Conflict(function.name(), existing.contribution().toolId(), candidate.contribution().toolId()));
            }
        }
        return new Resolution(Map.copyOf(owners), List.copyOf(conflicts));
    }

    public record ContributionFunctions(ToolContributionRecord contribution, List<McpFunctionDescriptor> functions) {
        public ContributionFunctions {
            if (contribution == null) throw new IllegalArgumentException("tool contribution is required");
            functions = functions == null ? List.of() : List.copyOf(functions);
        }
    }

    public record FunctionOwner(ToolContributionRecord contribution, McpFunctionDescriptor function) { }

    public record Conflict(String functionName, String owningToolId, String rejectedToolId) { }

    public record Resolution(Map<String, FunctionOwner> owners, List<Conflict> conflicts) { }
}

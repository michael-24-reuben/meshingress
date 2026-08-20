package dev.mrk.meshingress.controller.roles.registration;

import java.util.List;

public interface ToolContributionStore {
    ToolContributionRecord reserve(ToolContributionRecord contribution);

    List<ToolContributionRecord> listNamespace(String namespace);

    default List<ToolContributionRecord> listContributions() {
        return List.of();
    }

    default void replaceConflicts(String namespace, List<ToolContributionConflict> conflicts) {
    }

    default List<ToolContributionConflict> listConflicts(String namespace) {
        return List.of();
    }
}

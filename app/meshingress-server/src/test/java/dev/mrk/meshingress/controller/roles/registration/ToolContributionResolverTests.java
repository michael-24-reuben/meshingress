package dev.mrk.meshingress.controller.roles.registration;

import dev.mrk.meshingress.api.tools.ToolVisibility;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolContributionResolverTests {

    private final ToolContributionResolver resolver = new ToolContributionResolver();

    @Test
    void higherPrecedenceContributionOwnsDuplicateRegardlessOfCandidateOrder() {
        ToolContributionRecord official = contribution("tool001", 10);
        ToolContributionRecord extension = contribution("tool005", 20);

        ToolContributionResolver.Resolution resolution = resolver.resolve("youtube", List.of(
                new ToolContributionResolver.ContributionFunctions(extension, List.of(function("youtube.video.metadata"), function("youtube.video.download"))),
                new ToolContributionResolver.ContributionFunctions(official, List.of(function("youtube.video.metadata")))
        ));

        assertThat(resolution.owners().get("youtube.video.metadata").contribution().toolId()).isEqualTo("tool001");
        assertThat(resolution.owners().get("youtube.video.download").contribution().toolId()).isEqualTo("tool005");
        assertThat(resolution.conflicts()).containsExactly(new ToolContributionResolver.Conflict("youtube.video.metadata", "tool001", "tool005"));
    }

    @Test
    void rejectsFunctionOutsideContributionNamespace() {
        assertThatThrownBy(() -> resolver.resolve("youtube", List.of(
                new ToolContributionResolver.ContributionFunctions(contribution("tool001", 10), List.of(function("vimeo.video.metadata")))
        ))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("does not belong to namespace youtube");
    }

    private ToolContributionRecord contribution(String toolId, int precedence) {
        return new ToolContributionRecord(toolId, "youtube", precedence, ToolContributionActivationMode.CONTRIBUTOR, "reserved", OffsetDateTime.parse("2026-08-07T00:00:00Z"));
    }

    private McpFunctionDescriptor function(String name) {
        return new McpFunctionDescriptor(name, name, "", 1, true, ToolVisibility.PUBLIC, name + ".handler", null, null, null, false);
    }
}

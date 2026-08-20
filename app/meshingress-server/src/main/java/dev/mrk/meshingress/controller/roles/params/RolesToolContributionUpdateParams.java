package dev.mrk.meshingress.controller.roles.params;

import dev.mrk.meshingress.controller.roles.registration.ToolContributionActivationMode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Administrator-controlled precedence and activation state for a registered contribution.")
public record RolesToolContributionUpdateParams(
        @Schema(description = "Registered tool artifact identifier.", example = "tool005") String toolId,
        @Schema(description = "One-name tool namespace.", example = "youtube") String namespace,
        @Schema(description = "Lower values own duplicate function names first.", example = "20") Integer precedence,
        @Schema(description = "Activation policy for this contribution.") ToolContributionActivationMode activationMode
) {
}

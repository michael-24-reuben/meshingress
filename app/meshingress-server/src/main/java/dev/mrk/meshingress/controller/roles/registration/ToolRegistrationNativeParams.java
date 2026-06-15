package dev.mrk.meshingress.controller.roles.registration;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Native built-in registration source.")
public record ToolRegistrationNativeParams(
        @Schema(description = "Native namespace to register.", example = "architect")
        String namespace
) {
}


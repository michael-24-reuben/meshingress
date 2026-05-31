package dev.mrk.toolspace.voicebox;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record VoiceboxGenerationStatusArgs(
        @McpInputField(
                value = "generationId",
                description = "Voicebox generation id returned by voicebox.speak.",
                required = true
        )
        String generationId
) {
}

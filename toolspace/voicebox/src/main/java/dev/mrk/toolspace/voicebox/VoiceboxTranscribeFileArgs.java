package dev.mrk.toolspace.voicebox;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record VoiceboxTranscribeFileArgs(
        @McpInputField(
                value = "audioPath",
                description = "Absolute path to a local audio file readable by the Meshingress process.",
                required = true
        )
        String audioPath,

        @McpInputField(
                value = "language",
                description = "Optional transcription language code.",
                required = false
        )
        String language,

        @McpInputField(
                value = "model",
                description = "Optional Whisper model: base, small, medium, large, or turbo.",
                required = false
        )
        String model
) {
}

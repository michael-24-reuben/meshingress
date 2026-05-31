package dev.mrk.toolspace.voicebox;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record VoiceboxSpeakArgs(
        @McpInputField(
                value = "text",
                description = "Text to speak through Voicebox.",
                required = true
        )
        String text,

        @McpInputField(
                value = "profile",
                description = "Optional Voicebox profile name or id. Falls back to the Voicebox per-client/default binding.",
                required = false
        )
        String profile,

        @McpInputField(
                value = "engine",
                description = "Optional Voicebox engine: qwen, qwen_custom_voice, luxtts, chatterbox, chatterbox_turbo, tada, or kokoro.",
                required = false
        )
        String engine,

        @McpInputField(
                value = "personality",
                description = "Whether Voicebox should rewrite the text through the selected profile personality before TTS.",
                required = false
        )
        Boolean personality,

        @McpInputField(
                value = "language",
                description = "Optional language code. Defaults to Voicebox's en behavior when omitted.",
                required = false
        )
        String language
) {
}

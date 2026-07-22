package org.toolspace.fasterwhisper;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

import java.util.List;
import java.util.Map;

/**
 * Optional values override the configured generation defaults for this one non-mutating call.
 */
public record FasterWhisperTranscribeArgs(
        @McpInputField(value = "audioPath", description = "Local audio file to transcribe.", required = true)
        String audioPath,

        @McpInputField(value = "language", description = "Optional BCP-47 language code; omit for detection.", required = false)
        String language,

        @McpInputField(value = "task", description = "transcribe or translate.", required = false)
        String task,

        @McpInputField(value = "beamSize", description = "Beam-search width.", required = false)
        Integer beamSize,

        @McpInputField(value = "bestOf", description = "Sampling candidate count.", required = false)
        Integer bestOf,

        @McpInputField(value = "patience", description = "Beam-search patience factor.", required = false)
        Double patience,

        @McpInputField(value = "lengthPenalty", description = "Decoded length penalty.", required = false)
        Double lengthPenalty,

        @McpInputField(value = "repetitionPenalty", description = "Previously generated token penalty.", required = false)
        Double repetitionPenalty,

        @McpInputField(value = "noRepeatNgramSize", description = "N-gram repetition prevention size.", required = false)
        Integer noRepeatNgramSize,

        @McpInputField(value = "temperature", description = "Fallback sampling temperatures.", required = false)
        List<Double> temperature,

        @McpInputField(value = "compressionRatioThreshold", description = "Compression-ratio failure threshold.", required = false)
        Double compressionRatioThreshold,

        @McpInputField(value = "logProbThreshold", description = "Average log-probability failure threshold.", required = false)
        Double logProbThreshold,

        @McpInputField(value = "noSpeechThreshold", description = "No-speech probability threshold.", required = false)
        Double noSpeechThreshold,

        @McpInputField(value = "conditionOnPreviousText", description = "Feed prior window text into the next window.", required = false)
        Boolean conditionOnPreviousText,

        @McpInputField(value = "promptResetOnTemperature", description = "Temperature at which the prompt resets.", required = false)
        Double promptResetOnTemperature,

        @McpInputField(value = "initialPrompt", description = "Initial model prompt.", required = false)
        String initialPrompt,

        @McpInputField(value = "prefix", description = "Per-window text prefix.", required = false)
        String prefix,

        @McpInputField(value = "suppressBlank", description = "Suppress initial blank output.", required = false)
        Boolean suppressBlank,

        @McpInputField(value = "suppressTokens", description = "Token IDs to suppress.", required = false)
        List<Integer> suppressTokens,

        @McpInputField(value = "withoutTimestamps", description = "Return text without timestamp tokens.", required = false)
        Boolean withoutTimestamps,

        @McpInputField(value = "maxInitialTimestamp", description = "Maximum initial timestamp.", required = false)
        Double maxInitialTimestamp,

        @McpInputField(value = "wordTimestamps", description = "Extract word-level timestamps.", required = false)
        Boolean wordTimestamps,

        @McpInputField(value = "prependPunctuations", description = "Punctuation attached to following words.", required = false)
        String prependPunctuations,

        @McpInputField(value = "appendPunctuations", description = "Punctuation attached to preceding words.", required = false)
        String appendPunctuations,

        @McpInputField(value = "multilingual", description = "Detect language for every segment.", required = false)
        Boolean multilingual,

        @McpInputField(value = "vadFilter", description = "Enable Silero voice-activity filtering.", required = false)
        Boolean vadFilter,

        @McpInputField(value = "vadParameters", description = "Silero VAD options.", required = false)
        Map<String, Object> vadParameters,

        @McpInputField(value = "maxNewTokens", description = "Maximum generated tokens per chunk.", required = false)
        Integer maxNewTokens,

        @McpInputField(value = "chunkLength", description = "Audio chunk length in seconds.", required = false)
        Integer chunkLength,

        @McpInputField(value = "clipTimestamps", description = "Optional clip boundary timestamps in seconds.", required = false)
        List<Double> clipTimestamps,

        @McpInputField(value = "hallucinationSilenceThreshold", description = "Silent-period hallucination guard in seconds.", required = false)
        Double hallucinationSilenceThreshold,

        @McpInputField(value = "hotwords", description = "Hint phrases; ignored when prefix is supplied.", required = false)
        String hotwords,

        @McpInputField(value = "languageDetectionThreshold", description = "Language-detection confidence threshold.", required = false)
        Double languageDetectionThreshold,

        @McpInputField(value = "languageDetectionSegments", description = "Segments used for language detection.", required = false)
        Integer languageDetectionSegments

) {
}

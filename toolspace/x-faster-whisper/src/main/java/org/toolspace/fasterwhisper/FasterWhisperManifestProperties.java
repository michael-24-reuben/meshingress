package org.toolspace.fasterwhisper;

import dev.mrk.meshingress.toolmetadata.ToolProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Runtime binding and the canonical manifest property schema for this tool module.
 */
@ConfigurationProperties(prefix = "meshingress.faster-whisper")
public record FasterWhisperManifestProperties(
        String pythonExecutable,
        String bridgeScript,
        String bridgeRuntimeRoot,
        Long timeoutMs,
        Model model,
        Generation generation
) {
    public FasterWhisperManifestProperties {
        pythonExecutable = blankToDefault(pythonExecutable,
                "toolspace/x-faster-whisper/.venv/Scripts/python.exe");
        bridgeRuntimeRoot = blankToDefault(bridgeRuntimeRoot,
                System.getProperty("java.io.tmpdir") + "/meshingress/faster-whisper");
        timeoutMs = timeoutMs == null || timeoutMs <= 0 ? 300_000L : Math.min(timeoutMs, 900_000L);
        model = model == null ? Model.defaults() : model;
        generation = generation == null ? Generation.defaults() : generation;
    }

    public static List<ToolProperty> manifestProperties() {
        return List.of(
                property("python-executable", "Python interpreter for the verified provisioned environment.", "", "string", true),
                property("bridge-script", "Optional path overriding the packaged Python bridge script.", "", "string", false),
                property("bridge-runtime-root", "Directory used to materialize the packaged Python bridge.", "${java.io.tmpdir}/meshingress/faster-whisper", "string", false),
                property("timeout-ms", "Maximum bridge execution time in milliseconds.", 300_000L, "long", false),
                property("model.name", "Local Faster Whisper model name or local model path.", "small", "string", false),
                property("model.device", "Inference device, such as auto, cpu, or cuda.", "auto", "string", false),
                property("model.device-indexes", "Optional ordered accelerator device indexes.", "", "integer-list", false),
                property("model.compute-type", "CTranslate2 compute type.", "default", "string", false),
                property("model.cpu-threads", "CPU thread count; zero delegates selection to the runtime.", 0, "long", false),
                property("model.num-workers", "Number of parallel model workers.", 1, "long", false),
                property("model.download-root", "Local model-cache root; tool calls never download models.", "", "string", false),
                property("model.local-files-only", "Require models to already be local during tool calls.", true, "boolean", false),
                property("model.revision", "Optional local model revision selector.", "", "string", false),
                property("generation.task", "Default Whisper task.", "transcribe", "string", false),
                property("generation.beam-size", "Beam-search width.", 5, "long", false),
                property("generation.best-of", "Candidate count for nonzero-temperature decoding.", 5, "long", false),
                property("generation.patience", "Beam-search patience.", 1.0, "number", false),
                property("generation.length-penalty", "Sequence length penalty.", 1.0, "number", false),
                property("generation.repetition-penalty", "Token repetition penalty.", 1.0, "number", false),
                property("generation.no-repeat-ngram-size", "N-gram repetition guard; zero disables it.", 0, "long", false),
                property("generation.temperature", "Ordered decoder temperatures.", "0.0", "number-list", false),
                property("generation.compression-ratio-threshold", "Compression-ratio fallback threshold.", 2.4, "number", false),
                property("generation.log-prob-threshold", "Log-probability fallback threshold.", -1.0, "number", false),
                property("generation.no-speech-threshold", "No-speech probability threshold.", 0.6, "number", false),
                property("generation.condition-on-previous-text", "Use the previous segment as decoder context.", true, "boolean", false),
                property("generation.prompt-reset-on-temperature", "Temperature at which decoder prompt context resets.", 0.5, "number", false),
                property("generation.suppress-blank", "Suppress blank decoder tokens.", true, "boolean", false),
                property("generation.suppress-tokens", "Suppressed decoder token IDs.", "-1", "integer-list", false),
                property("generation.without-timestamps", "Omit timestamp token generation.", false, "boolean", false),
                property("generation.max-initial-timestamp", "Maximum initial timestamp in seconds.", 1.0, "number", false),
                property("generation.word-timestamps", "Include word-level timestamps.", false, "boolean", false),
                property("generation.prepend-punctuations", "Punctuation attached to following words in word timestamps.", "\"'“¿([{-", "string", false),
                property("generation.append-punctuations", "Punctuation attached to preceding words in word timestamps.", "\"'.。，，!！?？:：”)]}、", "string", false),
                property("generation.multilingual", "Treat the configured local model as multilingual.", false, "boolean", false),
                property("generation.vad-filter", "Enable voice-activity filtering.", false, "boolean", false),
                property("generation.max-new-tokens", "Optional maximum generated tokens per segment.", "", "long", false),
                property("generation.chunk-length", "Optional audio chunk length in seconds.", "", "long", false),
                property("generation.hallucination-silence-threshold", "Optional silence threshold for hallucination filtering.", "", "number", false),
                property("generation.batch-size", "Batch size for batched transcription.", 8, "long", false),
                property("generation.language-detection-threshold", "Language-detection confidence threshold.", 0.5, "number", false),
                property("generation.language-detection-segments", "Segments sampled for language detection.", 1, "long", false)
        );
    }

    private static ToolProperty property(String suffix, String description, Object defaultValue, String valueType, boolean required) {
        return ToolProperty.string("meshingress.faster-whisper." + suffix)
                .description(description)
                .defaultValue(defaultValue)
                .valueType(valueType)
                .required(required);
    }

    public record Model(
            String name,
            String device,
            List<Integer> deviceIndexes,
            String computeType,
            Integer cpuThreads,
            Integer numWorkers,
            String downloadRoot,
            Boolean localFilesOnly,
            String revision
    ) {
        public Model {
            name = blankToDefault(name, "small");
            device = blankToDefault(device, "auto");
            computeType = blankToDefault(computeType, "default");
            deviceIndexes = deviceIndexes == null ? List.of() : List.copyOf(deviceIndexes);
            cpuThreads = cpuThreads == null ? 0 : Math.max(cpuThreads, 0);
            numWorkers = numWorkers == null ? 1 : Math.max(numWorkers, 1);
            localFilesOnly = localFilesOnly == null || localFilesOnly;
        }

        static Model defaults() {
            return new Model("small", "auto", List.of(), "default", 0, 1, "", true, "");
        }
    }

    public record Generation(
            String task,
            Integer beamSize,
            Integer bestOf,
            Double patience,
            Double lengthPenalty,
            Double repetitionPenalty,
            Integer noRepeatNgramSize,
            List<Double> temperature,
            Double compressionRatioThreshold,
            Double logProbThreshold,
            Double noSpeechThreshold,
            Boolean conditionOnPreviousText,
            Double promptResetOnTemperature,
            Boolean suppressBlank,
            List<Integer> suppressTokens,
            Boolean withoutTimestamps,
            Double maxInitialTimestamp,
            Boolean wordTimestamps,
            String prependPunctuations,
            String appendPunctuations,
            Boolean multilingual,
            Boolean vadFilter,
            Integer maxNewTokens,
            Integer chunkLength,
            Double hallucinationSilenceThreshold,
            Integer batchSize,
            Double languageDetectionThreshold,
            Integer languageDetectionSegments
    ) {
        public Generation {
            task = blankToDefault(task, "transcribe");
            beamSize = defaultInt(beamSize, 5);
            bestOf = defaultInt(bestOf, 5);
            patience = defaultDouble(patience, 1.0);
            lengthPenalty = defaultDouble(lengthPenalty, 1.0);
            repetitionPenalty = defaultDouble(repetitionPenalty, 1.0);
            noRepeatNgramSize = defaultInt(noRepeatNgramSize, 0);
            temperature = temperature == null || temperature.isEmpty() ? List.of(0.0) : List.copyOf(temperature);
            compressionRatioThreshold = defaultDouble(compressionRatioThreshold, 2.4);
            logProbThreshold = defaultDouble(logProbThreshold, -1.0);
            noSpeechThreshold = defaultDouble(noSpeechThreshold, 0.6);
            conditionOnPreviousText = conditionOnPreviousText == null || conditionOnPreviousText;
            promptResetOnTemperature = defaultDouble(promptResetOnTemperature, 0.5);
            suppressBlank = suppressBlank == null || suppressBlank;
            suppressTokens = suppressTokens == null ? List.of(-1) : List.copyOf(suppressTokens);
            withoutTimestamps = withoutTimestamps != null && withoutTimestamps;
            maxInitialTimestamp = defaultDouble(maxInitialTimestamp, 1.0);
            wordTimestamps = wordTimestamps != null && wordTimestamps;
            prependPunctuations = blankToDefault(prependPunctuations, "\"'“¿([{-");
            appendPunctuations = blankToDefault(appendPunctuations, "\"'.。，，!！?？:：”)]}、");
            multilingual = multilingual != null && multilingual;
            vadFilter = vadFilter != null && vadFilter;
            batchSize = defaultInt(batchSize, 8);
            languageDetectionThreshold = defaultDouble(languageDetectionThreshold, 0.5);
            languageDetectionSegments = defaultInt(languageDetectionSegments, 1);
        }

        static Generation defaults() {
            return new Generation("transcribe", 5, 5, 1.0, 1.0, 1.0, 0, List.of(0.0), 2.4, -1.0, 0.6,
                    true, 0.5, true, List.of(-1), false, 1.0, false, "\"'“¿([{-", "\"'.。，，!！?？:：”)]}、",
                    false, false, null, null, null, 8, 0.5, 1);
        }
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static Integer defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private static Double defaultDouble(Double value, double fallback) {
        return value == null ? fallback : value;
    }
}

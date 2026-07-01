package dev.mrk.toolspace.omnivoice;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.result.ResultContent;
import dev.mrk.meshingress.api.tools.annotation.McpConfigureMapping;
import dev.mrk.meshingress.api.tools.annotation.McpFunction;
import dev.mrk.meshingress.api.tools.annotation.McpInputField;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.api.tools.annotation.McpToolMapping;
import dev.mrk.meshingress.api.tools.annotation.McpToolScopes;
import dev.mrk.meshingress.dispatch.tool.ToolResultContent;
import dev.mrk.meshingress.scopes.McpToolScope;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@McpTool(
        value = "omnivoice",
        title = "OmniVoice TTS",
        description = "Guarded JSON-wrapper boundary for k2-fsa/OmniVoice text-to-speech, voice design, voice cloning, model management, and local voice assets.",
        defaultFunction = "generate"
)
@McpToolMapping("tools")
@McpToolScopes({
        McpToolScope.PROCESS_EXECUTE,
        McpToolScope.FILES_READ,
        McpToolScope.FILES_WRITE,
        McpToolScope.FILES_LIST
})
public class OmniVoiceTool {

    private static final String UPSTREAM_REPOSITORY = "https://github.com/k2-fsa/OmniVoice";
    private static final String UPSTREAM_MODEL = "https://huggingface.co/k2-fsa/OmniVoice";
    private static final String AUDIO_TOKENIZER_MODEL = "eustlb/higgs-audio-v2-tokenizer";
    private static final String ASR_MODEL = "openai/whisper-large-v3-turbo";
    private static final int MAX_CAPTURED_OUTPUT = 12_000;

    private final ObjectMapper objectMapper;
    private final OmniVoiceRunner runner;
    private final OmniVoiceConfig config;
    private final Path outputRoot;
    private final Path referenceRoot;
    private final Path voiceAssetRoot;

    public OmniVoiceTool(ObjectMapper objectMapper, OmniVoiceRunner runner, OmniVoiceConfig config) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.runner = Objects.requireNonNull(runner, "runner must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.outputRoot = config.outputRoot().toAbsolutePath().normalize();
        this.referenceRoot = config.referenceRoot().toAbsolutePath().normalize();
        this.voiceAssetRoot = config.voiceAssetRoot().toAbsolutePath().normalize();
    }

    @McpConfigureMapping(timeoutMs = 15_000, audit = true)
    @McpFunction(
            value = "runtime.health",
            title = "OmniVoice Runtime Health",
            description = "Return wrapper configuration and optionally probe the configured OmniVoice JSON CLI."
    )
    @McpToolScopes({
            McpToolScope.HEALTH_CHECK,
            McpToolScope.RUNTIME_READ
    })
    public DispatchExecutionResult runtimeHealth(OmniVoiceRuntimeHealthArgs args, McpCallContext context) {
        ObjectNode structured = baseResponse(false);
        addConfiguration(structured);
        boolean probeCommand = args != null && enabled(args.probeCommand());
        structured.put("available", false);
        structured.put("probeAttempted", probeCommand);

        if (!probeCommand) {
            structured.put("available", !config.command().isEmpty());
            structured.put("message", "Command probe skipped. Pass probeCommand=true to call the configured wrapper.");
            return ok("OmniVoice runtime configuration loaded.", structured);
        }

        try {
            ObjectNode request = baseWrapperRequest("runtime.health");
            OmniVoiceCommandResult result = runner.run(config.command(), request, outputRoot, Duration.ofSeconds(15));
            structured.set("commandResult", commandResultJson(result));
            structured.put("available", !result.timedOut() && result.exitCode() == 0);
            return result.exitCode() == 0 && !result.timedOut()
                    ? ok("OmniVoice runtime health probe completed.", structured)
                    : failure("OMNIVOICE_RUNTIME_UNAVAILABLE", "OmniVoice runtime probe failed.", structured);
        } catch (Exception exception) {
            return failure("OMNIVOICE_RUNTIME_UNAVAILABLE", "OmniVoice runtime probe failed.", exception, structured);
        }
    }

    @McpConfigureMapping(timeoutMs = 600_000, audit = true, debugTrace = true)
    @McpFunction(
            value = "generate",
            title = "Generate OmniVoice Speech",
            description = "Generate speech from text using the configured OmniVoice JSON CLI without a reference voice."
    )
    @McpToolScopes({
            McpToolScope.PROCESS_EXECUTE,
            McpToolScope.FILES_WRITE
    })
    public DispatchExecutionResult generate(OmniVoiceGenerateArgs args, McpCallContext context) {
        try {
            validateText(args == null ? null : args.text());
            Path outputPath = resolveOutputPath(args.outputPath(), "generate");
            ObjectNode request = generationRequest("generate", args.text(), outputPath, args.model(), args.languageId(), args.duration(), args.speed(), args.numStep(), args.device(), args.dtype());
            return runWrapper("OmniVoice speech generation completed.", request, true);
        } catch (Exception exception) {
            return failure("OMNIVOICE_GENERATE_FAILED", "OmniVoice generate failed.", exception, null);
        }
    }

    @McpConfigureMapping(timeoutMs = 600_000, audit = true, debugTrace = true)
    @McpFunction(
            value = "design",
            title = "Design OmniVoice Speech",
            description = "Generate speech from text and an OmniVoice voice-design instruction."
    )
    @McpToolScopes({
            McpToolScope.PROCESS_EXECUTE,
            McpToolScope.FILES_WRITE
    })
    public DispatchExecutionResult design(OmniVoiceDesignArgs args, McpCallContext context) {
        try {
            validateText(args == null ? null : args.text());
            if (args.instruct() == null || args.instruct().isBlank()) {
                throw new IllegalArgumentException("instruct is required");
            }
            Path outputPath = resolveOutputPath(args.outputPath(), "design");
            ObjectNode request = generationRequest("design", args.text(), outputPath, args.model(), args.languageId(), args.duration(), args.speed(), args.numStep(), args.device(), args.dtype());
            request.put("instruct", args.instruct().trim());
            return runWrapper("OmniVoice voice design completed.", request, true);
        } catch (Exception exception) {
            return failure("OMNIVOICE_DESIGN_FAILED", "OmniVoice design failed.", exception, null);
        }
    }

    @McpConfigureMapping(timeoutMs = 600_000, audit = true, debugTrace = true)
    @McpFunction(
            value = "clone",
            title = "Clone Voice With OmniVoice",
            description = "Generate speech from text using a constrained local reference audio file and optional reference transcript."
    )
    @McpToolScopes({
            McpToolScope.PROCESS_EXECUTE,
            McpToolScope.FILES_READ,
            McpToolScope.FILES_WRITE
    })
    public DispatchExecutionResult cloneVoice(OmniVoiceCloneArgs args, McpCallContext context) {
        try {
            validateText(args == null ? null : args.text());
            Path referenceAudio = resolveReferenceAudio(args.referenceAudioPath());
            Path outputPath = resolveOutputPath(args.outputPath(), "clone");
            ObjectNode request = generationRequest("clone", args.text(), outputPath, args.model(), args.languageId(), args.duration(), args.speed(), args.numStep(), args.device(), args.dtype());
            request.put("referenceAudioPath", referenceAudio.toString());
            if (args.referenceText() != null && !args.referenceText().isBlank()) {
                request.put("referenceText", args.referenceText().trim());
            }
            request.put("allowAutoTranscribe", Boolean.TRUE.equals(args.allowAutoTranscribe()));
            return runWrapper("OmniVoice voice clone completed.", request, true);
        } catch (Exception exception) {
            return failure("OMNIVOICE_CLONE_FAILED", "OmniVoice clone failed.", exception, null);
        }
    }

    @McpConfigureMapping(timeoutMs = 30_000, audit = true)
    @McpFunction(
            value = "models.status",
            title = "OmniVoice Model Status",
            description = "Return configured OmniVoice model identifiers and local cache hints without pulling models."
    )
    @McpToolScopes({
            McpToolScope.RUNTIME_READ,
            McpToolScope.FILES_METADATA_READ
    })
    public DispatchExecutionResult modelsStatus(OmniVoiceModelsStatusArgs args, McpCallContext context) {
        ObjectNode structured = baseResponse(false);
        addModelMatrix(structured, modelOrDefault(args == null ? null : args.model()));
        structured.put("modelPullEnabled", config.modelPullEnabled());
        structured.put("automaticDownloadControlledByWrapper", true);
        structured.put("cachePath", "Hugging Face cache or wrapper-specific local cache");
        return ok("OmniVoice model status loaded.", structured);
    }

    @McpConfigureMapping(timeoutMs = 1_800_000, audit = true, debugTrace = true)
    @McpFunction(
            value = "models.pull",
            title = "Pull OmniVoice Models",
            description = "Ask the configured OmniVoice wrapper to prefetch models. Disabled by default."
    )
    @McpToolScopes({
            McpToolScope.PROCESS_EXECUTE,
            McpToolScope.HTTP_CLIENT,
            McpToolScope.EXTERNAL_API_READ,
            McpToolScope.FILES_WRITE
    })
    public DispatchExecutionResult modelsPull(OmniVoiceModelsPullArgs args, McpCallContext context) {
        if (!config.modelPullEnabled()) {
            ObjectNode structured = baseResponse(false);
            structured.put("modelPullEnabled", false);
            structured.put("message", "Model pull is disabled by meshingress.omnivoice.model-pull-enabled=false.");
            return failure("OMNIVOICE_MODEL_PULL_DISABLED", "OmniVoice model pull is disabled.", structured);
        }
        try {
            ObjectNode request = baseWrapperRequest("models.pull");
            request.put("model", modelOrDefault(args == null ? null : args.model()));
            request.put("audioTokenizerModel", valueOrDefault(args == null ? null : args.audioTokenizerModel(), AUDIO_TOKENIZER_MODEL));
            request.put("asrModel", valueOrDefault(args == null ? null : args.asrModel(), ASR_MODEL));
            request.put("includeAsrModel", args != null && enabled(args.includeAsrModel()));
            request.put("force", args != null && enabled(args.force()));
            return runWrapper("OmniVoice model pull completed.", request, true);
        } catch (Exception exception) {
            return failure("OMNIVOICE_MODEL_PULL_FAILED", "OmniVoice model pull failed.", exception, null);
        }
    }

    @McpConfigureMapping(timeoutMs = 30_000, audit = true)
    @McpFunction(
            value = "voices.providers",
            title = "List OmniVoice Voice Providers",
            description = "List configured voice provider adapters. Provider downloads are disabled by default."
    )
    @McpToolScopes(McpToolScope.RUNTIME_READ)
    public DispatchExecutionResult voicesProviders(OmniVoiceProvidersArgs args, McpCallContext context) {
        ObjectNode structured = baseResponse(false);
        structured.put("providerDownloadsEnabled", config.providerDownloadsEnabled());
        ArrayNode providers = objectMapper.createArrayNode();
        ObjectNode local = objectMapper.createObjectNode();
        local.put("id", "local");
        local.put("displayName", "Local reference audio import");
        local.put("enabled", true);
        local.put("requiresAuth", false);
        local.put("policy", "Only user-provided local reference audio under the configured reference root is supported.");
        ArrayNode capabilities = objectMapper.createArrayNode();
        capabilities.add("import");
        local.set("capabilities", capabilities);
        providers.add(local);
        structured.set("providers", providers);
        structured.put("unsupportedProviderPolicy", "Unknown provider adapters are rejected unless implemented in the external wrapper and enabled by configuration.");
        return ok("OmniVoice voice providers loaded.", structured);
    }

    @McpConfigureMapping(timeoutMs = 60_000, audit = true, debugTrace = true)
    @McpFunction(
            value = "voices.import",
            title = "Import Local OmniVoice Voice Asset",
            description = "Normalize a local reference audio file into the configured OmniVoice voice asset store."
    )
    @McpToolScopes({
            McpToolScope.FILES_READ,
            McpToolScope.FILES_WRITE,
            McpToolScope.AUDIT_APPEND
    })
    public DispatchExecutionResult voicesImport(OmniVoiceImportArgs args, McpCallContext context) {
        try {
            if (args == null) {
                throw new IllegalArgumentException("arguments are required");
            }
            Path referenceAudio = resolveReferenceAudio(args.referenceAudioPath());
            String voiceId = safeId(valueOrDefault(args.voiceId(), "local-" + UUID.randomUUID()));
            Path voiceDirectory = voiceAssetRoot.resolve("local").resolve(voiceId).toAbsolutePath().normalize();
            if (!voiceDirectory.startsWith(voiceAssetRoot)) {
                throw new IllegalArgumentException("voiceId resolves outside the configured voice asset root");
            }
            Files.createDirectories(voiceDirectory);

            Path copiedReference = voiceDirectory.resolve("reference" + extension(referenceAudio)).normalize();
            Files.copy(referenceAudio, copiedReference, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            ObjectNode metadata = voiceMetadata(args, voiceId, copiedReference);
            objectMapper.writeValue(voiceDirectory.resolve("voice.json").toFile(), metadata);
            objectMapper.writeValue(voiceDirectory.resolve("metadata.json").toFile(), metadata);

            ObjectNode structured = baseResponse(true);
            structured.put("voiceId", voiceId);
            structured.put("voiceDirectory", voiceDirectory.toString());
            structured.put("referenceAudioPath", copiedReference.toString());
            structured.set("metadata", metadata);
            return ok("OmniVoice local voice asset imported.", structured);
        } catch (Exception exception) {
            return failure("OMNIVOICE_VOICE_IMPORT_FAILED", "OmniVoice voice import failed.", exception, null);
        }
    }

    @McpConfigureMapping(timeoutMs = 600_000, audit = true, debugTrace = true)
    @McpFunction(
            value = "voices.download",
            title = "Download OmniVoice Voice Asset",
            description = "Ask an explicitly enabled provider adapter to download or import a voice asset. Disabled by default."
    )
    @McpToolScopes({
            McpToolScope.PROCESS_EXECUTE,
            McpToolScope.HTTP_CLIENT,
            McpToolScope.EXTERNAL_API_READ,
            McpToolScope.FILES_WRITE,
            McpToolScope.AUDIT_APPEND
    })
    public DispatchExecutionResult voicesDownload(OmniVoiceDownloadArgs args, McpCallContext context) {
        if (!config.providerDownloadsEnabled()) {
            ObjectNode structured = baseResponse(false);
            structured.put("providerDownloadsEnabled", false);
            structured.put("message", "Provider-based voice download is disabled by meshingress.omnivoice.provider-downloads-enabled=false.");
            return failure("OMNIVOICE_PROVIDER_DOWNLOAD_DISABLED", "OmniVoice provider download is disabled.", structured);
        }
        try {
            if (args == null || args.providerId() == null || args.providerId().isBlank()) {
                throw new IllegalArgumentException("providerId is required");
            }
            ObjectNode request = baseWrapperRequest("voices.download");
            request.put("providerId", args.providerId().trim());
            putIfPresent(request, "voiceId", args.voiceId());
            putIfPresent(request, "outputVoiceId", args.outputVoiceId());
            request.put("voiceAssetRoot", voiceAssetRoot.toString());
            return runWrapper("OmniVoice provider voice download completed.", request, true);
        } catch (Exception exception) {
            return failure("OMNIVOICE_PROVIDER_DOWNLOAD_FAILED", "OmniVoice provider download failed.", exception, null);
        }
    }

    private DispatchExecutionResult runWrapper(String summary, ObjectNode request, boolean mutating)
            throws IOException, InterruptedException {
        Files.createDirectories(outputRoot);
        Files.createDirectories(voiceAssetRoot);
        OmniVoiceCommandResult result = runner.run(config.command(), request, outputRoot, config.timeout());
        JsonNode parsed = parseStdout(result.stdout());

        ObjectNode structured = baseResponse(mutating);
        structured.set("request", sanitizedRequest(request));
        structured.set("wrapperResponse", parsed);
        structured.set("commandResult", commandResultJson(result));

        if (result.timedOut()) {
            return failure("OMNIVOICE_TIMEOUT", "OmniVoice wrapper timed out.", structured);
        }
        if (result.exitCode() != 0) {
            return failure("OMNIVOICE_WRAPPER_FAILED", "OmniVoice wrapper exited with code " + result.exitCode() + ".", structured);
        }
        structured.put("ok", true);
        return DispatchExecutionResult.builder()
                .appendContent(ResultContent.json(parsed))
                .structuredContent(toolResult(structured))
                .status("ok")
                .summary(summary)
                .build();
    }

    private ObjectNode generationRequest(
            String operation,
            String text,
            Path outputPath,
            String model,
            String languageId,
            Double duration,
            Double speed,
            Integer numStep,
            String device,
            String dtype
    ) {
        ObjectNode request = baseWrapperRequest(operation);
        request.put("model", modelOrDefault(model));
        request.put("text", text.trim());
        request.put("outputPath", outputPath.toString());
        putIfPresent(request, "languageId", languageId);
        putIfPresent(request, "device", device);
        putIfPresent(request, "dtype", dtype);
        if (duration != null) {
            request.put("duration", duration);
        }
        if (speed != null) {
            request.put("speed", speed);
        }
        if (numStep != null) {
            request.put("numStep", numStep);
        }
        return request;
    }

    private ObjectNode baseWrapperRequest(String operation) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("operation", operation);
        request.put("outputRoot", outputRoot.toString());
        request.put("referenceRoot", referenceRoot.toString());
        request.put("voiceAssetRoot", voiceAssetRoot.toString());
        request.put("createdAt", Instant.now().toString());
        return request;
    }

    private ObjectNode baseResponse(boolean mutating) {
        ObjectNode structured = objectMapper.createObjectNode();
        structured.put("ok", false);
        structured.put("mutating", mutating);
        structured.put("receivedAt", Instant.now().toString());
        structured.put("upstreamRepository", UPSTREAM_REPOSITORY);
        structured.put("upstreamModel", UPSTREAM_MODEL);
        structured.put("policy", "Voice cloning requires permission for the reference voice. Provider downloads are disabled unless explicitly configured.");
        return structured;
    }

    private DispatchExecutionResult ok(String summary, ObjectNode structured) {
        structured.put("ok", true);
        return DispatchExecutionResult.builder()
                .appendContent(ResultContent.json(structured))
                .structuredContent(toolResult(structured))
                .status("ok")
                .summary(summary)
                .build();
    }

    private DispatchExecutionResult failure(String code, String summary, Exception exception, ObjectNode structured) {
        ObjectNode body = structured == null ? baseResponse(false) : structured;
        body.put("ok", false);
        body.put("exceptionType", exception.getClass().getName());
        body.put("message", exception.getMessage() == null ? "" : exception.getMessage());
        return failure(code, exception.getMessage() == null ? summary : exception.getMessage(), body);
    }

    private DispatchExecutionResult failure(String code, String message, ObjectNode structured) {
        structured.put("ok", false);
        return DispatchExecutionResult.builder()
                .structuredContent(toolResult(structured))
                .error(code, message)
                .status("failed")
                .summary(message)
                .build();
    }

    private Path resolveOutputPath(String requested, String operation) throws IOException {
        Files.createDirectories(outputRoot);
        String value = requested == null || requested.isBlank()
                ? operation + "-" + Instant.now().toEpochMilli() + ".wav"
                : requested.trim();
        Path raw = Path.of(value);
        Path resolved = raw.isAbsolute()
                ? raw.toAbsolutePath().normalize()
                : outputRoot.resolve(raw).toAbsolutePath().normalize();
        if (!resolved.startsWith(outputRoot)) {
            throw new IllegalArgumentException("outputPath must stay inside the configured OmniVoice output root");
        }
        Files.createDirectories(resolved.getParent());
        return resolved;
    }

    private Path resolveReferenceAudio(String value) throws IOException {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("referenceAudioPath is required");
        }
        Path raw = Path.of(value.trim());
        Path resolved = raw.isAbsolute()
                ? raw.toAbsolutePath().normalize()
                : referenceRoot.resolve(raw).toAbsolutePath().normalize();
        if (!resolved.startsWith(referenceRoot)) {
            throw new IllegalArgumentException("referenceAudioPath must stay inside the configured OmniVoice reference root");
        }
        if (!Files.isRegularFile(resolved)) {
            throw new IllegalArgumentException("referenceAudioPath is not a readable file: " + resolved);
        }
        return resolved;
    }

    private void validateText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text is required");
        }
    }

    private void addConfiguration(ObjectNode structured) {
        structured.set("command", array(config.command()));
        structured.put("outputRoot", outputRoot.toString());
        structured.put("referenceRoot", referenceRoot.toString());
        structured.put("voiceAssetRoot", voiceAssetRoot.toString());
        structured.put("defaultModel", config.defaultModel());
        structured.put("timeoutSeconds", config.timeout().toSeconds());
        structured.put("providerDownloadsEnabled", config.providerDownloadsEnabled());
        structured.put("modelPullEnabled", config.modelPullEnabled());
    }

    private ToolResultContent toolResult(ObjectNode structured) {
        ToolResultContent result = new ToolResultContent();
        result.setTool("omnivoice");
        result.setOk(booleanValue(structured, "ok"));
        result.setMutating(booleanValue(structured, "mutating"));
        result.setReceivedAt(textValue(structured, "receivedAt"));
        result.setMessage(textValue(structured, "message"));
        result.setExceptionType(textValue(structured, "exceptionType"));
        result.setUpstreamRepository(textValue(structured, "upstreamRepository"));
        result.setUpstreamModel(textValue(structured, "upstreamModel"));
        result.setPolicy(textValue(structured, "policy"));
        result.setRequest(structured.get("request"));
        result.setWrapperResponse(structured.get("wrapperResponse"));
        result.setResponse(structured.get("wrapperResponse"));
        result.setCommandResult(structured.get("commandResult"));
        result.setMetadata(structured.get("metadata"));
        result.setDetails(structured.deepCopy());

        JsonNode request = structured.get("request");
        if (request != null && request.get("operation") != null && request.get("operation").isTextual()) {
            result.setOperation(request.get("operation").asText());
        }

        return result;
    }

    private static boolean booleanValue(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isBoolean() && value.asBoolean();
    }

    private static String textValue(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || !value.isTextual() ? null : value.asText();
    }

    private void addModelMatrix(ObjectNode structured, String model) {
        ArrayNode models = objectMapper.createArrayNode();
        models.add(modelEntry(model, "huggingface", "basic TTS, voice design, voice cloning", false, "wrapper load or models.pull"));
        models.add(modelEntry(AUDIO_TOKENIZER_MODEL, "huggingface", "audio tokenization", false, "OmniVoice.from_pretrained when tokenizer is not bundled"));
        models.add(modelEntry(ASR_MODEL, "huggingface", "auto reference transcription", true, "clone when reference text is absent and ASR is enabled"));
        structured.set("models", models);
    }

    private ObjectNode modelEntry(String model, String provider, String requiredFor, boolean optional, String trigger) {
        ObjectNode entry = objectMapper.createObjectNode();
        entry.put("model", model);
        entry.put("provider", provider);
        entry.put("requiredFor", requiredFor);
        entry.put("optional", optional);
        entry.put("downloadTrigger", trigger);
        return entry;
    }

    private ObjectNode commandResultJson(OmniVoiceCommandResult result) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("exitCode", result.exitCode());
        node.put("timedOut", result.timedOut());
        node.put("stdout", truncate(result.stdout()));
        node.put("stderr", truncate(result.stderr()));
        node.set("command", array(result.command()));
        return node;
    }

    private JsonNode parseStdout(String stdout) throws IOException {
        if (stdout == null || stdout.isBlank()) {
            return objectMapper.createObjectNode();
        }
        String trimmed = stdout.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return objectMapper.readTree(trimmed);
        }
        return objectMapper.valueToTree(trimmed);
    }

    private ObjectNode sanitizedRequest(ObjectNode request) {
        ObjectNode copy = request.deepCopy();
        if (copy.has("referenceText")) {
            copy.put("referenceText", "<provided>");
        }
        return copy;
    }

    private ArrayNode array(List<String> values) {
        ArrayNode array = objectMapper.createArrayNode();
        values.forEach(array::add);
        return array;
    }

    private ObjectNode voiceMetadata(OmniVoiceImportArgs args, String voiceId, Path referenceAudioPath) {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("voiceId", voiceId);
        metadata.put("source", "local");
        metadata.putNull("providerId");
        metadata.put("displayName", valueOrDefault(args.displayName(), voiceId));
        metadata.put("referenceAudioPath", referenceAudioPath.toString());
        if (args.referenceText() == null || args.referenceText().isBlank()) {
            metadata.putNull("referenceText");
        } else {
            metadata.put("referenceText", args.referenceText().trim());
        }
        putNullable(metadata, "language", args.language());
        putNullable(metadata, "license", args.license());
        putNullable(metadata, "termsUrl", args.termsUrl());
        metadata.put("createdAt", Instant.now().toString());
        ArrayNode tags = objectMapper.createArrayNode();
        if (args.tags() != null) {
            for (String tag : args.tags()) {
                if (tag != null && !tag.isBlank()) {
                    tags.add(tag.trim());
                }
            }
        }
        metadata.set("tags", tags);
        metadata.set("usageRestrictions", objectMapper.createArrayNode());
        return metadata;
    }

    private void putIfPresent(ObjectNode node, String field, String value) {
        if (value != null && !value.isBlank()) {
            node.put(field, value.trim());
        }
    }

    private void putNullable(ObjectNode node, String field, String value) {
        if (value == null || value.isBlank()) {
            node.putNull(field);
        } else {
            node.put(field, value.trim());
        }
    }

    private String modelOrDefault(String model) {
        return valueOrDefault(model, config.defaultModel());
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String safeId(String value) {
        String safe = value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "-");
        if (safe.isBlank() || safe.equals(".") || safe.equals("..")) {
            throw new IllegalArgumentException("voiceId must contain at least one safe id character");
        }
        return safe;
    }

    private String extension(Path path) {
        String name = path.getFileName() == null ? "" : path.getFileName().toString();
        int index = name.lastIndexOf('.');
        if (index < 0 || index == name.length() - 1) {
            return ".wav";
        }
        return name.substring(index).replaceAll("[^A-Za-z0-9.]", "");
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() <= MAX_CAPTURED_OUTPUT) {
            return value;
        }
        return value.substring(0, MAX_CAPTURED_OUTPUT) + "\n... truncated ...";
    }

    private boolean enabled(Boolean value) {
        return Boolean.TRUE.equals(value);
    }
}

record OmniVoiceConfig(
        List<String> command,
        Path outputRoot,
        Path referenceRoot,
        Path voiceAssetRoot,
        String defaultModel,
        Duration timeout,
        boolean providerDownloadsEnabled,
        boolean modelPullEnabled
) {
    OmniVoiceConfig {
        command = command == null ? List.of("python", "toolspace/omnivoice-tts/runtime/omnivoice_wrapper.py", "--stdin") : List.copyOf(command);
    }
}

record OmniVoiceRuntimeHealthArgs(
        @McpInputField(value = "probeCommand", description = "When true, call the configured wrapper runtime.health operation.", required = false)
        Boolean probeCommand
) {
}

record OmniVoiceGenerateArgs(
        @McpInputField(value = "text", description = "Text to synthesize.", required = true)
        String text,
        @McpInputField(value = "outputPath", description = "Relative output path under the configured OmniVoice output root. Defaults to a generated WAV file.", required = false)
        String outputPath,
        @McpInputField(value = "model", description = "OmniVoice model id or local path. Defaults to meshingress.omnivoice.default-model.", required = false)
        String model,
        @McpInputField(value = "languageId", description = "Optional OmniVoice language id.", required = false)
        String languageId,
        @McpInputField(value = "duration", description = "Optional fixed output duration in seconds.", required = false)
        Double duration,
        @McpInputField(value = "speed", description = "Optional speech speed factor.", required = false)
        Double speed,
        @McpInputField(value = "numStep", description = "Optional diffusion step count.", required = false)
        Integer numStep,
        @McpInputField(value = "device", description = "Optional runtime device hint such as cuda:0, cpu, mps, or xpu.", required = false)
        String device,
        @McpInputField(value = "dtype", description = "Optional runtime dtype hint such as float16 or bfloat16.", required = false)
        String dtype
) {
}

record OmniVoiceDesignArgs(
        @McpInputField(value = "text", description = "Text to synthesize.", required = true)
        String text,
        @McpInputField(value = "instruct", description = "Voice design instruction, such as female, low pitch, british accent.", required = true)
        String instruct,
        @McpInputField(value = "outputPath", description = "Relative output path under the configured OmniVoice output root. Defaults to a generated WAV file.", required = false)
        String outputPath,
        @McpInputField(value = "model", description = "OmniVoice model id or local path. Defaults to meshingress.omnivoice.default-model.", required = false)
        String model,
        @McpInputField(value = "languageId", description = "Optional OmniVoice language id.", required = false)
        String languageId,
        @McpInputField(value = "duration", description = "Optional fixed output duration in seconds.", required = false)
        Double duration,
        @McpInputField(value = "speed", description = "Optional speech speed factor.", required = false)
        Double speed,
        @McpInputField(value = "numStep", description = "Optional diffusion step count.", required = false)
        Integer numStep,
        @McpInputField(value = "device", description = "Optional runtime device hint such as cuda:0, cpu, mps, or xpu.", required = false)
        String device,
        @McpInputField(value = "dtype", description = "Optional runtime dtype hint such as float16 or bfloat16.", required = false)
        String dtype
) {
}

record OmniVoiceCloneArgs(
        @McpInputField(value = "text", description = "Text to synthesize.", required = true)
        String text,
        @McpInputField(value = "referenceAudioPath", description = "Reference audio path under the configured OmniVoice reference root.", required = true)
        String referenceAudioPath,
        @McpInputField(value = "referenceText", description = "Optional transcript of the reference audio. If absent, the wrapper may use ASR only when allowAutoTranscribe is true.", required = false)
        String referenceText,
        @McpInputField(value = "allowAutoTranscribe", description = "Allow the wrapper to use ASR for missing referenceText.", required = false)
        Boolean allowAutoTranscribe,
        @McpInputField(value = "outputPath", description = "Relative output path under the configured OmniVoice output root. Defaults to a generated WAV file.", required = false)
        String outputPath,
        @McpInputField(value = "model", description = "OmniVoice model id or local path. Defaults to meshingress.omnivoice.default-model.", required = false)
        String model,
        @McpInputField(value = "languageId", description = "Optional OmniVoice language id.", required = false)
        String languageId,
        @McpInputField(value = "duration", description = "Optional fixed output duration in seconds.", required = false)
        Double duration,
        @McpInputField(value = "speed", description = "Optional speech speed factor.", required = false)
        Double speed,
        @McpInputField(value = "numStep", description = "Optional diffusion step count.", required = false)
        Integer numStep,
        @McpInputField(value = "device", description = "Optional runtime device hint such as cuda:0, cpu, mps, or xpu.", required = false)
        String device,
        @McpInputField(value = "dtype", description = "Optional runtime dtype hint such as float16 or bfloat16.", required = false)
        String dtype
) {
}

record OmniVoiceModelsStatusArgs(
        @McpInputField(value = "model", description = "Optional model id to report. Defaults to meshingress.omnivoice.default-model.", required = false)
        String model
) {
}

record OmniVoiceModelsPullArgs(
        @McpInputField(value = "model", description = "OmniVoice model id or local path. Defaults to meshingress.omnivoice.default-model.", required = false)
        String model,
        @McpInputField(value = "audioTokenizerModel", description = "Audio tokenizer model id. Defaults to eustlb/higgs-audio-v2-tokenizer.", required = false)
        String audioTokenizerModel,
        @McpInputField(value = "asrModel", description = "ASR model id for optional auto transcription.", required = false)
        String asrModel,
        @McpInputField(value = "includeAsrModel", description = "When true, include the optional ASR model in the pull request.", required = false)
        Boolean includeAsrModel,
        @McpInputField(value = "force", description = "Ask the wrapper to refresh cached model assets.", required = false)
        Boolean force
) {
}

record OmniVoiceProvidersArgs(
        @McpInputField(value = "includeDisabled", description = "Reserved for future provider adapter listing.", required = false)
        Boolean includeDisabled
) {
}

record OmniVoiceImportArgs(
        @McpInputField(value = "voiceId", description = "Optional stable voice id. Unsafe characters are normalized.", required = false)
        String voiceId,
        @McpInputField(value = "displayName", description = "Human-readable voice name.", required = false)
        String displayName,
        @McpInputField(value = "referenceAudioPath", description = "Reference audio path under the configured OmniVoice reference root.", required = true)
        String referenceAudioPath,
        @McpInputField(value = "referenceText", description = "Optional transcript of the reference audio.", required = false)
        String referenceText,
        @McpInputField(value = "language", description = "Optional language metadata.", required = false)
        String language,
        @McpInputField(value = "tags", description = "Optional metadata tags.", required = false)
        String[] tags,
        @McpInputField(value = "license", description = "Optional license metadata.", required = false)
        String license,
        @McpInputField(value = "termsUrl", description = "Optional terms URL metadata.", required = false)
        String termsUrl
) {
}

record OmniVoiceDownloadArgs(
        @McpInputField(value = "providerId", description = "Configured provider adapter id.", required = true)
        String providerId,
        @McpInputField(value = "voiceId", description = "Provider voice id.", required = false)
        String voiceId,
        @McpInputField(value = "outputVoiceId", description = "Optional local voice id for the downloaded asset.", required = false)
        String outputVoiceId
) {
}

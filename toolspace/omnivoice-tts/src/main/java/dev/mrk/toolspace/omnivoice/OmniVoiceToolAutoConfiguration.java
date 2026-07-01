package dev.mrk.toolspace.omnivoice;

import dev.mrk.meshingress.config.MeshingressProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@AutoConfiguration
public class OmniVoiceToolAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    OmniVoiceRunner omniVoiceRunner(ObjectMapper objectMapper) {
        return new ProcessOmniVoiceRunner(objectMapper);
    }

    @Bean
    OmniVoiceTool omniVoiceTool(
            ObjectMapper objectMapper,
            OmniVoiceRunner runner,
            MeshingressProperties properties,
            @Value("${meshingress.omnivoice.command:}") String command,
            @Value("${meshingress.omnivoice.output-root:./var/meshingress/omnivoice/outputs}") String outputRoot,
            @Value("${meshingress.omnivoice.reference-root:./var/meshingress/omnivoice/reference-audio}") String referenceRoot,
            @Value("${meshingress.omnivoice.voice-asset-root:./var/meshingress/omnivoice/voice-assets}") String voiceAssetRoot,
            @Value("${meshingress.omnivoice.default-model:k2-fsa/OmniVoice}") String defaultModel,
            @Value("${meshingress.omnivoice.timeout-seconds:900}") long timeoutSeconds,
            @Value("${meshingress.omnivoice.provider-downloads-enabled:false}") boolean providerDownloadsEnabled,
            @Value("${meshingress.omnivoice.model-pull-enabled:false}") boolean modelPullEnabled
    ) {
        OmniVoiceConfig config = new OmniVoiceConfig(
                splitCommand(command, properties),
                Path.of(outputRoot),
                Path.of(referenceRoot),
                Path.of(voiceAssetRoot),
                defaultModel == null || defaultModel.isBlank() ? "k2-fsa/OmniVoice" : defaultModel.trim(),
                Duration.ofSeconds(Math.max(5L, timeoutSeconds)),
                providerDownloadsEnabled,
                modelPullEnabled
        );
        return new OmniVoiceTool(objectMapper, runner, config);
    }

    private List<String> splitCommand(String raw, MeshingressProperties properties) {
        if (raw == null || raw.isBlank() || raw.trim().equals("omnivoice-wrapper --stdin")) {
            return defaultCommand(properties);
        }
        String value = raw.trim();
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        char quote = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if ((ch == '"' || ch == '\'') && (!quoted || quote == ch)) {
                quoted = !quoted;
                quote = quoted ? ch : 0;
                continue;
            }
            if (Character.isWhitespace(ch) && !quoted) {
                if (!current.isEmpty()) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(ch);
        }
        if (!current.isEmpty()) {
            parts.add(current.toString());
        }
        return parts.isEmpty() ? defaultCommand(properties) : List.copyOf(parts);
    }

    private List<String> defaultCommand(MeshingressProperties properties) {
        Path venvPython = Path.of(properties.cache().location(), "omnivoice-venv", "Scripts", "python.exe")
                .toAbsolutePath()
                .normalize();
        String python = java.nio.file.Files.isRegularFile(venvPython) ? venvPython.toString() : "python";
        Path wrapper = Path.of("toolspace", "omnivoice-tts", "runtime", "omnivoice_wrapper.py")
                .toAbsolutePath()
                .normalize();
        return List.of(python, wrapper.toString(), "--stdin");
    }
}

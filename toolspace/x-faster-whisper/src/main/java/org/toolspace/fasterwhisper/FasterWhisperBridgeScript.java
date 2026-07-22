package org.toolspace.fasterwhisper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Materializes the packaged Python bridge without coupling tool invocation to the vendor source path. */
final class FasterWhisperBridgeScript {
    Path resolve(FasterWhisperManifestProperties properties) throws IOException {
        if (properties.bridgeScript() != null && !properties.bridgeScript().isBlank()) {
            Path configured = Path.of(properties.bridgeScript()).toAbsolutePath().normalize();
            if (!Files.isRegularFile(configured)) {
                throw new IllegalStateException("Configured faster-whisper bridge script is unavailable: " + configured);
            }
            return configured;
        }
        Path target = Path.of(properties.bridgeRuntimeRoot()).toAbsolutePath().normalize().resolve("faster_whisper_bridge.py");
        if (Files.isRegularFile(target)) {
            return target;
        }
        Files.createDirectories(target.getParent());
        try (InputStream source = FasterWhisperBridgeScript.class.getResourceAsStream("/python/faster_whisper_bridge.py")) {
            if (source == null) {
                throw new IllegalStateException("Packaged faster-whisper bridge resource is unavailable");
            }
            Path temporary = Files.createTempFile(target.getParent(), "faster-whisper-bridge-", ".py");
            Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        return target;
    }
}

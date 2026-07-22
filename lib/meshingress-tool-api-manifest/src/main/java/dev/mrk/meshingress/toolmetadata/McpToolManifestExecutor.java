package dev.mrk.meshingress.toolmetadata;

import java.lang.reflect.Constructor;
import java.nio.file.Path;

/**
 * Build-time entry point that serializes a no-argument {@link McpToolManifestDefinition}
 * implementation into the static manifest consumed by repository assessment.
 */
public final class McpToolManifestExecutor {

    private McpToolManifestExecutor() {
    }

    public static void main(String[] arguments) {
        if (arguments == null || arguments.length != 2) {
            throw new IllegalArgumentException(
                    "Usage: McpToolManifestExecutor <manifest-class> <output-tool-manifest.json>"
            );
        }
        export(arguments[0], Path.of(arguments[1]));
    }

    public static void export(String manifestClassName, Path outputPath) {
        if (manifestClassName == null || manifestClassName.isBlank()) {
            throw new IllegalArgumentException("manifestClassName must not be blank");
        }
        if (outputPath == null) {
            throw new IllegalArgumentException("outputPath must not be null");
        }
        McpToolManifestDefinition manifest = instantiate(manifestClassName.strip());
        McpToolManifestJson.write(McpToolNativeMetadata.fromManifest(manifest), outputPath);
    }

    private static McpToolManifestDefinition instantiate(String manifestClassName) {
        try {
            Class<?> candidate = Class.forName(manifestClassName);
            if (!McpToolManifestDefinition.class.isAssignableFrom(candidate)) {
                throw new IllegalArgumentException(
                        "Manifest class does not implement McpToolManifestDefinition: " + manifestClassName
                );
            }
            @SuppressWarnings("unchecked")
            Class<? extends McpToolManifestDefinition> manifestType =
                    (Class<? extends McpToolManifestDefinition>) candidate;
            Constructor<? extends McpToolManifestDefinition> constructor = manifestType.getDeclaredConstructor();
            return constructor.newInstance();
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException(
                    "Unable to instantiate manifest class " + manifestClassName
                            + "; it must have an accessible no-argument constructor",
                    exception
            );
        }
    }
}

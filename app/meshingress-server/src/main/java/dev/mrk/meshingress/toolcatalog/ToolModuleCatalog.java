package dev.mrk.meshingress.toolcatalog;

import dev.mrk.meshingress.runtime.lifecycle.ToolModuleId;
import dev.mrk.meshingress.toolmetadata.McpToolManifestDefinition;
import dev.mrk.meshingress.toolmetadata.McpToolNativeMetadata;
import dev.mrk.meshingress.toolmetadata.ToolIcon;
import dev.mrk.meshingress.toolmetadata.ToolReadmeResolver;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Server-owned catalog of active module manifests.
 *
 * <p>Entries are registered from manifest beans, so classpath/open-source modules and
 * dynamically loaded modules share the same public resource model. The catalog keeps
 * only manifest-declared resources and never resolves a caller-provided path.</p>
 */
@Service
public final class ToolModuleCatalog implements SmartInitializingSingleton {

    private static final Logger logger = LoggerFactory.getLogger(ToolModuleCatalog.class);

    private final List<McpToolManifestDefinition> classpathManifests;
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private final ToolReadmeResolver readmeResolver = new ToolReadmeResolver();

    public ToolModuleCatalog(List<McpToolManifestDefinition> classpathManifests) {
        this.classpathManifests = classpathManifests == null ? List.of() : List.copyOf(classpathManifests);
    }

    @Override
    public synchronized void afterSingletonsInstantiated() {
        for (McpToolManifestDefinition manifest : classpathManifests) {
            register(classpathId(manifest), "classpath", manifest);
        }
    }

    public synchronized void registerRuntimeModule(ToolModuleId moduleId, Collection<McpToolManifestDefinition> manifests) {
        if (moduleId == null) return;
        unregister(moduleId.value());
        if (manifests == null) return;
        List<McpToolManifestDefinition> declared = manifests.stream()
                .filter(manifest -> manifest != null && manifest.metadata() != null && !manifest.metadata().namespace().isBlank())
                .toList();
        if (declared.size() > 1) {
            throw new IllegalArgumentException("A runtime tool module may declare only one module manifest: " + moduleId.value());
        }
        declared.forEach(manifest -> register(moduleId.value(), "runtime", manifest));
    }

    public synchronized void unregister(String moduleId) {
        if (moduleId != null) entries.remove(moduleId);
    }

    public synchronized List<Entry> list() {
        return entries.values().stream()
                .sorted(Comparator.comparing(Entry::namespace).thenComparing(Entry::id))
                .toList();
    }

    public synchronized Optional<Entry> find(String moduleId) {
        return Optional.ofNullable(entries.get(moduleId));
    }

    /** Finds an active module using its public, opaque catalog identifier. */
    public synchronized Optional<Entry> findByToolId(String toolId) {
        if (toolId == null || toolId.isBlank()) return Optional.empty();
        return entries.values().stream().filter(entry -> entry.toolId().equals(toolId)).findFirst();
    }

    /** Returns the stable public identity for a registered classpath manifest. */
    public String classpathToolId(McpToolManifestDefinition manifest) {
        return publicToolId("classpath", classpathId(manifest));
    }

    /** Returns the stable public identity for a runtime module owner. */
    public String runtimeToolId(String moduleId) {
        return publicToolId("runtime", moduleId);
    }

    private void register(String moduleId, String source, McpToolManifestDefinition manifest) {
        if (manifest == null || manifest.metadata() == null || manifest.metadata().namespace().isBlank()) return;
        McpToolNativeMetadata metadata = McpToolNativeMetadata.fromManifest(manifest);
        ToolIcon icon = metadata.metadata().icon();
        if (icon != null && !icon.hasNamespaceFileName(metadata.metadata().namespace())) {
            logger.warn("Tool module '{}' declares icon resource '{}'; its filename should be '{}' to match namespace '{}'. The icon remains available.",
                    moduleId, icon.resourcePath(), metadata.metadata().namespace() + icon.mimeType().fileExtension(), metadata.metadata().namespace());
        }
        Resource iconResource = icon == null ? null : new ClassPathResource(icon.resourcePath(), manifest.getClass().getClassLoader());
        if (iconResource != null && !iconResource.exists()) iconResource = null;
        entries.put(moduleId, new Entry(
                moduleId,
                publicToolId(source, moduleId),
                source,
                metadata,
                readmeResolver.resolve(metadata.readme()),
                iconResource
        ));
    }

    private static String classpathId(McpToolManifestDefinition manifest) {
        return "classpath:" + manifest.getClass().getName();
    }

    private static String publicToolId(String source, String moduleId) {
        String prefix = "classpath".equals(source) ? "cp-" : "rt-";
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(moduleId.getBytes(StandardCharsets.UTF_8));
            return prefix + Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }

    public record Entry(
            String id,
            String toolId,
            String source,
            McpToolNativeMetadata manifest,
            String readme,
            Resource iconResource
    ) {
        public String namespace() {
            return manifest.metadata().namespace();
        }

        public Optional<ToolIcon> icon() {
            return iconResource == null ? Optional.empty() : Optional.ofNullable(manifest.metadata().icon());
        }
    }
}

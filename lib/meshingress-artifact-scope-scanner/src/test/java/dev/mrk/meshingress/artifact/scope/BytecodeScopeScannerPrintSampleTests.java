package dev.mrk.meshingress.artifact.scope;

import dev.mrk.meshingress.api.tools.annotation.McpCacheResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class BytecodeScopeScannerPrintSampleTests {
    @Test
    void printJsonCatalogBackedJarScopeInference(@TempDir Path tempDir) throws Exception {
        Path sampleJar = tempDir.resolve("sample-module.jar");
        writeJar(sampleJar, CacheWritingTool.class);
        ScopeInferenceCatalog catalogBean = loadCatalogBean();
        JarScopeScanResult result = new BytecodeScopeScanner().scan(sampleJar, catalogBean);

        System.out.println();
        System.out.println("Catalog bean version: " + catalogBean.version());
        System.out.println("Scanned JAR: " + result.artifact().toAbsolutePath().normalize());
        System.out.println("Inferred scopes: " + result.inferredScopes());

        Map<String, List<ScopeFinding>> findingsByScope = result.findings().stream()
                .sorted(Comparator.comparing(ScopeFinding::scope).thenComparing(ScopeFinding::location))
                .collect(Collectors.groupingBy(ScopeFinding::scope));

        findingsByScope.forEach((scope, findings) -> {
            System.out.println();
            System.out.println(scope + " (" + findings.size() + ")");
            for (ScopeFinding finding : findings) {
                System.out.println("  - rule=" + finding.ruleId()
                        + ", matcher=" + finding.matcherType().wireName()
                        + ", confidence=" + finding.confidence());
                System.out.println("    location=" + finding.location());
                System.out.println("    evidence=" + finding.evidence());
            }
        });
    }

    private static ScopeInferenceCatalog loadCatalogBean() throws Exception {
        ScopeInferenceCatalogLoader loader = new ScopeInferenceCatalogLoader(new ObjectMapper());
        try (var stream = BytecodeScopeScannerPrintSampleTests.class.getResourceAsStream("/scope-rules/sample-bytecode-scope-catalog.json")) {
            return loader.load(stream);
        }
    }

    private static void writeJar(Path jar, Class<?>... classes) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(java.nio.file.Files.newOutputStream(jar))) {
            for (Class<?> type : classes) {
                String entryName = type.getName().replace('.', '/') + ".class";
                zip.putNextEntry(new ZipEntry(entryName));
                zip.write(classBytes(type));
                zip.closeEntry();
            }
        }
    }

    private static byte[] classBytes(Class<?> type) throws IOException {
        String resourceName = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream inputStream = type.getResourceAsStream(resourceName)) {
            assertThat(inputStream).isNotNull();
            return inputStream.readAllBytes();
        }
    }

    private static final class CacheWritingTool {
        @McpCacheResult
        String cached() {
            return "cached";
        }
    }
}

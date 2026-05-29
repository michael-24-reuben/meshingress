package dev.mrk.meshingress.artifact.scope;

import dev.mrk.meshingress.api.tools.annotation.McpFunction;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class BytecodeScopeScannerTests {
    private static final Path SAMPLE_JAR = Path.of("..", "..", "temp", "sample-module-0.0.1-SNAPSHOT-all.jar");

    @Test
    void scansJarWithJsonCatalogLoadedOnce() throws Exception {
        ScopeInferenceCatalog catalogBean = loadCatalogBean();
        BytecodeScopeScanner scanner = new BytecodeScopeScanner();

        JarScopeScanResult firstScan = scanner.scan(SAMPLE_JAR, catalogBean);
        JarScopeScanResult secondScan = scanner.scan(SAMPLE_JAR, catalogBean);

        assertThat(firstScan.catalogVersion()).isEqualTo("sample-bytecode-catalog-v1");
        assertThat(firstScan.inferredScopes()).contains("CACHE_WRITE");
        assertThat(secondScan.findings()).hasSize(firstScan.findings().size());
    }

    @Test
    void reachableScanIgnoresRiskyMethodsThatAreNotReachableFromToolEntrypoints(@TempDir Path tempDir) throws Exception {
        Path jar = tempDir.resolve("reachable-tool.jar");
        writeJar(jar, ReachableTool.class, ReachableHelper.class, UnreachableDependency.class);

        JarScopeScanResult result = new BytecodeScopeScanner()
                .scanReachableFromToolEntrypoints(jar, loadCatalogBean());

        assertThat(result.analysisMode()).isEqualTo("sootup-cha-reachable-tool-entrypoints");
        assertThat(result.entrypoints()).isNotEmpty();
        assertThat(result.inferredScopes()).contains("FILES_READ");
        assertThat(result.inferredScopes()).doesNotContain("SHELL_EXECUTE");
        assertThat(result.findings())
                .allSatisfy(finding -> assertThat(finding.reachability()).isEqualTo("reachable-from-tool-entrypoint"));
    }

    private static ScopeInferenceCatalog loadCatalogBean() throws Exception {
        ScopeInferenceCatalogLoader loader = new ScopeInferenceCatalogLoader(new ObjectMapper());
        try (var stream = BytecodeScopeScannerTests.class.getResourceAsStream("/scope-rules/sample-bytecode-scope-catalog.json")) {
            assertThat(stream).isNotNull();
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

    @McpTool("reachable")
    private static final class ReachableTool {
        @McpFunction("read")
        String call() throws IOException {
            return new ReachableHelper().read(Path.of("sample.txt"));
        }
    }

    private static final class ReachableHelper {
        String read(Path path) throws IOException {
            return java.nio.file.Files.readString(path);
        }
    }

    private static final class UnreachableDependency {
        void execute() throws IOException {
            new ProcessBuilder("cmd", "/c", "echo unreachable").start();
        }
    }
}

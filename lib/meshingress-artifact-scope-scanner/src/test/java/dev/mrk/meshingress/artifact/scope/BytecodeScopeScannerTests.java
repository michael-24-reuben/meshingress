package dev.mrk.meshingress.artifact.scope;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;

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

    private static ScopeInferenceCatalog loadCatalogBean() throws Exception {
        ScopeInferenceCatalogLoader loader = new ScopeInferenceCatalogLoader(new ObjectMapper());
        try (var stream = BytecodeScopeScannerTests.class.getResourceAsStream("/scope-rules/sample-bytecode-scope-catalog.json")) {
            assertThat(stream).isNotNull();
            return loader.load(stream);
        }
    }
}

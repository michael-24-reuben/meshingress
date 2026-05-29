package dev.mrk.meshingress.artifact.scope;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class BytecodeScopeScannerPrintSampleTests {
    private static final Path SAMPLE_JAR = Path.of("..", "..", "temp", "sample-module-0.0.1-SNAPSHOT-all.jar");

    @Test
    void printJsonCatalogBackedJarScopeInference() throws Exception {
        ScopeInferenceCatalog catalogBean = loadCatalogBean();
        JarScopeScanResult result = new BytecodeScopeScanner().scan(SAMPLE_JAR, catalogBean);

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
}

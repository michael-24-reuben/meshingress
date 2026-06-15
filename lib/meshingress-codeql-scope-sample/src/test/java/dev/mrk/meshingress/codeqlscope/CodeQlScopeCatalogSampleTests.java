package dev.mrk.meshingress.codeqlscope;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CodeQlScopeCatalogSampleTests {
    private static final Path SAMPLE_JAR = Path.of("..", "..", "temp", "sample-module-0.0.1-SNAPSHOT-all.jar");

    @Test
    void scansSampleJarAgainstPredefinedScopeCatalog() throws Exception {
        assertThat(Files.exists(SAMPLE_JAR))
                .as("sample jar must exist at %s", SAMPLE_JAR.toAbsolutePath())
                .isTrue();

        List<ScopeFinding> findings = new JarScopeCategorizer()
                .scan(SAMPLE_JAR, PredefinedScopeCatalog.sampleRules());

        assertThat(findings)
                .extracting(ScopeFinding::scope)
                .contains("CACHE_WRITE");

        System.out.println("Scope findings for " + SAMPLE_JAR.toAbsolutePath() + ":");
        System.out.println(findings.stream()
                .map(finding -> "- " + finding.scope()
                        + " rule=" + finding.ruleId()
                        + " matcher=" + finding.matcherType().wireName()
                        + " confidence=" + finding.confidence()
                        + " location=" + finding.location())
                .collect(Collectors.joining(System.lineSeparator())));
    }

    @Test
    void generatesCodeQlQueryFromSamePredefinedScopeCatalog() {
        String query = new CodeQlScopeQueryGenerator()
                .generateQuery(PredefinedScopeCatalog.sampleRules());

        assertThat(query)
                .contains("Generated Meshingress scope inference query")
                .contains("FILES_READ")
                .contains("java.nio.file")
                .contains("Files")
                .contains("NETWORK_ACCESS");

        System.out.println("Generated CodeQL query sample:");
        System.out.println(query);
    }
}

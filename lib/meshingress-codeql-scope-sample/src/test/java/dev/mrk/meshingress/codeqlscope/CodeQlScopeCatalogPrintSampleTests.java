package dev.mrk.meshingress.codeqlscope;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class CodeQlScopeCatalogPrintSampleTests {
    private static final Path SAMPLE_JAR = Path.of("..", "..", "temp", "sample-module-0.0.1-SNAPSHOT-all.jar");

    @Test
    void printJarScopeCategorizationSample() throws Exception {
        List<ScopeRule> rules = PredefinedScopeCatalog.sampleRules();
        List<ScopeFinding> findings = new JarScopeCategorizer().scan(SAMPLE_JAR, rules);

        Map<String, List<ScopeFinding>> findingsByScope = findings.stream()
                .collect(Collectors.groupingBy(ScopeFinding::scope));

        System.out.println();
        System.out.println("Sample JAR:");
        System.out.println(SAMPLE_JAR.toAbsolutePath().normalize());
        System.out.println();
        System.out.println("Scope findings:");

        findingsByScope.forEach((scope, scopeFindings) -> {
            System.out.println();
            System.out.println(scope + " (" + scopeFindings.size() + ")");
            for (ScopeFinding finding : scopeFindings) {
                System.out.println("  - rule=" + finding.ruleId()
                        + ", matcher=" + finding.matcherType().wireName()
                        + ", confidence=" + finding.confidence()
                        + ", reviewOnly=" + finding.reviewOnly());
                System.out.println("    location=" + finding.location());
                System.out.println("    evidence=" + finding.evidence());
            }
        });
    }

    @Test
    void printGeneratedCodeQlQuerySample() {
        String query = new CodeQlScopeQueryGenerator()
                .generateQuery(PredefinedScopeCatalog.sampleRules());

        System.out.println();
        System.out.println("Generated CodeQL query:");
        System.out.println(query);
    }
}

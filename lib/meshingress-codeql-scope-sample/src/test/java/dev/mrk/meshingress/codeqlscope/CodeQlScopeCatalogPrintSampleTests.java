package dev.mrk.meshingress.codeqlscope;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class CodeQlScopeCatalogPrintSampleTests {
    @Test
    void printJarScopeCategorizationSample(@TempDir Path tempDir) throws Exception {
        Path sampleJar = tempDir.resolve("sample-module.jar");
        writeJar(sampleJar, FileWritingTool.class);
        List<ScopeRule> rules = PredefinedScopeCatalog.sampleRules();
        List<ScopeFinding> findings = new JarScopeCategorizer().scan(sampleJar, rules);

        Map<String, List<ScopeFinding>> findingsByScope = findings.stream()
                .collect(Collectors.groupingBy(ScopeFinding::scope));

        System.out.println();
        System.out.println("Sample JAR:");
        System.out.println(sampleJar.toAbsolutePath().normalize());
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

    private static void writeJar(Path jar, Class<?>... classes) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(jar))) {
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

    private static final class FileWritingTool {
        void write(Path path) throws IOException {
            Files.writeString(path, "sample");
        }
    }
}

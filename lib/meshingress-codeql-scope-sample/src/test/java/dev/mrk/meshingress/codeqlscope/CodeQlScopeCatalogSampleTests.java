package dev.mrk.meshingress.codeqlscope;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class CodeQlScopeCatalogSampleTests {
    @Test
    void scansSampleJarAgainstPredefinedScopeCatalog(@TempDir Path tempDir) throws Exception {
        Path sampleJar = tempDir.resolve("sample-module.jar");
        writeJar(sampleJar, FileWritingTool.class);

        List<ScopeFinding> findings = new JarScopeCategorizer()
                .scan(sampleJar, PredefinedScopeCatalog.sampleRules());

        assertThat(findings)
                .extracting(ScopeFinding::scope)
                .contains("FILES_WRITE");

        System.out.println("Scope findings for " + sampleJar.toAbsolutePath() + ":");
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

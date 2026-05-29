package dev.mrk.meshingress.artifact.scope;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record JarScopeScanResult(
        Path artifact,
        String catalogVersion,
        List<ScopeFinding> findings,
        String analysisMode,
        List<String> entrypoints,
        List<String> diagnostics
) {
    public JarScopeScanResult(Path artifact, String catalogVersion, List<ScopeFinding> findings) {
        this(artifact, catalogVersion, findings, "bytecode-full", List.of(), List.of());
    }

    public JarScopeScanResult {
        findings = findings == null ? List.of() : List.copyOf(findings);
        analysisMode = analysisMode == null || analysisMode.isBlank() ? "bytecode-full" : analysisMode;
        entrypoints = entrypoints == null ? List.of() : List.copyOf(entrypoints);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public Set<String> inferredScopes() {
        return findings.stream()
                .map(ScopeFinding::scope)
                .collect(Collectors.toUnmodifiableSet());
    }
}

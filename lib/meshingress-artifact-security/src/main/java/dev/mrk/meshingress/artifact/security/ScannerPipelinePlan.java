package dev.mrk.meshingress.artifact.security;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public record ScannerPipelinePlan(List<ScannerPipelineStage> stages) {
    public ScannerPipelinePlan {
        stages = stages == null ? List.of() : List.copyOf(stages);
    }

    public static ScannerPipelinePlan empty() {
        return new ScannerPipelinePlan(List.of());
    }

    public Optional<ScannerPipelineStage> stageFor(String scanner) {
        String normalized = normalize(scanner);
        return stages.stream()
                .filter(stage -> normalize(stage.scanner()).equals(normalized))
                .findFirst();
    }

    public ScannerPipelineStage stageOrDefault(String scanner) {
        return stageFor(scanner)
                .orElseGet(() -> new ScannerPipelineStage(
                        scanner,
                        "",
                        true,
                        ScannerPipelineStage.DEFAULT_TIMEOUT,
                        ScannerFailurePolicy.BLOCK
                ));
    }

    public List<String> requiredScanners() {
        return stages.stream()
                .filter(ScannerPipelineStage::required)
                .map(ScannerPipelineStage::scanner)
                .toList();
    }

    public List<String> optionalScanners() {
        return stages.stream()
                .filter(stage -> !stage.required())
                .map(ScannerPipelineStage::scanner)
                .toList();
    }

    public Map<String, Object> toSummary() {
        return Map.of(
                "requiredScanners", requiredScanners(),
                "optionalScanners", optionalScanners(),
                "stages", stages.stream().map(ScannerPipelineStage::toSummary).toList()
        );
    }

    private static String normalize(String scanner) {
        return scanner == null ? "" : scanner.trim().toLowerCase(Locale.ROOT);
    }
}

package dev.mrk.meshingress.artifact.security;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class GrypeScannerAdapter implements ScannerAdapter {
    public static final String SCANNER_NAME = "grype-vulnerability-scanner";
    private static final String RAW_REPORT_FILE = SCANNER_NAME + ".json";

    private final ScannerProcessRunner runner;
    private final ObjectMapper objectMapper;
    private final GrypeScannerOptions options;

    public GrypeScannerAdapter(ScannerProcessRunner runner, ObjectMapper objectMapper, GrypeScannerOptions options) {
        this.runner = Objects.requireNonNull(runner, "runner");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.options = options == null ? GrypeScannerOptions.defaults() : options;
    }

    @Override
    public String name() {
        return SCANNER_NAME;
    }

    @Override
    public ScannerResult scan(ScannerRequest request) {
        Path rawReportPath = rawReportPath(request);
        ScannerProcessResult process = runner.run(new ScannerProcessRequest(
                name(),
                List.of(options.executable(), request.artifactPath().toString(), "-o", "json", "--quiet"),
                request.quarantinePath(),
                options.timeout(),
                Map.of(),
                options.maxOutputChars()
        ));
        if (!process.successfulExit()) {
            writeFailureReport(rawReportPath, process);
            return process.toFailureScannerResult(options.scannerVersion(), options.failurePolicy(), rawReportPath);
        }
        writeRawReport(rawReportPath, process.stdout());
        return parse(process.stdout(), rawReportPath);
    }

    ScannerResult parse(String json, Path rawReportPath) {
        try {
            JsonNode root = objectMapper.readTree(json == null ? "{}" : json);
            List<Finding> findings = parseFindings(root.path("matches"));
            Map<String, Long> severityCounts = severityCounts(findings);
            Map<String, Object> rawSummary = new LinkedHashMap<>();
            rawSummary.put("matchCount", findings.size());
            rawSummary.put("severityCounts", severityCounts);
            rawSummary.put("blockSeverityThreshold", options.blockSeverityThreshold().name());
            rawSummary.put("reviewSeverityThreshold", options.reviewSeverityThreshold().name());
            rawSummary.put("sourceType", root.path("source").path("type").asString(""));
            rawSummary.put("target", root.path("descriptor").path("name").asString(""));
            if (rawReportPath != null) {
                rawSummary.put("rawReport", RAW_REPORT_FILE);
            }
            return new ScannerResult(
                    name(),
                    options.scannerVersion(),
                    statusFrom(findings),
                    findings,
                    rawSummary,
                    rawReportPath
            );
        } catch (Exception exception) {
            return parseFailure(exception, rawReportPath);
        }
    }

    private List<Finding> parseFindings(JsonNode matchesNode) {
        List<Finding> findings = new ArrayList<>();
        for (JsonNode matchNode : matchesNode) {
            JsonNode vulnerability = matchNode.path("vulnerability");
            JsonNode artifact = matchNode.path("artifact");
            String vulnerabilityId = vulnerability.path("id").asString("");
            String severity = GrypeSeverity.fromWire(vulnerability.path("severity").asString(""), GrypeSeverity.INFO).name();
            String packageName = artifact.path("name").asString("");
            String packageVersion = artifact.path("version").asString("");
            String packageRef = packageName + (packageVersion.isBlank() ? "" : ":" + packageVersion);
            String message = vulnerabilityId + (packageRef.isBlank() ? "" : " in " + packageRef);
            String description = vulnerability.path("description").asString("");
            if (!description.isBlank()) {
                message = message + " - " + description;
            }
            findings.add(new Finding(
                    severity,
                    vulnerabilityId,
                    message,
                    pathFrom(artifact)
            ));
        }
        return findings.stream()
                .sorted(Comparator.comparing((Finding finding) -> GrypeSeverity.fromWire(finding.severity(), GrypeSeverity.INFO).rank())
                        .reversed()
                        .thenComparing(Finding::code)
                        .thenComparing(Finding::path))
                .toList();
    }

    private String pathFrom(JsonNode artifact) {
        for (JsonNode location : artifact.path("locations")) {
            String path = location.path("path").asString("");
            if (!path.isBlank()) {
                return path;
            }
        }
        String purl = artifact.path("purl").asString("");
        if (!purl.isBlank()) {
            return purl;
        }
        String name = artifact.path("name").asString("");
        String version = artifact.path("version").asString("");
        return name + (version.isBlank() ? "" : ":" + version);
    }

    private ScannerStatus statusFrom(List<Finding> findings) {
        if (findings.stream().anyMatch(finding -> GrypeSeverity.fromWire(finding.severity(), GrypeSeverity.INFO)
                .atLeast(options.blockSeverityThreshold()))) {
            return ScannerStatus.BLOCKED;
        }
        if (findings.stream().anyMatch(finding -> GrypeSeverity.fromWire(finding.severity(), GrypeSeverity.INFO)
                .atLeast(options.reviewSeverityThreshold()))) {
            return ScannerStatus.REVIEW;
        }
        return ScannerStatus.PASSED;
    }

    private Map<String, Long> severityCounts(List<Finding> findings) {
        Map<String, Long> counts = new TreeMap<>();
        for (Finding finding : findings) {
            counts.merge(finding.severity(), 1L, Long::sum);
        }
        return counts;
    }

    private ScannerResult parseFailure(Exception exception, Path rawReportPath) {
        Map<String, Object> rawSummary = Map.of(
                "failurePolicy", options.failurePolicy().name(),
                "failureReason", exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage()
        );
        return new ScannerResult(
                name(),
                options.scannerVersion(),
                options.failurePolicy().scannerStatusForFailure(),
                List.of(new Finding(
                        "HIGH",
                        "GRYPE_JSON_PARSE_ERROR",
                        "Grype JSON output could not be parsed: " + rawSummary.get("failureReason"),
                        RAW_REPORT_FILE
                )),
                rawSummary,
                rawReportPath
        );
    }

    private Path rawReportPath(ScannerRequest request) {
        if (request.rawReportDirectory() == null) {
            return null;
        }
        return request.rawReportDirectory().resolve(RAW_REPORT_FILE);
    }

    private void writeRawReport(Path rawReportPath, String report) {
        if (rawReportPath == null) {
            return;
        }
        try {
            Files.createDirectories(rawReportPath.getParent());
            Files.writeString(rawReportPath, report == null ? "" : report);
        } catch (Exception exception) {
            throw new IllegalStateException("unable to write Grype raw report: " + exception.getMessage(), exception);
        }
    }

    private void writeFailureReport(Path rawReportPath, ScannerProcessResult process) {
        if (rawReportPath == null) {
            return;
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("scanner", name());
        report.put("processStatus", process.status().name());
        report.put("failureReason", process.failureReason());
        report.put("stdout", process.stdout());
        report.put("stderr", process.stderr());
        try {
            writeRawReport(rawReportPath, objectMapper.writeValueAsString(report));
        } catch (Exception exception) {
            throw new IllegalStateException("unable to write Grype failure report: " + exception.getMessage(), exception);
        }
    }
}

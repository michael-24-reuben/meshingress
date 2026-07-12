package dev.mrk.meshingress.repository.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import dev.mrk.meshingress.artifact.security.ScannerProcessRequest;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "meshingress.repository")
public record MeshingressRepositoryProperties(
        @NotNull Path root,
        @NotBlank String signingSecret,
        @NotBlank String signingKeyId,
        String provenanceSourceRepo,
        String provenanceSourceCommit,
        String provenanceGeneratedBy,
        String provenanceGeneratorCommit,
        boolean scopeScannerEnabled,
        String scopeCatalogLocation,
        String metadataStore,
        ScannerPipeline scannerPipeline,
        RawReportRetention rawReportRetention,
        Sandbox sandbox,
        Grype grype,
        PublicationEligibility publicationEligibility,
        Sql sql
) {
    public MeshingressRepositoryProperties {
        root = root == null ? Path.of("repository") : root;
        signingSecret = signingSecret == null || signingSecret.isBlank() ? "dev-repository-signing-key" : signingSecret;
        signingKeyId = signingKeyId == null || signingKeyId.isBlank() ? "local-dev-hmac" : signingKeyId.trim();
        provenanceSourceRepo = provenanceSourceRepo == null ? "" : provenanceSourceRepo.trim();
        provenanceSourceCommit = provenanceSourceCommit == null ? "" : provenanceSourceCommit.trim();
        provenanceGeneratedBy = provenanceGeneratedBy == null || provenanceGeneratedBy.isBlank()
                ? "meshingress-repository"
                : provenanceGeneratedBy.trim();
        provenanceGeneratorCommit = provenanceGeneratorCommit == null ? "" : provenanceGeneratorCommit.trim();
        scopeCatalogLocation = scopeCatalogLocation == null || scopeCatalogLocation.isBlank()
                ? "classpath:/scope-rules/default-bytecode-scope-catalog.json"
                : scopeCatalogLocation;
        metadataStore = metadataStore == null || metadataStore.isBlank() ? "sql" : metadataStore.trim();
        scannerPipeline = scannerPipeline == null ? ScannerPipeline.defaults() : scannerPipeline.withDefaults();
        rawReportRetention = rawReportRetention == null ? RawReportRetention.defaults() : rawReportRetention.withDefaults();
        sandbox = sandbox == null ? Sandbox.defaults() : sandbox.withDefaults();
        grype = grype == null ? Grype.defaults() : grype.withDefaults();
        publicationEligibility = publicationEligibility == null
                ? PublicationEligibility.defaults()
                : publicationEligibility.withDefaults();
        sql = sql == null ? Sql.defaults() : sql;
    }

    public record ScannerPipeline(
            List<String> requiredScanners,
            List<String> optionalScanners,
            Duration defaultTimeout,
            String failurePolicy,
            Map<String, String> scannerVersions,
            Map<String, Duration> scannerTimeouts,
            Map<String, String> scannerFailurePolicies
    ) {
        public ScannerPipeline {
            requiredScanners = copyOrDefault(requiredScanners, List.of("cyclonedx-sbom", "embedded-jar-sandbox", "bytecode-scope-scanner"));
            optionalScanners = copyOrDefault(optionalScanners, List.of());
            defaultTimeout = defaultTimeout == null || defaultTimeout.isNegative() || defaultTimeout.isZero()
                    ? Duration.ofSeconds(30)
                    : defaultTimeout;
            failurePolicy = failurePolicy == null || failurePolicy.isBlank() ? "block" : failurePolicy.trim();
            scannerVersions = scannerVersions == null ? Map.of() : Map.copyOf(scannerVersions);
            scannerTimeouts = scannerTimeouts == null ? Map.of() : Map.copyOf(scannerTimeouts);
            scannerFailurePolicies = scannerFailurePolicies == null ? Map.of() : Map.copyOf(scannerFailurePolicies);
        }

        static ScannerPipeline defaults() {
            return new ScannerPipeline(
                    List.of("cyclonedx-sbom", "embedded-jar-sandbox", "bytecode-scope-scanner"),
                    List.of(),
                    Duration.ofSeconds(30),
                    "block",
                    Map.of(),
                    Map.of(),
                    Map.of()
            );
        }

        ScannerPipeline withDefaults() {
            return new ScannerPipeline(
                    requiredScanners,
                    optionalScanners,
                    defaultTimeout,
                    failurePolicy,
                    scannerVersions,
                    scannerTimeouts,
                    scannerFailurePolicies
            );
        }

        private static List<String> copyOrDefault(List<String> values, List<String> fallback) {
            if (values == null) {
                return fallback;
            }
            return values.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .toList();
        }
    }

    public record RawReportRetention(
            String policy,
            String location,
            Boolean scannerReports,
            Boolean sandboxReports
    ) {
        public RawReportRetention {
            policy = policy == null || policy.isBlank() ? "retain-with-artifact" : policy.trim();
            location = location == null || location.isBlank() ? "artifact-assessment-directory" : location.trim();
            scannerReports = scannerReports == null || scannerReports;
            sandboxReports = sandboxReports == null || sandboxReports;
        }

        static RawReportRetention defaults() {
            return new RawReportRetention(
                    "retain-with-artifact",
                    "artifact-assessment-directory",
                    true,
                    true
            );
        }

        RawReportRetention withDefaults() {
            return new RawReportRetention(policy, location, scannerReports, sandboxReports);
        }

        public Map<String, Object> toSummary() {
            return Map.of(
                    "policy", policy,
                    "location", location,
                    "scannerReports", scannerReports,
                    "sandboxReports", sandboxReports
            );
        }
    }

    public record Sandbox(
            Boolean enabled,
            String scannerName,
            String scannerVersion,
            String strategy
    ) {
        public Sandbox {
            enabled = enabled == null || enabled;
            scannerName = scannerName == null || scannerName.isBlank() ? "embedded-jar-sandbox" : scannerName.trim();
            scannerVersion = scannerVersion == null || scannerVersion.isBlank() ? "static-1" : scannerVersion.trim();
            strategy = strategy == null || strategy.isBlank() ? "static-quarantine-inspection" : strategy.trim();
        }

        static Sandbox defaults() {
            return new Sandbox(
                    true,
                    "embedded-jar-sandbox",
                    "static-1",
                    "static-quarantine-inspection"
            );
        }

        Sandbox withDefaults() {
            return new Sandbox(enabled, scannerName, scannerVersion, strategy);
        }
    }

    public record Grype(
            String executable,
            String blockSeverityThreshold,
            String reviewSeverityThreshold,
            Integer maxOutputChars
    ) {
        public Grype {
            executable = executable == null || executable.isBlank() ? "grype" : executable.trim();
            blockSeverityThreshold = blockSeverityThreshold == null || blockSeverityThreshold.isBlank()
                    ? "critical"
                    : blockSeverityThreshold.trim();
            reviewSeverityThreshold = reviewSeverityThreshold == null || reviewSeverityThreshold.isBlank()
                    ? "low"
                    : reviewSeverityThreshold.trim();
            maxOutputChars = maxOutputChars == null || maxOutputChars <= 0
                    ? ScannerProcessRequest.DEFAULT_MAX_OUTPUT_CHARS
                    : maxOutputChars;
        }

        static Grype defaults() {
            return new Grype("grype", "critical", "low", ScannerProcessRequest.DEFAULT_MAX_OUTPUT_CHARS);
        }

        Grype withDefaults() {
            return new Grype(executable, blockSeverityThreshold, reviewSeverityThreshold, maxOutputChars);
        }
    }

    public record PublicationEligibility(
            Boolean enabled,
            Integer minimumReviewerApprovals,
            List<String> requiredScanners,
            List<String> requiredProvenanceFields,
            List<String> trustedBuilders,
            List<String> trustedSigningKeyIds,
            List<String> revokedSigningKeyIds
    ) {
        public PublicationEligibility {
            enabled = enabled == null || enabled;
            minimumReviewerApprovals = minimumReviewerApprovals == null || minimumReviewerApprovals < 1
                    ? 1
                    : minimumReviewerApprovals;
            requiredScanners = copyOrDefault(requiredScanners, List.of());
            requiredProvenanceFields = copyOrDefault(requiredProvenanceFields, List.of("generatedBy"));
            trustedBuilders = copyOrDefault(trustedBuilders, List.of());
            trustedSigningKeyIds = copyOrDefault(trustedSigningKeyIds, List.of());
            revokedSigningKeyIds = copyOrDefault(revokedSigningKeyIds, List.of());
        }

        static PublicationEligibility defaults() {
            return new PublicationEligibility(true, 1, List.of(), List.of("generatedBy"), List.of(), List.of(), List.of());
        }

        PublicationEligibility withDefaults() {
            return new PublicationEligibility(
                    enabled,
                    minimumReviewerApprovals,
                    requiredScanners,
                    requiredProvenanceFields,
                    trustedBuilders,
                    trustedSigningKeyIds,
                    revokedSigningKeyIds
            );
        }

        private static List<String> copyOrDefault(List<String> values, List<String> fallback) {
            if (values == null) {
                return fallback;
            }
            return values.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .toList();
        }
    }

    public record Sql(
            String schema,
            String tablePrefix,
            Tables table,
            boolean initializeSchema
    ) {
        public Sql {
            schema = schema == null || schema.isBlank() ? "meshingress" : schema.trim();
            tablePrefix = tablePrefix == null ? "repository_" : tablePrefix.trim();
            table = table == null ? Tables.defaults(tablePrefix) : table.withDefaults(tablePrefix);
        }

        static Sql defaults() {
            return new Sql("meshingress", "repository_", Tables.defaults("repository_"), true);
        }
    }

    public record Tables(
            String artifacts,
            String artifactFiles,
            String assessments,
            String publications,
            String lifecycleEvents
    ) {
        public Tables withDefaults(String prefix) {
            return new Tables(
                    valueOrDefault(artifacts, prefix + "artifacts"),
                    valueOrDefault(artifactFiles, prefix + "artifact_files"),
                    valueOrDefault(assessments, prefix + "assessments"),
                    valueOrDefault(publications, prefix + "publications"),
                    valueOrDefault(lifecycleEvents, prefix + "lifecycle_events")
            );
        }

        static Tables defaults(String prefix) {
            return new Tables(
                    prefix + "artifacts",
                    prefix + "artifact_files",
                    prefix + "assessments",
                    prefix + "publications",
                    prefix + "lifecycle_events"
            );
        }

        private static String valueOrDefault(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value.trim();
        }
    }
}

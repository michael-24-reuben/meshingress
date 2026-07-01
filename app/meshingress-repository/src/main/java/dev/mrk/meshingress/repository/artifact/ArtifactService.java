package dev.mrk.meshingress.repository.artifact;

import dev.mrk.meshingress.artifact.model.ArtifactAssessmentSummary;
import dev.mrk.meshingress.artifact.model.ArtifactFileEntry;
import dev.mrk.meshingress.artifact.model.ArtifactCoordinate;
import dev.mrk.meshingress.artifact.model.ArtifactPublicationRecord;
import dev.mrk.meshingress.artifact.model.ArtifactProvenance;
import dev.mrk.meshingress.artifact.model.ArtifactRecord;
import dev.mrk.meshingress.artifact.model.ArtifactScopeDeclaration;
import dev.mrk.meshingress.artifact.model.ArtifactTrustStatus;
import dev.mrk.meshingress.artifact.model.DeniedScope;
import dev.mrk.meshingress.artifact.model.MeshingressArtifactType;
import dev.mrk.meshingress.artifact.model.PublicationEligibilityDecision;
import dev.mrk.meshingress.artifact.publication.PublicationRecordSigner;
import dev.mrk.meshingress.artifact.publication.PublicationSignature;
import dev.mrk.meshingress.artifact.scope.BytecodeScopeScanner;
import dev.mrk.meshingress.artifact.scope.CycloneDxSbom;
import dev.mrk.meshingress.artifact.scope.CycloneDxSbomGenerator;
import dev.mrk.meshingress.artifact.scope.JarScopeScanResult;
import dev.mrk.meshingress.artifact.scope.ScopeFinding;
import dev.mrk.meshingress.artifact.scope.ScopeInferenceCatalog;
import dev.mrk.meshingress.artifact.security.ScannerAdapter;
import dev.mrk.meshingress.artifact.security.Finding;
import dev.mrk.meshingress.artifact.security.ScannerPipelinePlan;
import dev.mrk.meshingress.artifact.security.ScannerPipelineStage;
import dev.mrk.meshingress.artifact.security.ScannerRequest;
import dev.mrk.meshingress.artifact.security.ScannerResult;
import dev.mrk.meshingress.artifact.security.ScannerStatus;
import dev.mrk.meshingress.repository.config.MeshingressRepositoryProperties;
import dev.mrk.meshingress.artifact.storage.FileSystemArtifactStorage;
import dev.mrk.meshingress.artifact.storage.StoredArtifactBlob;
import dev.mrk.meshingress.repository.artifact.store.ArtifactMetadataEntry;
import dev.mrk.meshingress.repository.artifact.store.ArtifactLifecycleEvent;
import dev.mrk.meshingress.repository.artifact.store.ArtifactMetadataStore;
import dev.mrk.meshingress.toolmetadata.McpToolNativeMetadata;
import dev.mrk.meshingress.toolmetadata.McpToolNativeMetadataExporter;
import dev.mrk.meshingress.toolmetadata.McpToolNativeMetadataExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ArtifactService {

    private final FileSystemArtifactStorage storage;
    private final List<ScannerAdapter> scanners;
    private final BytecodeScopeScanner bytecodeScopeScanner;
    private final CycloneDxSbomGenerator sbomGenerator;
    private final ScannerPipelinePlan scannerPipelinePlan;
    private final MeshingressRepositoryProperties.RawReportRetention rawReportRetention;
    private final MeshingressRepositoryProperties.Sandbox sandbox;
    private final MeshingressRepositoryProperties.PublicationEligibility publicationEligibility;
    private final ScopeInferenceCatalog scopeInferenceCatalog;
    private final boolean scopeScannerEnabled;
    private final ArtifactProvenance publicationProvenance;
    private final PublicationRecordSigner signer;
    private final ObjectMapper objectMapper;
    private final ArtifactMetadataStore metadataStore;
    private final McpToolNativeMetadataExtractor nativeMetadataExtractor;
    private final McpToolNativeMetadataExporter nativeMetadataExporter;

    public ArtifactService(
            FileSystemArtifactStorage storage,
            List<ScannerAdapter> scanners,
            BytecodeScopeScanner bytecodeScopeScanner,
            CycloneDxSbomGenerator sbomGenerator,
            ScannerPipelinePlan scannerPipelinePlan,
            ScopeInferenceCatalog scopeInferenceCatalog,
            MeshingressRepositoryProperties properties,
            PublicationRecordSigner signer,
            ObjectMapper objectMapper,
            ArtifactMetadataStore metadataStore,
            McpToolNativeMetadataExtractor nativeMetadataExtractor,
            McpToolNativeMetadataExporter nativeMetadataExporter
    ) {
        this.storage = storage;
        this.bytecodeScopeScanner = bytecodeScopeScanner;
        this.sbomGenerator = sbomGenerator;
        this.scannerPipelinePlan = scannerPipelinePlan == null ? ScannerPipelinePlan.empty() : scannerPipelinePlan;
        this.scanners = scanners == null
                ? List.of()
                : scanners.stream()
                .filter(scanner -> this.scannerPipelinePlan.stageFor(scanner.name()).isPresent())
                .toList();
        this.rawReportRetention = properties.rawReportRetention();
        this.sandbox = properties.sandbox();
        this.publicationEligibility = properties.publicationEligibility();
        this.scopeInferenceCatalog = scopeInferenceCatalog;
        this.scopeScannerEnabled = properties.scopeScannerEnabled();
        this.publicationProvenance = new ArtifactProvenance(
                properties.provenanceSourceRepo(),
                properties.provenanceSourceCommit(),
                properties.provenanceGeneratedBy(),
                properties.provenanceGeneratorCommit()
        );
        this.signer = signer;
        this.objectMapper = objectMapper;
        this.metadataStore = metadataStore;
        this.nativeMetadataExtractor = nativeMetadataExtractor;
        this.nativeMetadataExporter = nativeMetadataExporter;
    }

    public ArtifactRecord upload(
            String groupId,
            String artifactId,
            String version,
            String packaging,
            MeshingressArtifactType type,
            List<String> requestedScopes,
            MultipartFile file,
            RepositoryRequestContext context
    ) {
        try {
            ArtifactCoordinate coordinate = coordinate(groupId, artifactId, version, packaging);
            StoredArtifactBlob blob = storage.storeArtifact(coordinate, file.getOriginalFilename(), file.getInputStream());
            List<dev.mrk.meshingress.artifact.model.ArtifactFileEntry> files = storage.extractArchiveToQuarantine(coordinate, blob.path());
            ArtifactRecord record = new ArtifactRecord(
                    coordinate,
                    type,
                    ArtifactTrustStatus.QUARANTINED,
                    blob.uri(),
                    blob.checksum(),
                    files,
                    new ArtifactScopeDeclaration(requestedScopes, List.of(), List.of(), List.of()),
                    null,
                    OffsetDateTime.now(),
                    OffsetDateTime.now()
            );
            metadataStore.saveArtifact(record, blob.path());
            appendLifecycleEvent(coordinate, "UPLOAD", null, record.trustStatus().name(), context, "Artifact uploaded to quarantine.");
            return record;
        } catch (RepositoryException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RepositoryException("artifact upload failed: " + exception.getMessage(), exception);
        }
    }

    public ArtifactRecord metadata(String groupId, String artifactId, String version) {
        return requireEntry(coordinate(groupId, artifactId, version, "jar")).record();
    }

    public Path artifactFile(String groupId, String artifactId, String version) {
        Path artifactPath = requireEntry(coordinate(groupId, artifactId, version, "jar")).artifactPath();
        if (!Files.isRegularFile(artifactPath)) {
            throw new RepositoryException("artifact file not found");
        }
        return artifactPath;
    }

    public Path artifactResourceFile(String groupId, String artifactId, String version, String resourceName) {
        if (!"application.yaml".equals(resourceName) && !"README.md".equals(resourceName)) {
            throw new RepositoryException("artifact resource is not available");
        }
        ArtifactRecord record = requireEntry(coordinate(groupId, artifactId, version, "jar")).record();
        Path resourcePath = storage.layout()
                .artifactDirectory(record.coordinate())
                .resolve("resources")
                .resolve(resourceName)
                .normalize();
        Path resourcesDirectory = storage.layout()
                .artifactDirectory(record.coordinate())
                .resolve("resources")
                .normalize();
        if (!resourcePath.startsWith(resourcesDirectory) || !Files.isRegularFile(resourcePath)) {
            throw new RepositoryException("artifact resource not found");
        }
        return resourcePath;
    }

    public ArtifactRecord assess(String groupId, String artifactId, String version, RepositoryRequestContext context) {
        ArtifactMetadataEntry entry = requireEntry(coordinate(groupId, artifactId, version, "jar"));
        ArtifactRecord current = entry.record();
        Path artifactPath = entry.artifactPath();
        List<ScannerResult> results = new ArrayList<>();
        ScannerRequest scannerRequest = new ScannerRequest(
                current.coordinate(),
                artifactPath,
                storage.layout().quarantineDirectory(current.coordinate()),
                rawReportRetention.scannerReports()
                        ? storage.layout().assessmentDirectory(current.coordinate())
                        : null,
                current.files()
        );
        for (ScannerAdapter scanner : scanners) {
            results.add(withPipelineSummary(scanner.scan(scannerRequest)));
        }
        List<String> inferredScopes = current.scopes().inferredScopes();
        if (isJar(current.coordinate().packaging())) {
            results.add(withPipelineSummary(generateSbomResult(current, artifactPath)));
        }
        if (sandbox.enabled() && isJar(current.coordinate().packaging())) {
            results.add(withPipelineSummary(generateSandboxResult(current, scannerRequest)));
        }
        if (scopeScannerEnabled && isJar(current.coordinate().packaging())) {
            JarScopeScanResult scopeScan = scanScopes(artifactPath);
            inferredScopes = scopeScan.inferredScopes().stream()
                    .sorted()
                    .toList();
            results.add(withPipelineSummary(toScannerResult(scopeScan)));
        }

        ArtifactTrustStatus nextStatus = statusFrom(results);
        ArtifactAssessmentSummary summary = summaryFrom(results);
        ArtifactRecord assessed = current.withTrustStatus(nextStatus, summary);
        ArtifactRecord updated = assessed.withScopes(
                new ArtifactScopeDeclaration(
                        current.scopes().requestedScopes(),
                        inferredScopes,
                        current.scopes().approvedScopes(),
                        current.scopes().deniedScopes()
                ),
                assessed.trustStatus()
        );
        metadataStore.saveArtifact(updated, artifactPath);
        metadataStore.saveAssessment(current.coordinate(), results);
        appendLifecycleEvent(
                current.coordinate(),
                "ASSESS",
                current.trustStatus().name(),
                updated.trustStatus().name(),
                context,
                "Artifact assessment completed."
        );
        writeJson(storage.layout().assessmentDirectory(current.coordinate()).resolve("assessment.json"), results);
        McpToolNativeMetadata nativeMetadata = nativeMetadataExtractor.extract(storage.layout().quarantineDirectory(current.coordinate()));
        nativeMetadataExporter.export(nativeMetadata, storage.layout().artifactDirectory(current.coordinate()));
        storage.cleanQuarantine(current.coordinate());
        return updated;
    }

    public List<ScannerResult> assessment(String groupId, String artifactId, String version) {
        ArtifactRecord record = requireEntry(coordinate(groupId, artifactId, version, "jar")).record();
        return metadataStore.findAssessment(record.coordinate());
    }

    public ArtifactRecord approve(String groupId, String artifactId, String version, ArtifactReviewRequest request, RepositoryRequestContext context) {
        ArtifactMetadataEntry entry = requireEntry(coordinate(groupId, artifactId, version, "jar"));
        ArtifactRecord current = entry.record();
        if (current.trustStatus() != ArtifactTrustStatus.REVIEW_PENDING) {
            throw new RepositoryException("artifact must be REVIEW_PENDING before approval");
        }
        ArtifactTrustStatus status = request.trustStatus() == null
                ? (request.deniedScopes().isEmpty() ? ArtifactTrustStatus.APPROVED_TRUSTED : ArtifactTrustStatus.APPROVED_LIMITED)
                : request.trustStatus();
        if (!status.installable()) {
            throw new RepositoryException("approval requires APPROVED_TRUSTED or APPROVED_LIMITED trust status");
        }

        ArtifactScopeDeclaration scopes = new ArtifactScopeDeclaration(
                current.scopes().requestedScopes(),
                current.scopes().inferredScopes(),
                request.approvedScopes().isEmpty() ? current.scopes().inferredScopes() : request.approvedScopes(),
                request.deniedScopes()
        );
        ArtifactRecord updated = current.withScopes(scopes, status);
        metadataStore.saveArtifact(updated, entry.artifactPath());
        appendLifecycleEvent(
                current.coordinate(),
                "APPROVE",
                current.trustStatus().name(),
                updated.trustStatus().name(),
                context,
                "Artifact review completed by " + (request.reviewer() == null || request.reviewer().isBlank() ? "unknown" : request.reviewer()) + "."
        );
        return updated;
    }

    public ArtifactRecord reject(String groupId, String artifactId, String version, ArtifactReviewRequest request, RepositoryRequestContext context) {
        ArtifactReviewRequest safeRequest = request == null ? ArtifactReviewRequest.empty() : request;
        ArtifactMetadataEntry entry = requireEntry(coordinate(groupId, artifactId, version, "jar"));
        ArtifactRecord current = entry.record();
        if (current.trustStatus() != ArtifactTrustStatus.REVIEW_PENDING) {
            throw new RepositoryException("artifact must be REVIEW_PENDING before rejection");
        }

        ArtifactScopeDeclaration scopes = new ArtifactScopeDeclaration(
                current.scopes().requestedScopes(),
                current.scopes().inferredScopes(),
                List.of(),
                safeRequest.deniedScopes()
        );
        ArtifactRecord updated = current.withScopes(scopes, ArtifactTrustStatus.REJECTED);
        metadataStore.saveArtifact(updated, entry.artifactPath());
        appendLifecycleEvent(
                current.coordinate(),
                "REJECT",
                current.trustStatus().name(),
                updated.trustStatus().name(),
                context,
                reviewReason("Artifact rejected", safeRequest)
        );
        return updated;
    }

    public ArtifactPublicationRecord publish(String groupId, String artifactId, String version, RepositoryRequestContext context) {
        ArtifactRecord record = requireEntry(coordinate(groupId, artifactId, version, "jar")).record();
        if (!record.trustStatus().installable()) {
            throw new RepositoryException("only approved artifacts can be published");
        }
        PublicationEligibilityDecision eligibilityDecision = evaluatePublicationEligibility(record);
        if (!eligibilityDecision.eligible()) {
            throw new RepositoryException("publication eligibility denied: " + String.join(", ", eligibilityDecision.reasonCodes()));
        }
        ArtifactPublicationRecord unsigned = new ArtifactPublicationRecord(
                record.coordinate(),
                record.type(),
                record.trustStatus(),
                record.artifactUri(),
                record.artifactChecksum(),
                record.scopes(),
                record.assessment(),
                publicationProvenance,
                eligibilityDecision,
                false,
                OffsetDateTime.now(),
                signer.keyId(),
                signer.algorithm(),
                ""
        );
        String unsignedPayload = toJson(unsigned);
        PublicationSignature signature = signer.sign(unsignedPayload);
        ArtifactPublicationRecord signed = new ArtifactPublicationRecord(
                unsigned.coordinate(),
                unsigned.type(),
                unsigned.trustStatus(),
                unsigned.artifactUri(),
                unsigned.artifactChecksum(),
                unsigned.scopePolicy(),
                unsigned.scanSummary(),
                unsigned.provenance(),
                unsigned.eligibilityDecision(),
                unsigned.revoked(),
                unsigned.publishedAt(),
                signature.keyId(),
                signature.algorithm(),
                signature.value()
        );
        metadataStore.savePublication(signed);
        appendLifecycleEvent(
                record.coordinate(),
                "PUBLISH",
                record.trustStatus().name(),
                record.trustStatus().name(),
                context,
                "Artifact publication record signed."
        );
        return signed;
    }

    public ArtifactPublicationRecord revoke(String groupId, String artifactId, String version, ArtifactReviewRequest request, RepositoryRequestContext context) {
        ArtifactReviewRequest safeRequest = request == null ? ArtifactReviewRequest.empty() : request;
        ArtifactMetadataEntry entry = requireEntry(coordinate(groupId, artifactId, version, "jar"));
        ArtifactRecord current = entry.record();
        ArtifactPublicationRecord currentPublication = metadataStore.findPublication(current.coordinate())
                .orElseThrow(() -> new RepositoryException("publication record not found"));
        if (currentPublication.revoked()) {
            throw new RepositoryException("publication is already revoked");
        }
        if (!current.trustStatus().installable()) {
            throw new RepositoryException("artifact must be approved before revocation");
        }

        ArtifactRecord updatedArtifact = current.withTrustStatus(ArtifactTrustStatus.REVOKED, current.assessment());
        metadataStore.saveArtifact(updatedArtifact, entry.artifactPath());

        ArtifactPublicationRecord unsigned = new ArtifactPublicationRecord(
                currentPublication.coordinate(),
                currentPublication.type(),
                ArtifactTrustStatus.REVOKED,
                currentPublication.artifactUri(),
                currentPublication.artifactChecksum(),
                currentPublication.scopePolicy(),
                currentPublication.scanSummary(),
                currentPublication.provenance(),
                currentPublication.eligibilityDecision(),
                true,
                currentPublication.publishedAt(),
                signer.keyId(),
                signer.algorithm(),
                ""
        );
        String unsignedPayload = toJson(unsigned);
        PublicationSignature signature = signer.sign(unsignedPayload);
        ArtifactPublicationRecord revoked = new ArtifactPublicationRecord(
                unsigned.coordinate(),
                unsigned.type(),
                unsigned.trustStatus(),
                unsigned.artifactUri(),
                unsigned.artifactChecksum(),
                unsigned.scopePolicy(),
                unsigned.scanSummary(),
                unsigned.provenance(),
                unsigned.eligibilityDecision(),
                unsigned.revoked(),
                unsigned.publishedAt(),
                signature.keyId(),
                signature.algorithm(),
                signature.value()
        );
        metadataStore.savePublication(revoked);
        appendLifecycleEvent(
                current.coordinate(),
                "REVOKE",
                current.trustStatus().name(),
                updatedArtifact.trustStatus().name(),
                context,
                reviewReason("Artifact publication revoked", safeRequest)
        );
        return revoked;
    }

    public ArtifactRecord delete(String groupId, String artifactId, String version, ArtifactReviewRequest request, RepositoryRequestContext context) {
        ArtifactReviewRequest safeRequest = request == null ? ArtifactReviewRequest.empty() : request;
        ArtifactMetadataEntry entry = requireEntry(coordinate(groupId, artifactId, version, "jar"));
        ArtifactRecord current = entry.record();
        if (current.trustStatus() == ArtifactTrustStatus.DELETED) {
            throw new RepositoryException("artifact is already deleted");
        }
        metadataStore.findPublication(current.coordinate())
                .filter(publication -> !publication.revoked())
                .ifPresent(publication -> {
                    throw new RepositoryException("published artifacts must be revoked before deletion");
                });

        ArtifactRecord deleted = current.withTrustStatus(ArtifactTrustStatus.DELETED, current.assessment());
        metadataStore.saveArtifact(deleted, entry.artifactPath());
        appendLifecycleEvent(
                current.coordinate(),
                "DELETE",
                current.trustStatus().name(),
                deleted.trustStatus().name(),
                context,
                reviewReason("Artifact deleted", safeRequest)
        );
        return deleted;
    }

    public ArtifactRecord restore(String groupId, String artifactId, String version, ArtifactReviewRequest request, RepositoryRequestContext context) {
        ArtifactReviewRequest safeRequest = request == null ? ArtifactReviewRequest.empty() : request;
        ArtifactMetadataEntry entry = requireEntry(coordinate(groupId, artifactId, version, "jar"));
        ArtifactRecord current = entry.record();
        if (current.trustStatus() != ArtifactTrustStatus.DELETED) {
            throw new RepositoryException("artifact must be DELETED before restore");
        }

        ArtifactLifecycleEvent deleteEvent = metadataStore.findLatestLifecycleEvent(current.coordinate(), "DELETE")
                .orElseThrow(() -> new RepositoryException("artifact delete event not found"));
        ArtifactTrustStatus restoredStatus = restoreStatus(deleteEvent.fromState());
        ArtifactRecord restored = current.withTrustStatus(restoredStatus, current.assessment());
        metadataStore.saveArtifact(restored, entry.artifactPath());
        appendLifecycleEvent(
                current.coordinate(),
                "RESTORE",
                current.trustStatus().name(),
                restored.trustStatus().name(),
                context,
                reviewReason("Artifact restored", safeRequest)
        );
        return restored;
    }

    public ArtifactPublicationRecord publication(String groupId, String artifactId, String version) {
        ArtifactCoordinate coordinate = coordinate(groupId, artifactId, version, "jar");
        return metadataStore.findPublication(coordinate)
                .orElseThrow(() -> new RepositoryException("publication record not found"));
    }

    public List<ArtifactReviewQueueItem> reviewQueue() {
        return metadataStore.findPendingReviewArtifacts();
    }

    private ArtifactAssessmentSummary summaryFrom(List<ScannerResult> results) {
        int findingCount = results.stream().mapToInt(result -> result.findings().size()).sum();
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("scannerCount", results.size());
        raw.put("findingCount", findingCount);
        raw.put("pipeline", scannerPipelinePlan.toSummary());
        raw.put("rawReportRetention", rawReportRetention.toSummary());
        results.stream()
                .filter(result -> result.scanner().equals("cyclonedx-sbom"))
                .findFirst()
                .ifPresent(result -> raw.put("sbom", result.rawSummary()));
        results.stream()
                .filter(result -> result.scanner().equals(sandbox.scannerName()))
                .findFirst()
                .ifPresent(result -> raw.put("sandbox", result.rawSummary()));
        return new ArtifactAssessmentSummary(
                findingCount == 0 ? "clean" : "findings",
                results.stream().map(ScannerResult::scanner).toList(),
                findingCount,
                raw
        );
    }

    private ScannerResult generateSbomResult(ArtifactRecord record, Path artifactPath) {
        try {
            CycloneDxSbom sbom = sbomGenerator.generate(artifactPath);
            Path rawReportPath = null;
            if (rawReportRetention.scannerReports()) {
                rawReportPath = storage.layout().assessmentDirectory(record.coordinate()).resolve("cyclonedx-sbom.json");
                Files.createDirectories(rawReportPath.getParent());
                Files.writeString(rawReportPath, sbom.toJson(objectMapper));
            }
            return toScannerResult(sbom, rawReportPath);
        } catch (Exception exception) {
            throw new RepositoryException("artifact SBOM generation failed: " + exception.getMessage(), exception);
        }
    }

    private ScannerResult generateSandboxResult(ArtifactRecord record, ScannerRequest request) {
        try {
            List<Finding> findings = sandboxFindings(request.fileEntries());
            Map<String, Object> isolation = Map.of(
                    "execution", "none",
                    "classLoading", false,
                    "processLaunch", false,
                    "networkAccess", false,
                    "hostSecretsAccess", false,
                    "repositoryInternalsAccess", false
            );
            Map<String, Object> rawReport = new LinkedHashMap<>();
            rawReport.put("scanner", sandbox.scannerName());
            rawReport.put("scannerVersion", sandbox.scannerVersion());
            rawReport.put("strategy", sandbox.strategy());
            rawReport.put("artifact", request.coordinate().display());
            rawReport.put("fileCount", request.fileEntries().size());
            rawReport.put("executableFileCount", request.fileEntries().stream().filter(ArtifactFileEntry::executable).count());
            rawReport.put("findingCount", findings.size());
            rawReport.put("isolation", isolation);
            rawReport.put("retention", rawReportRetention.toSummary());
            rawReport.put("findings", findings.stream()
                    .map(finding -> Map.of(
                            "severity", finding.severity(),
                            "code", finding.code(),
                            "message", finding.message(),
                            "path", finding.path()
                    ))
                    .toList());

            Path rawReportPath = null;
            if (rawReportRetention.sandboxReports()) {
                rawReportPath = storage.layout().assessmentDirectory(record.coordinate())
                        .resolve(sandbox.scannerName() + ".json");
                writeJson(rawReportPath, rawReport);
            }

            Map<String, Object> rawSummary = new LinkedHashMap<>();
            rawSummary.put("strategy", sandbox.strategy());
            rawSummary.put("isolation", isolation);
            rawSummary.put("retention", rawReportRetention.toSummary());
            rawSummary.put("fileCount", request.fileEntries().size());
            rawSummary.put("executableFileCount", request.fileEntries().stream().filter(ArtifactFileEntry::executable).count());
            rawSummary.put("findingCount", findings.size());
            if (rawReportPath != null) {
                rawSummary.put("rawReport", sandbox.scannerName() + ".json");
            }

            return new ScannerResult(
                    sandbox.scannerName(),
                    sandbox.scannerVersion(),
                    findings.isEmpty() ? ScannerStatus.PASSED : ScannerStatus.REVIEW,
                    findings,
                    rawSummary,
                    rawReportPath
            );
        } catch (Exception exception) {
            throw new RepositoryException("artifact sandbox analysis failed: " + exception.getMessage(), exception);
        }
    }

    private JarScopeScanResult scanScopes(Path artifactPath) {
        try {
            return bytecodeScopeScanner.scanReachableFromToolEntrypoints(artifactPath, scopeInferenceCatalog);
        } catch (Exception exception) {
            throw new RepositoryException("artifact scope inference failed: " + exception.getMessage(), exception);
        }
    }

    private ScannerResult toScannerResult(CycloneDxSbom sbom, Path rawReportPath) {
        Map<String, Object> rawSummary = new LinkedHashMap<>(sbom.summary());
        rawSummary.put("retention", rawReportRetention.toSummary());
        if (rawReportPath != null) {
            rawSummary.put("rawReport", "cyclonedx-sbom.json");
        }
        return new ScannerResult(
                "cyclonedx-sbom",
                sbom.specVersion(),
                ScannerStatus.PASSED,
                List.of(),
                rawSummary,
                rawReportPath
        );
    }

    private ScannerResult toScannerResult(JarScopeScanResult scopeScan) {
        List<ScopeFinding> scopeFindings = scopeScan.findings().stream()
                .sorted(Comparator.comparing(ScopeFinding::scope).thenComparing(ScopeFinding::location))
                .toList();
        Set<String> inferredScopes = new LinkedHashSet<>();
        List<Finding> findings = new ArrayList<>();
        for (ScopeFinding finding : scopeFindings) {
            inferredScopes.add(finding.scope());
            findings.add(new Finding(
                    "INFO",
                    finding.ruleId(),
                    finding.scope() + " inferred from " + finding.evidence(),
                    finding.location()
            ));
        }

        return new ScannerResult(
                "bytecode-scope-scanner",
                scopeScan.catalogVersion(),
                findings.isEmpty() ? ScannerStatus.PASSED : ScannerStatus.REVIEW,
                findings,
                Map.of(
                        "catalogVersion", scopeScan.catalogVersion(),
                        "analysisMode", scopeScan.analysisMode(),
                        "entrypoints", scopeScan.entrypoints(),
                        "diagnostics", scopeScan.diagnostics(),
                        "inferredScopes", List.copyOf(inferredScopes),
                        "findingCount", findings.size()
                ),
                null
        );
    }

    private List<Finding> sandboxFindings(List<ArtifactFileEntry> fileEntries) {
        List<Finding> findings = new ArrayList<>();
        for (ArtifactFileEntry entry : fileEntries.stream()
                .sorted(Comparator.comparing(ArtifactFileEntry::path))
                .toList()) {
            String lower = entry.path().toLowerCase(Locale.ROOT);
            if (entry.executable()) {
                findings.add(new Finding(
                        "MEDIUM",
                        "EXECUTABLE_PAYLOAD",
                        "JAR contains an executable payload; static sandbox strategy requires human review.",
                        entry.path()
                ));
            } else if (lower.endsWith(".jar") || lower.endsWith(".zip")) {
                findings.add(new Finding(
                        "LOW",
                        "NESTED_ARCHIVE",
                        "JAR contains a nested archive that the static sandbox records for reviewer attention.",
                        entry.path()
                ));
            }
        }
        return List.copyOf(findings);
    }

    private PublicationEligibilityDecision evaluatePublicationEligibility(ArtifactRecord record) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        if (!publicationEligibility.enabled()) {
            evidence.put("enabled", false);
            return PublicationEligibilityDecision.accepted(evidence);
        }

        List<String> reasonCodes = new ArrayList<>();
        List<ScannerResult> assessment = metadataStore.findAssessment(record.coordinate());
        Set<String> presentScanners = assessment.stream()
                .map(ScannerResult::scanner)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<String> requiredScanners = publicationEligibility.requiredScanners().isEmpty()
                ? scannerPipelinePlan.requiredScanners()
                : publicationEligibility.requiredScanners();
        for (String scanner : requiredScanners) {
            ScannerResult result = assessment.stream()
                    .filter(candidate -> candidate.scanner().equals(scanner))
                    .findFirst()
                    .orElse(null);
            if (result == null) {
                reasonCodes.add("MISSING_REQUIRED_SCANNER:" + scanner);
            } else if (result.status() == ScannerStatus.BLOCKED || result.status() == ScannerStatus.FAILED) {
                reasonCodes.add("REQUIRED_SCANNER_NOT_ACCEPTED:" + scanner + ":" + result.status().name());
            }
        }

        int approvalCount = metadataStore.countLifecycleEvents(record.coordinate(), "APPROVE");
        if (approvalCount < publicationEligibility.minimumReviewerApprovals()) {
            reasonCodes.add("INSUFFICIENT_REVIEW_APPROVALS");
        }

        Set<String> approvedScopes = new LinkedHashSet<>(record.scopes().approvedScopes());
        Set<String> deniedScopes = record.scopes().deniedScopes().stream()
                .map(DeniedScope::scope)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        for (String deniedScope : deniedScopes) {
            if (approvedScopes.contains(deniedScope)) {
                reasonCodes.add("DENIED_SCOPE_APPROVED:" + deniedScope);
            }
        }
        for (String inferredScope : record.scopes().inferredScopes()) {
            if (!approvedScopes.contains(inferredScope) && !deniedScopes.contains(inferredScope)) {
                reasonCodes.add("INFERRED_SCOPE_UNREVIEWED:" + inferredScope);
            }
        }

        requireProvenance(publicationProvenance, reasonCodes);
        if (!publicationEligibility.trustedBuilders().isEmpty()
                && !publicationEligibility.trustedBuilders().contains(publicationProvenance.generatedBy())) {
            reasonCodes.add("UNTRUSTED_BUILDER:" + publicationProvenance.generatedBy());
        }
        String signingKeyId = signer.keyId();
        if (signingKeyId.isBlank() || signer.algorithm().isBlank()) {
            reasonCodes.add("SIGNING_KEY_MISSING");
        } else {
            if (!publicationEligibility.trustedSigningKeyIds().isEmpty()
                    && !publicationEligibility.trustedSigningKeyIds().contains(signingKeyId)) {
                reasonCodes.add("SIGNING_KEY_UNKNOWN:" + signingKeyId);
            }
            if (publicationEligibility.revokedSigningKeyIds().contains(signingKeyId)) {
                reasonCodes.add("SIGNING_KEY_REVOKED:" + signingKeyId);
            }
        }

        evidence.put("requiredScanners", requiredScanners);
        evidence.put("presentScanners", List.copyOf(presentScanners));
        evidence.put("minimumReviewerApprovals", publicationEligibility.minimumReviewerApprovals());
        evidence.put("reviewerApprovalCount", approvalCount);
        evidence.put("approvedScopes", record.scopes().approvedScopes());
        evidence.put("deniedScopes", record.scopes().deniedScopes());
        evidence.put("inferredScopes", record.scopes().inferredScopes());
        evidence.put("requiredProvenanceFields", publicationEligibility.requiredProvenanceFields());
        evidence.put("trustedBuilders", publicationEligibility.trustedBuilders());
        evidence.put("trustedSigningKeyIds", publicationEligibility.trustedSigningKeyIds());
        evidence.put("revokedSigningKeyIds", publicationEligibility.revokedSigningKeyIds());
        evidence.put("signingKeyId", signingKeyId);
        return reasonCodes.isEmpty()
                ? PublicationEligibilityDecision.accepted(evidence)
                : PublicationEligibilityDecision.denied(reasonCodes, evidence);
    }

    private void requireProvenance(ArtifactProvenance provenance, List<String> reasonCodes) {
        for (String field : publicationEligibility.requiredProvenanceFields()) {
            boolean missing = switch (field) {
                case "sourceRepo" -> provenance.sourceRepo().isBlank();
                case "sourceCommit" -> provenance.sourceCommit().isBlank();
                case "generatedBy" -> provenance.generatedBy().isBlank();
                case "generatorCommit" -> provenance.generatorCommit().isBlank();
                default -> false;
            };
            if (missing) {
                reasonCodes.add("PROVENANCE_FIELD_MISSING:" + field);
            }
        }
    }

    private ScannerResult withPipelineSummary(ScannerResult result) {
        ScannerPipelineStage stage = scannerPipelinePlan.stageOrDefault(result.scanner());
        Map<String, Object> rawSummary = new LinkedHashMap<>(result.rawSummary());
        rawSummary.put("pipeline", stage.toSummary());
        return new ScannerResult(
                result.scanner(),
                result.scannerVersion(),
                result.status(),
                result.findings(),
                rawSummary,
                result.rawReportPath()
        );
    }

    private boolean isJar(String packaging) {
        return packaging == null || packaging.isBlank() || "jar".equalsIgnoreCase(packaging);
    }

    private ArtifactTrustStatus statusFrom(List<ScannerResult> results) {
        if (results.stream().anyMatch(result -> result.status() == ScannerStatus.BLOCKED)) {
            return ArtifactTrustStatus.BLOCKED_POLICY;
        }
        if (results.stream().anyMatch(result -> result.status() == ScannerStatus.FAILED)) {
            return ArtifactTrustStatus.REVIEW_PENDING;
        }
        return ArtifactTrustStatus.REVIEW_PENDING;
    }

    private ArtifactMetadataEntry requireEntry(ArtifactCoordinate coordinate) {
        return metadataStore.findArtifact(coordinate)
                .orElseThrow(() -> new RepositoryException("artifact not found: " + coordinate.display()));
    }

    private ArtifactCoordinate coordinate(String groupId, String artifactId, String version, String packaging) {
        return new ArtifactCoordinate(groupId, artifactId, version, null, packaging);
    }

    private void appendLifecycleEvent(
            ArtifactCoordinate coordinate,
            String eventType,
            String fromState,
            String toState,
            RepositoryRequestContext context,
            String reason
    ) {
        RepositoryRequestContext safeContext = context == null ? RepositoryRequestContext.system() : context;
        metadataStore.appendLifecycleEvent(new ArtifactLifecycleEvent(
                coordinate,
                eventType,
                fromState,
                toState,
                safeContext.actor(),
                safeContext.requestId(),
                reason
        ));
    }

    private String reviewReason(String prefix, ArtifactReviewRequest request) {
        ArtifactReviewRequest safeRequest = request == null ? ArtifactReviewRequest.empty() : request;
        String reviewer = safeRequest.reviewer().isBlank() ? "unknown" : safeRequest.reviewer();
        String notes = safeRequest.notes().isBlank() ? "" : " Notes: " + safeRequest.notes();
        return prefix + " by " + reviewer + "." + notes;
    }

    private ArtifactTrustStatus restoreStatus(String fromState) {
        if (fromState == null || fromState.isBlank()) {
            throw new RepositoryException("artifact delete event is missing restore state");
        }
        try {
            ArtifactTrustStatus status = ArtifactTrustStatus.valueOf(fromState);
            if (status == ArtifactTrustStatus.DELETED) {
                throw new RepositoryException("artifact delete event cannot restore DELETED state");
            }
            return status;
        } catch (IllegalArgumentException exception) {
            throw new RepositoryException("artifact delete event has unknown restore state: " + fromState, exception);
        }
    }

    private void writeJson(Path path, Object value) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, toJson(value));
        } catch (Exception exception) {
            throw new RepositoryException("unable to write repository metadata: " + exception.getMessage(), exception);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new RepositoryException("unable to serialize repository metadata: " + exception.getMessage(), exception);
        }
    }
}

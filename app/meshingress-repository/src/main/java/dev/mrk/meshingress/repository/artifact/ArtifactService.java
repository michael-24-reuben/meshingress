package dev.mrk.meshingress.repository.artifact;

import dev.mrk.meshingress.artifact.model.ArtifactAssessmentSummary;
import dev.mrk.meshingress.artifact.model.ArtifactCoordinate;
import dev.mrk.meshingress.artifact.model.ArtifactPublicationRecord;
import dev.mrk.meshingress.artifact.model.ArtifactRecord;
import dev.mrk.meshingress.artifact.model.ArtifactScopeDeclaration;
import dev.mrk.meshingress.artifact.model.ArtifactTrustStatus;
import dev.mrk.meshingress.artifact.model.MeshingressArtifactType;
import dev.mrk.meshingress.artifact.publication.PublicationRecordSigner;
import dev.mrk.meshingress.artifact.publication.PublicationSignature;
import dev.mrk.meshingress.artifact.scope.BytecodeScopeScanner;
import dev.mrk.meshingress.artifact.scope.JarScopeScanResult;
import dev.mrk.meshingress.artifact.scope.ScopeFinding;
import dev.mrk.meshingress.artifact.scope.ScopeInferenceCatalog;
import dev.mrk.meshingress.artifact.security.ScannerAdapter;
import dev.mrk.meshingress.artifact.security.Finding;
import dev.mrk.meshingress.artifact.security.ScannerRequest;
import dev.mrk.meshingress.artifact.security.ScannerResult;
import dev.mrk.meshingress.artifact.security.ScannerStatus;
import dev.mrk.meshingress.repository.config.MeshingressRepositoryProperties;
import dev.mrk.meshingress.artifact.storage.FileSystemArtifactStorage;
import dev.mrk.meshingress.artifact.storage.StoredArtifactBlob;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ArtifactService {

    private final FileSystemArtifactStorage storage;
    private final List<ScannerAdapter> scanners;
    private final BytecodeScopeScanner bytecodeScopeScanner;
    private final ScopeInferenceCatalog scopeInferenceCatalog;
    private final boolean scopeScannerEnabled;
    private final PublicationRecordSigner signer;
    private final ObjectMapper objectMapper;
    private final Map<String, ArtifactRecord> records = new ConcurrentHashMap<>();
    private final Map<String, Path> artifactPaths = new ConcurrentHashMap<>();
    private final Map<String, List<ScannerResult>> assessments = new ConcurrentHashMap<>();
    private final Map<String, ArtifactPublicationRecord> publications = new ConcurrentHashMap<>();

    public ArtifactService(
            FileSystemArtifactStorage storage,
            List<ScannerAdapter> scanners,
            BytecodeScopeScanner bytecodeScopeScanner,
            ScopeInferenceCatalog scopeInferenceCatalog,
            MeshingressRepositoryProperties properties,
            PublicationRecordSigner signer,
            ObjectMapper objectMapper
    ) {
        this.storage = storage;
        this.scanners = scanners == null ? List.of() : List.copyOf(scanners);
        this.bytecodeScopeScanner = bytecodeScopeScanner;
        this.scopeInferenceCatalog = scopeInferenceCatalog;
        this.scopeScannerEnabled = properties.scopeScannerEnabled();
        this.signer = signer;
        this.objectMapper = objectMapper;
    }

    public ArtifactRecord upload(
            String groupId,
            String artifactId,
            String version,
            String packaging,
            MeshingressArtifactType type,
            List<String> requestedScopes,
            MultipartFile file
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
            records.put(key(coordinate), record);
            artifactPaths.put(key(coordinate), blob.path());
            writeMetadata(record);
            return record;
        } catch (RepositoryException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RepositoryException("artifact upload failed: " + exception.getMessage(), exception);
        }
    }

    public ArtifactRecord metadata(String groupId, String artifactId, String version) {
        return requireRecord(coordinate(groupId, artifactId, version, "jar"));
    }

    public ArtifactRecord assess(String groupId, String artifactId, String version) {
        ArtifactRecord current = requireRecord(coordinate(groupId, artifactId, version, "jar"));
        String key = key(current.coordinate());
        Path artifactPath = artifactPaths.get(key);
        List<ScannerResult> results = new ArrayList<>();
        for (ScannerAdapter scanner : scanners) {
            results.add(scanner.scan(new ScannerRequest(
                    current.coordinate(),
                    artifactPath,
                    storage.layout().quarantineDirectory(current.coordinate()),
                    current.files()
            )));
        }
        List<String> inferredScopes = current.scopes().inferredScopes();
        if (scopeScannerEnabled && isJar(current.coordinate().packaging())) {
            JarScopeScanResult scopeScan = scanScopes(artifactPath);
            inferredScopes = scopeScan.inferredScopes().stream()
                    .sorted()
                    .toList();
            results.add(toScannerResult(scopeScan));
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
        records.put(key, updated);
        assessments.put(key, List.copyOf(results));
        writeJson(storage.layout().assessmentDirectory(current.coordinate()).resolve("assessment.json"), results);
        writeMetadata(updated);
        return updated;
    }

    public List<ScannerResult> assessment(String groupId, String artifactId, String version) {
        ArtifactRecord record = requireRecord(coordinate(groupId, artifactId, version, "jar"));
        return assessments.getOrDefault(key(record.coordinate()), List.of());
    }

    public ArtifactRecord approve(String groupId, String artifactId, String version, ArtifactReviewRequest request) {
        ArtifactRecord current = requireRecord(coordinate(groupId, artifactId, version, "jar"));
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
        records.put(key(current.coordinate()), updated);
        writeJson(storage.layout().reviewDirectory(current.coordinate()).resolve("latest-review.json"), request);
        writeMetadata(updated);
        return updated;
    }

    public ArtifactPublicationRecord publish(String groupId, String artifactId, String version) {
        ArtifactRecord record = requireRecord(coordinate(groupId, artifactId, version, "jar"));
        if (!record.trustStatus().installable()) {
            throw new RepositoryException("only approved artifacts can be published");
        }
        ArtifactPublicationRecord unsigned = new ArtifactPublicationRecord(
                record.coordinate(),
                record.type(),
                record.trustStatus(),
                record.artifactUri(),
                record.artifactChecksum(),
                record.scopes(),
                record.assessment(),
                null,
                false,
                OffsetDateTime.now(),
                "",
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
                unsigned.revoked(),
                unsigned.publishedAt(),
                signature.algorithm(),
                signature.value()
        );
        publications.put(key(record.coordinate()), signed);
        writeJson(storage.layout().publicationDirectory(record.coordinate()).resolve("publication.json"), signed);
        return signed;
    }

    public ArtifactPublicationRecord publication(String groupId, String artifactId, String version) {
        ArtifactCoordinate coordinate = coordinate(groupId, artifactId, version, "jar");
        ArtifactPublicationRecord publication = publications.get(key(coordinate));
        if (publication == null) {
            throw new RepositoryException("publication record not found");
        }
        return publication;
    }

    private ArtifactAssessmentSummary summaryFrom(List<ScannerResult> results) {
        int findingCount = results.stream().mapToInt(result -> result.findings().size()).sum();
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("scannerCount", results.size());
        raw.put("findingCount", findingCount);
        raw.put("fakeScannerMvp", results.stream().anyMatch(result -> result.scanner().equals("fake-scanner")));
        return new ArtifactAssessmentSummary(
                findingCount == 0 ? "clean" : "findings",
                results.stream().map(ScannerResult::scanner).toList(),
                findingCount,
                raw
        );
    }

    private JarScopeScanResult scanScopes(Path artifactPath) {
        try {
            return bytecodeScopeScanner.scanReachableFromToolEntrypoints(artifactPath, scopeInferenceCatalog);
        } catch (Exception exception) {
            throw new RepositoryException("artifact scope inference failed: " + exception.getMessage(), exception);
        }
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

    private ArtifactRecord requireRecord(ArtifactCoordinate coordinate) {
        ArtifactRecord record = records.get(key(coordinate));
        if (record == null) {
            throw new RepositoryException("artifact not found: " + coordinate.display());
        }
        return record;
    }

    private ArtifactCoordinate coordinate(String groupId, String artifactId, String version, String packaging) {
        return new ArtifactCoordinate(groupId, artifactId, version, null, packaging);
    }

    private String key(ArtifactCoordinate coordinate) {
        return coordinate.display();
    }

    private void writeMetadata(ArtifactRecord record) {
        writeJson(storage.layout().metadataDirectory(record.coordinate()).resolve("record.json"), record);
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

package dev.mrk.meshingress.repository.artifact;

import dev.mrk.meshingress.artifact.model.ArtifactPublicationRecord;
import dev.mrk.meshingress.artifact.model.ArtifactRecord;
import dev.mrk.meshingress.artifact.model.MeshingressArtifactType;
import dev.mrk.meshingress.artifact.security.ScannerResult;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
public class ArtifactController {

    private final ArtifactService artifactService;
    private final ArtifactLifecycleGuard lifecycleGuard;
    private final RepositoryAccessPolicy accessPolicy;

    public ArtifactController(
            ArtifactService artifactService,
            ArtifactLifecycleGuard lifecycleGuard,
            RepositoryAccessPolicy accessPolicy
    ) {
        this.artifactService = artifactService;
        this.lifecycleGuard = lifecycleGuard;
        this.accessPolicy = accessPolicy;
    }

    @PostMapping(path = "/artifact/{groupId}/{artifactId}/{version}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArtifactRecord upload(
            @PathVariable String groupId,
            @PathVariable String artifactId,
            @PathVariable String version,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "GENERATED_TOOL_MODULE") MeshingressArtifactType type,
            @RequestParam(defaultValue = "jar") String packaging,
            @RequestParam(required = false) List<String> requestedScopes,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Repository-Role", required = false) String role,
            @RequestHeader(value = "X-Repository-Actor", required = false) String actor,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        RepositoryRequestContext context = context(authorization, role, actor, requestId);
        accessPolicy.require(context, RepositoryAction.UPLOAD);
        return artifactService.upload(groupId, artifactId, version, packaging, type, requestedScopes, file, context);
    }

    @GetMapping("/artifact/{groupId}/{artifactId}/{version}/metadata")
    public ArtifactRecord metadata(
            @PathVariable String groupId,
            @PathVariable String artifactId,
            @PathVariable String version,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Repository-Role", required = false) String role,
            @RequestHeader(value = "X-Repository-Actor", required = false) String actor,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        accessPolicy.require(context(authorization, role, actor, requestId), RepositoryAction.READ);
        return artifactService.metadata(groupId, artifactId, version);
    }

    @GetMapping("/artifacts/jars")
    public List<ArtifactRecord> uploadedJarMetadata(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Repository-Role", required = false) String role,
            @RequestHeader(value = "X-Repository-Actor", required = false) String actor,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        accessPolicy.require(context(authorization, role, actor, requestId), RepositoryAction.READ);
        return artifactService.uploadedJarMetadata();
    }

    @GetMapping(path = "/artifact/{groupId}/{artifactId}/{version}/file", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> file(
            @PathVariable String groupId,
            @PathVariable String artifactId,
            @PathVariable String version,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Repository-Role", required = false) String role,
            @RequestHeader(value = "X-Repository-Actor", required = false) String actor,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        accessPolicy.require(context(authorization, role, actor, requestId), RepositoryAction.READ);
        Path artifactPath = artifactService.artifactFile(groupId, artifactId, version);
        try {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(Files.size(artifactPath))
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(artifactPath.getFileName().toString())
                            .build()
                            .toString())
                    .body(new FileSystemResource(artifactPath));
        } catch (Exception exception) {
            throw new RepositoryException("artifact download failed: " + exception.getMessage(), exception);
        }
    }

    @GetMapping(path = "/artifact/{groupId}/{artifactId}/{version}/resources/{resourceName}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<Resource> resource(
            @PathVariable String groupId,
            @PathVariable String artifactId,
            @PathVariable String version,
            @PathVariable String resourceName,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Repository-Role", required = false) String role,
            @RequestHeader(value = "X-Repository-Actor", required = false) String actor,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        accessPolicy.require(context(authorization, role, actor, requestId), RepositoryAction.READ);
        Path resourcePath;
        try {
            resourcePath = artifactService.artifactResourceFile(groupId, artifactId, version, resourceName);
        } catch (RepositoryException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
        try {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .contentLength(Files.size(resourcePath))
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(resourcePath.getFileName().toString())
                            .build()
                            .toString())
                    .body(new FileSystemResource(resourcePath));
        } catch (Exception exception) {
            throw new RepositoryException("artifact resource download failed: " + exception.getMessage(), exception);
        }
    }

    @PostMapping("/artifact/{groupId}/{artifactId}/{version}/assess")
    public ArtifactRecord assess(
            @PathVariable String groupId,
            @PathVariable String artifactId,
            @PathVariable String version,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Repository-Role", required = false) String role,
            @RequestHeader(value = "X-Repository-Actor", required = false) String actor,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        RepositoryRequestContext context = context(authorization, role, actor, requestId);
        accessPolicy.require(context, RepositoryAction.ASSESS);
        return artifactService.assess(groupId, artifactId, version, context);
    }

    @GetMapping("/artifact/{groupId}/{artifactId}/{version}/assessment")
    public List<ScannerResult> assessment(
            @PathVariable String groupId,
            @PathVariable String artifactId,
            @PathVariable String version,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Repository-Role", required = false) String role,
            @RequestHeader(value = "X-Repository-Actor", required = false) String actor,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        accessPolicy.require(context(authorization, role, actor, requestId), RepositoryAction.READ);
        return artifactService.assessment(groupId, artifactId, version);
    }

    @GetMapping("/artifact/reviews/pending")
    public List<ArtifactReviewQueueItem> pendingReviewQueue(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Repository-Role", required = false) String role,
            @RequestHeader(value = "X-Repository-Actor", required = false) String actor,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        accessPolicy.require(context(authorization, role, actor, requestId), RepositoryAction.READ);
        return artifactService.reviewQueue();
    }

    @PostMapping("/artifact/{groupId}/{artifactId}/{version}/approve")
    public ArtifactRecord approve(
            @PathVariable String groupId,
            @PathVariable String artifactId,
            @PathVariable String version,
            @RequestBody(required = false) ArtifactReviewRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Repository-Role", required = false) String role,
            @RequestHeader(value = "X-Repository-Actor", required = false) String actor,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        RepositoryRequestContext context = context(authorization, role, actor, requestId);
        accessPolicy.require(context, RepositoryAction.APPROVE);
        lifecycleGuard.requireAssessedBeforeApproval(groupId, artifactId, version);
        return artifactService.approve(groupId, artifactId, version, request == null ? ArtifactReviewRequest.empty() : request, context);
    }

    @PostMapping("/artifact/{groupId}/{artifactId}/{version}/reject")
    public ArtifactRecord reject(
            @PathVariable String groupId,
            @PathVariable String artifactId,
            @PathVariable String version,
            @RequestBody(required = false) ArtifactReviewRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Repository-Role", required = false) String role,
            @RequestHeader(value = "X-Repository-Actor", required = false) String actor,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        RepositoryRequestContext context = context(authorization, role, actor, requestId);
        accessPolicy.require(context, RepositoryAction.REJECT);
        lifecycleGuard.requireAssessedBeforeRejection(groupId, artifactId, version);
        return artifactService.reject(groupId, artifactId, version, request == null ? ArtifactReviewRequest.empty() : request, context);
    }

    @PostMapping("/artifact/{groupId}/{artifactId}/{version}/publish")
    public ArtifactPublicationRecord publish(
            @PathVariable String groupId,
            @PathVariable String artifactId,
            @PathVariable String version,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Repository-Role", required = false) String role,
            @RequestHeader(value = "X-Repository-Actor", required = false) String actor,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        RepositoryRequestContext context = context(authorization, role, actor, requestId);
        accessPolicy.require(context, RepositoryAction.PUBLISH);
        lifecycleGuard.requireReadyBeforePublication(groupId, artifactId, version);
        return artifactService.publish(groupId, artifactId, version, context);
    }

    @PostMapping("/artifact/{groupId}/{artifactId}/{version}/revoke")
    public ArtifactPublicationRecord revoke(
            @PathVariable String groupId,
            @PathVariable String artifactId,
            @PathVariable String version,
            @RequestBody(required = false) ArtifactReviewRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Repository-Role", required = false) String role,
            @RequestHeader(value = "X-Repository-Actor", required = false) String actor,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        RepositoryRequestContext context = context(authorization, role, actor, requestId);
        accessPolicy.require(context, RepositoryAction.REVOKE);
        lifecycleGuard.requirePublishedBeforeRevocation(groupId, artifactId, version);
        return artifactService.revoke(groupId, artifactId, version, request == null ? ArtifactReviewRequest.empty() : request, context);
    }

    @PostMapping("/artifact/{groupId}/{artifactId}/{version}/delete")
    public ArtifactRecord delete(
            @PathVariable String groupId,
            @PathVariable String artifactId,
            @PathVariable String version,
            @RequestBody(required = false) ArtifactReviewRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Repository-Role", required = false) String role,
            @RequestHeader(value = "X-Repository-Actor", required = false) String actor,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        RepositoryRequestContext context = context(authorization, role, actor, requestId);
        accessPolicy.require(context, RepositoryAction.DELETE);
        return artifactService.delete(groupId, artifactId, version, request == null ? ArtifactReviewRequest.empty() : request, context);
    }

    @PostMapping("/artifact/{groupId}/{artifactId}/{version}/restore")
    public ArtifactRecord restore(
            @PathVariable String groupId,
            @PathVariable String artifactId,
            @PathVariable String version,
            @RequestBody(required = false) ArtifactReviewRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Repository-Role", required = false) String role,
            @RequestHeader(value = "X-Repository-Actor", required = false) String actor,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        RepositoryRequestContext context = context(authorization, role, actor, requestId);
        accessPolicy.require(context, RepositoryAction.RESTORE);
        return artifactService.restore(groupId, artifactId, version, request == null ? ArtifactReviewRequest.empty() : request, context);
    }

    @GetMapping("/artifact/{groupId}/{artifactId}/{version}/publication")
    public ArtifactPublicationRecord publication(
            @PathVariable String groupId,
            @PathVariable String artifactId,
            @PathVariable String version,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Repository-Role", required = false) String role,
            @RequestHeader(value = "X-Repository-Actor", required = false) String actor,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        accessPolicy.require(context(authorization, role, actor, requestId), RepositoryAction.READ);
        return artifactService.publication(groupId, artifactId, version);
    }

    private RepositoryRequestContext context(String authorization, String role, String actor, String requestId) {
        return RepositoryRequestContext.fromHeaders(authorization, role, actor, requestId);
    }
}

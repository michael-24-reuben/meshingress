package dev.mrk.meshingress.repository.artifact;

import dev.mrk.meshingress.artifact.model.ArtifactPublicationRecord;
import dev.mrk.meshingress.artifact.model.ArtifactRecord;
import dev.mrk.meshingress.artifact.model.MeshingressArtifactType;
import dev.mrk.meshingress.artifact.security.ScannerResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/artifact")
@Tag(name = "Artifact repository", description = "Upload, assess, review, publish, and retrieve Meshingress tool artifacts.")
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

    @PostMapping(path = "/{groupId}/{artifactId}/{version}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an artifact", description = "Stores a JAR or supported artifact under its repository coordinate for later assessment and review.")
    @ArtifactOpenApiHeaders
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Artifact stored and its current lifecycle record returned."),
            @ApiResponse(responseCode = "400", description = "The coordinate, upload, or artifact metadata is invalid."),
            @ApiResponse(responseCode = "403", description = "The caller does not have the uploader or admin repository role.")
    })
    public ArtifactRecord upload(
            @PathVariable String groupId,
            @PathVariable String artifactId,
            @PathVariable String version,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "TOOL_MODULE") MeshingressArtifactType type,
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

    @GetMapping("/{groupId}/{artifactId}/{version}/metadata")
    @Operation(summary = "Get artifact metadata", description = "Returns the current lifecycle, checksum, scope, and assessment metadata for one artifact coordinate.")
    @ArtifactOpenApiHeaders
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current artifact record."),
            @ApiResponse(responseCode = "400", description = "The artifact coordinate is invalid or unavailable."),
            @ApiResponse(responseCode = "403", description = "The caller does not have a repository role that can read artifacts.")
    })
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

    @GetMapping("/jars")
    @Operation(summary = "List uploaded JAR artifacts", description = "Lists metadata for JAR artifacts known to the repository.")
    @ArtifactOpenApiHeaders
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Artifact metadata list."),
            @ApiResponse(responseCode = "403", description = "The caller does not have a repository role that can read artifacts.")
    })
    public List<ArtifactRecord> uploadedJarMetadata(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Repository-Role", required = false) String role,
            @RequestHeader(value = "X-Repository-Actor", required = false) String actor,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        accessPolicy.require(context(authorization, role, actor, requestId), RepositoryAction.READ);
        return artifactService.uploadedJarMetadata();
    }

    @GetMapping(path = "/{groupId}/{artifactId}/{version}/file", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Download an artifact file", description = "Downloads the stored artifact binary as an attachment.")
    @ArtifactOpenApiHeaders
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Artifact binary attachment."),
            @ApiResponse(responseCode = "400", description = "The artifact coordinate is invalid or the download could not be prepared."),
            @ApiResponse(responseCode = "403", description = "The caller does not have a repository role that can read artifacts.")
    })
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

    @GetMapping(path = "/{groupId}/{artifactId}/{version}/resources/{resourceName}", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Download an artifact resource", description = "Downloads one named text resource packaged with an artifact, such as its metadata or README.")
    @ArtifactOpenApiHeaders
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Named artifact resource attachment."),
            @ApiResponse(responseCode = "400", description = "The artifact coordinate is invalid or the resource could not be prepared."),
            @ApiResponse(responseCode = "403", description = "The caller does not have a repository role that can read artifacts."),
            @ApiResponse(responseCode = "404", description = "No resource with the requested name exists for this artifact.")
    })
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

    @PostMapping("/{groupId}/{artifactId}/{version}/assess")
    @Operation(summary = "Assess an artifact", description = "Runs the configured assessment pipeline and records its resulting lifecycle state.")
    @ArtifactOpenApiHeaders
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Artifact record after assessment."),
            @ApiResponse(responseCode = "400", description = "The coordinate is invalid or the assessment request cannot be completed."),
            @ApiResponse(responseCode = "403", description = "The caller does not have the reviewer or admin repository role.")
    })
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

    @GetMapping("/{groupId}/{artifactId}/{version}/assessment")
    @Operation(summary = "Get assessment results", description = "Returns scanner results recorded for one artifact coordinate.")
    @ArtifactOpenApiHeaders
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Scanner result list."),
            @ApiResponse(responseCode = "400", description = "The artifact coordinate is invalid or unavailable."),
            @ApiResponse(responseCode = "403", description = "The caller does not have a repository role that can read artifacts.")
    })
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

    @GetMapping("/reviews/pending")
    @Operation(summary = "List pending artifact reviews", description = "Lists artifacts that require repository review before approval and publication.")
    @ArtifactOpenApiHeaders
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pending review queue."),
            @ApiResponse(responseCode = "403", description = "The caller does not have a repository role that can read artifacts.")
    })
    public List<ArtifactReviewQueueItem> pendingReviewQueue(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Repository-Role", required = false) String role,
            @RequestHeader(value = "X-Repository-Actor", required = false) String actor,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        accessPolicy.require(context(authorization, role, actor, requestId), RepositoryAction.READ);
        return artifactService.reviewQueue();
    }

    @PostMapping("/{groupId}/{artifactId}/{version}/approve")
    @Operation(summary = "Approve an assessed artifact", description = "Records an approval decision and its allowed or denied scopes. The artifact must be assessed first.")
    @ArtifactOpenApiHeaders
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Artifact record after approval."),
            @ApiResponse(responseCode = "400", description = "The artifact is not ready for approval or the review decision is invalid."),
            @ApiResponse(responseCode = "403", description = "The caller does not have the reviewer or admin repository role.")
    })
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

    @PostMapping("/{groupId}/{artifactId}/{version}/reject")
    @Operation(summary = "Reject an assessed artifact", description = "Records a rejection decision. The artifact must be assessed first.")
    @ArtifactOpenApiHeaders
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Artifact record after rejection."),
            @ApiResponse(responseCode = "400", description = "The artifact is not ready for rejection or the review decision is invalid."),
            @ApiResponse(responseCode = "403", description = "The caller does not have the reviewer or admin repository role.")
    })
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

    @PostMapping("/{groupId}/{artifactId}/{version}/publish")
    @Operation(summary = "Publish an approved artifact", description = "Creates the signed publication record for an artifact that is eligible for publication.")
    @ArtifactOpenApiHeaders
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Signed artifact publication record."),
            @ApiResponse(responseCode = "400", description = "The artifact is not ready or eligible for publication."),
            @ApiResponse(responseCode = "403", description = "The caller does not have the publisher or admin repository role.")
    })
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

    @PostMapping("/{groupId}/{artifactId}/{version}/revoke")
    @Operation(summary = "Revoke an artifact publication", description = "Revokes a previously published artifact and records the supplied review details.")
    @ArtifactOpenApiHeaders
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Revoked artifact publication record."),
            @ApiResponse(responseCode = "400", description = "The artifact is not currently published or the review decision is invalid."),
            @ApiResponse(responseCode = "403", description = "The caller does not have the publisher or admin repository role.")
    })
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

    @PostMapping("/{groupId}/{artifactId}/{version}/delete")
    @Operation(summary = "Mark an artifact deleted", description = "Records a deletion decision for an artifact without removing its lifecycle evidence.")
    @ArtifactOpenApiHeaders
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Artifact record after deletion."),
            @ApiResponse(responseCode = "400", description = "The coordinate or review decision is invalid."),
            @ApiResponse(responseCode = "403", description = "Only the admin repository role can delete artifacts.")
    })
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

    @PostMapping("/{groupId}/{artifactId}/{version}/restore")
    @Operation(summary = "Restore an artifact", description = "Restores an artifact lifecycle record using the supplied review details.")
    @ArtifactOpenApiHeaders
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Artifact record after restoration."),
            @ApiResponse(responseCode = "400", description = "The coordinate or review decision is invalid."),
            @ApiResponse(responseCode = "403", description = "Only the admin repository role can restore artifacts.")
    })
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

    @GetMapping("/{groupId}/{artifactId}/{version}/publication")
    @Operation(summary = "Get an artifact publication", description = "Returns the signed publication record, including eligibility and revocation state, for one artifact coordinate.")
    @ArtifactOpenApiHeaders
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Artifact publication record."),
            @ApiResponse(responseCode = "400", description = "The artifact coordinate is invalid or unavailable."),
            @ApiResponse(responseCode = "403", description = "The caller does not have a repository role that can read artifacts.")
    })
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

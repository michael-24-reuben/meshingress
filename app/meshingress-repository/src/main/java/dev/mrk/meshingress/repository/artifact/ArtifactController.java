package dev.mrk.meshingress.repository.artifact;

import dev.mrk.meshingress.artifact.model.ArtifactPublicationRecord;
import dev.mrk.meshingress.artifact.model.ArtifactRecord;
import dev.mrk.meshingress.artifact.model.MeshingressArtifactType;
import dev.mrk.meshingress.artifact.security.ScannerResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class ArtifactController {

    private final ArtifactService artifactService;

    public ArtifactController(ArtifactService artifactService) {
        this.artifactService = artifactService;
    }

    @PostMapping(path = "/artifact/{groupId}/{artifactId}/{version}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArtifactRecord upload(
            @PathVariable String groupId,
            @PathVariable String artifactId,
            @PathVariable String version,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "GENERATED_TOOL_MODULE") MeshingressArtifactType type,
            @RequestParam(defaultValue = "jar") String packaging,
            @RequestParam(required = false) List<String> requestedScopes
    ) {
        return artifactService.upload(groupId, artifactId, version, packaging, type, requestedScopes, file);
    }

    @GetMapping("/artifact/{groupId}/{artifactId}/{version}/metadata")
    public ArtifactRecord metadata(
            @PathVariable String groupId,
            @PathVariable String artifactId,
            @PathVariable String version
    ) {
        return artifactService.metadata(groupId, artifactId, version);
    }

    @PostMapping("/artifact/{groupId}/{artifactId}/{version}/assess")
    public ArtifactRecord assess(
            @PathVariable String groupId,
            @PathVariable String artifactId,
            @PathVariable String version
    ) {
        return artifactService.assess(groupId, artifactId, version);
    }

    @GetMapping("/artifact/{groupId}/{artifactId}/{version}/assessment")
    public List<ScannerResult> assessment(
            @PathVariable String groupId,
            @PathVariable String artifactId,
            @PathVariable String version
    ) {
        return artifactService.assessment(groupId, artifactId, version);
    }

    @PostMapping("/artifact/{groupId}/{artifactId}/{version}/approve")
    public ArtifactRecord approve(
            @PathVariable String groupId,
            @PathVariable String artifactId,
            @PathVariable String version,
            @RequestBody(required = false) ArtifactReviewRequest request
    ) {
        return artifactService.approve(groupId, artifactId, version, request == null ? ArtifactReviewRequest.empty() : request);
    }

    @PostMapping("/artifact/{groupId}/{artifactId}/{version}/publish")
    public ArtifactPublicationRecord publish(
            @PathVariable String groupId,
            @PathVariable String artifactId,
            @PathVariable String version
    ) {
        return artifactService.publish(groupId, artifactId, version);
    }

    @GetMapping("/artifact/{groupId}/{artifactId}/{version}/publication")
    public ArtifactPublicationRecord publication(
            @PathVariable String groupId,
            @PathVariable String artifactId,
            @PathVariable String version
    ) {
        return artifactService.publication(groupId, artifactId, version);
    }
}

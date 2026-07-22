package dev.mrk.meshingress.repository.artifact;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Header contract shared by every artifact-repository operation.
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Parameters({
        @Parameter(name = "Authorization", in = ParameterIn.HEADER, description = "Optional bearer credential propagated with the repository request.", example = "Bearer <token>"),
        @Parameter(name = "X-Repository-Role", in = ParameterIn.HEADER, description = "Repository role or roles, separated by commas or whitespace. Required by the access policy; use uploader, reviewer, publisher, or admin as appropriate.", example = "reviewer"),
        @Parameter(name = "X-Repository-Actor", in = ParameterIn.HEADER, description = "Optional caller identity recorded with the repository action.", example = "release-bot"),
        @Parameter(name = "X-Request-Id", in = ParameterIn.HEADER, description = "Optional caller correlation identifier.", example = "req-123")
})
public @interface ArtifactOpenApiHeaders {
}

package dev.mrk.meshingress.runtime.artifacts;

public interface ToolArtifactResolver {
    boolean supports(ToolArtifactSource source);

    ResolvedToolArtifact resolve(ToolArtifactSource source, ToolArtifactResolutionContext context);
}

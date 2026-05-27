package dev.mrk.meshingress.runtime.artifacts;

import java.util.List;

public class ToolArtifactResolverChain implements ToolArtifactResolver {

    private final List<ToolArtifactResolver> resolvers;

    public ToolArtifactResolverChain(List<ToolArtifactResolver> resolvers) {
        this.resolvers = List.copyOf(resolvers);
    }

    @Override
    public boolean supports(ToolArtifactSource source) {
        return resolvers.stream().anyMatch(resolver -> resolver.supports(source));
    }

    @Override
    public ResolvedToolArtifact resolve(ToolArtifactSource source, ToolArtifactResolutionContext context) {
        return resolvers.stream()
                .filter(resolver -> resolver.supports(source))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No tool artifact resolver supports " + source.getClass().getName()))
                .resolve(source, context);
    }
}

package dev.mrk.meshingress.runtime.artifacts;

public sealed interface ToolArtifactSource
        permits MavenCoordinatesSource, LocalJarSource, PluginDirectorySource {
}

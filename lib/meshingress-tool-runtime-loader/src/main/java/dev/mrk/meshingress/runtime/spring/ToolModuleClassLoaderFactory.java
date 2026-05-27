package dev.mrk.meshingress.runtime.spring;

import dev.mrk.meshingress.runtime.artifacts.ResolvedToolArtifact;

import java.net.URLClassLoader;

public interface ToolModuleClassLoaderFactory {
    URLClassLoader create(ResolvedToolArtifact artifact);
}

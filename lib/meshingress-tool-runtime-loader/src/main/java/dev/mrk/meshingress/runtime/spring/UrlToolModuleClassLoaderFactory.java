package dev.mrk.meshingress.runtime.spring;

import dev.mrk.meshingress.runtime.artifacts.ResolvedToolArtifact;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class UrlToolModuleClassLoaderFactory implements ToolModuleClassLoaderFactory {

    @Override
    public URLClassLoader create(ResolvedToolArtifact artifact) {
        URL[] urls = artifact.runtimeClasspath().stream()
                .map(path -> {
                    try {
                        return path.toUri().toURL();
                    } catch (MalformedURLException exception) {
                        throw new IllegalArgumentException("Invalid tool module classpath entry: " + path, exception);
                    }
                })
                .toArray(URL[]::new);
        return new URLClassLoader("meshingress-tool-" + artifact.moduleId().value(), urls, getClass().getClassLoader());
    }
}

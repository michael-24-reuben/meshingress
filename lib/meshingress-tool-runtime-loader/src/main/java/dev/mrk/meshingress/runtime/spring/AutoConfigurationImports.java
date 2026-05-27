package dev.mrk.meshingress.runtime.spring;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

public final class AutoConfigurationImports {

    public static final String RESOURCE = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";

    private AutoConfigurationImports() {
    }

    public static List<String> read(ClassLoader classLoader) {
        try {
            Set<String> classNames = new LinkedHashSet<>();
            if (classLoader instanceof URLClassLoader urlClassLoader) {
                readUrlClassPath(urlClassLoader, classNames);
                return List.copyOf(classNames);
            }
            var resources = classLoader.getResources(RESOURCE);
            while (resources.hasMoreElements()) {
                readImports(resources.nextElement().openStream(), classNames);
            }
            return List.copyOf(classNames);
        } catch (IOException | URISyntaxException exception) {
            throw new IllegalStateException("Unable to read Spring auto-configuration imports from tool module", exception);
        }
    }

    private static void readUrlClassPath(URLClassLoader classLoader, Set<String> classNames) throws IOException, URISyntaxException {
        for (URL url : classLoader.getURLs()) {
            if (!"file".equalsIgnoreCase(url.getProtocol())) {
                continue;
            }
            Path path = Path.of(url.toURI());
            if (Files.isDirectory(path)) {
                Path imports = path.resolve(RESOURCE);
                if (Files.isRegularFile(imports)) {
                    try (InputStream input = Files.newInputStream(imports)) {
                        readImports(input, classNames);
                    }
                }
                continue;
            }
            if (Files.isRegularFile(path)) {
                try (JarFile jar = new JarFile(path.toFile())) {
                    var entry = jar.getJarEntry(RESOURCE);
                    if (entry != null) {
                        try (InputStream input = jar.getInputStream(entry)) {
                            readImports(input, classNames);
                        }
                    }
                }
            }
        }
    }

    private static void readImports(InputStream input, Set<String> classNames) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .filter(line -> !line.startsWith("#"))
                    .filter(line -> !line.startsWith("org.springframework.boot."))
                    .forEach(classNames::add);
        }
    }
}

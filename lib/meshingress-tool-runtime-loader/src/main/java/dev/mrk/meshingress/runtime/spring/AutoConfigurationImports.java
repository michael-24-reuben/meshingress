package dev.mrk.meshingress.runtime.spring;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AutoConfigurationImports {

    public static final String RESOURCE = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";

    private AutoConfigurationImports() {
    }

    public static List<String> read(ClassLoader classLoader) {
        try {
            Set<String> classNames = new LinkedHashSet<>();
            var resources = classLoader.getResources(RESOURCE);
            while (resources.hasMoreElements()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resources.nextElement().openStream(), StandardCharsets.UTF_8))) {
                    reader.lines()
                            .map(String::trim)
                            .filter(line -> !line.isBlank())
                            .filter(line -> !line.startsWith("#"))
                            .forEach(classNames::add);
                }
            }
            return List.copyOf(classNames);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read Spring auto-configuration imports from tool module", exception);
        }
    }
}

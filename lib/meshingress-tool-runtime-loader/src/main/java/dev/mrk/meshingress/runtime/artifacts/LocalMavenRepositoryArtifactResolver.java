package dev.mrk.meshingress.runtime.artifacts;

import dev.mrk.meshingress.runtime.lifecycle.ToolModuleId;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LocalMavenRepositoryArtifactResolver implements ToolArtifactResolver {

    @Override
    public boolean supports(ToolArtifactSource source) {
        return source instanceof MavenCoordinatesSource;
    }

    @Override
    public ResolvedToolArtifact resolve(ToolArtifactSource source, ToolArtifactResolutionContext context) {
        MavenCoordinatesSource coordinates = (MavenCoordinatesSource) source;
        Path artifactBase = artifactBase(context.localRepository(), coordinates.groupId(), coordinates.artifactId(), coordinates.version());
        Path mainJar = artifactBase.resolve(fileName(coordinates.artifactId(), coordinates.version(), "jar"));
        if (!Files.isRegularFile(mainJar)) {
            throw new IllegalArgumentException("Maven tool artifact is not installed in the local repository: " + mainJar);
        }

        LinkedHashSet<Path> classpath = new LinkedHashSet<>();
        resolveClasspath(context.localRepository(), coordinates.groupId(), coordinates.artifactId(), coordinates.version(), classpath, new LinkedHashSet<>());

        ToolModuleId moduleId = ToolModuleId.of(coordinates.groupId(), coordinates.artifactId(), coordinates.version());
        ToolModuleDescriptor descriptor = new ToolModuleDescriptor(
                moduleId,
                coordinates.artifactId(),
                coordinates.version(),
                List.of(),
                Map.of("source", "local-maven", "groupId", coordinates.groupId(), "artifactId", coordinates.artifactId())
        );
        return new ResolvedToolArtifact(moduleId, mainJar, List.copyOf(classpath), coordinates, Map.of(), descriptor);
    }

    private void resolveClasspath(
            Path localRepository,
            String groupId,
            String artifactId,
            String version,
            LinkedHashSet<Path> classpath,
            Set<String> seen
    ) {
        String key = groupId + ":" + artifactId + ":" + version;
        if (!seen.add(key)) {
            return;
        }

        Path artifactBase = artifactBase(localRepository, groupId, artifactId, version);
        Path jar = artifactBase.resolve(fileName(artifactId, version, "jar"));
        if (Files.isRegularFile(jar)) {
            classpath.add(jar);
        }

        Path pom = artifactBase.resolve(fileName(artifactId, version, "pom"));
        if (!Files.isRegularFile(pom)) {
            return;
        }

        for (MavenDependency dependency : dependencies(pom)) {
            Path dependencyJar = artifactBase(localRepository, dependency.groupId(), dependency.artifactId(), dependency.version())
                    .resolve(fileName(dependency.artifactId(), dependency.version(), "jar"));
            if (Files.isRegularFile(dependencyJar)) {
                resolveClasspath(localRepository, dependency.groupId(), dependency.artifactId(), dependency.version(), classpath, seen);
            }
        }
    }

    private List<MavenDependency> dependencies(Path pom) {
        try (InputStream input = Files.newInputStream(pom)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(input);
            document.getDocumentElement().normalize();

            List<MavenDependency> dependencies = new ArrayList<>();
            NodeList nodes = document.getElementsByTagName("dependency");
            for (int index = 0; index < nodes.getLength(); index++) {
                Node node = nodes.item(index);
                if (!(node instanceof Element dependencyElement)) {
                    continue;
                }
                String groupId = childText(dependencyElement, "groupId");
                String artifactId = childText(dependencyElement, "artifactId");
                String version = childText(dependencyElement, "version");
                String scope = childText(dependencyElement, "scope");
                String optional = childText(dependencyElement, "optional");
                String type = childText(dependencyElement, "type");
                if (groupId.isBlank() || artifactId.isBlank() || version.isBlank()) {
                    continue;
                }
                if ("true".equalsIgnoreCase(optional)
                        || "test".equals(scope)
                        || "provided".equals(scope)
                        || "system".equals(scope)
                        || "import".equals(scope)
                        || (!type.isBlank() && !"jar".equals(type))) {
                    continue;
                }
                dependencies.add(new MavenDependency(groupId, artifactId, version));
            }
            return dependencies;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse Maven POM: " + pom, exception);
        }
    }

    private Path artifactBase(Path localRepository, String groupId, String artifactId, String version) {
        return localRepository.resolve(groupId.replace('.', '/')).resolve(artifactId).resolve(version);
    }

    private String fileName(String artifactId, String version, String extension) {
        return artifactId + "-" + version + "." + extension;
    }

    private String childText(Element element, String tagName) {
        NodeList children = element.getElementsByTagName(tagName);
        if (children.getLength() == 0) {
            return "";
        }
        return children.item(0).getTextContent().trim();
    }

    private record MavenDependency(String groupId, String artifactId, String version) {
    }
}

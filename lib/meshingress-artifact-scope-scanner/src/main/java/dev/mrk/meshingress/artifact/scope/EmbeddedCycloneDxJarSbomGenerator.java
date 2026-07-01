package dev.mrk.meshingress.artifact.scope;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class EmbeddedCycloneDxJarSbomGenerator implements CycloneDxSbomGenerator {
    @Override
    public CycloneDxSbom generate(Path artifact) throws IOException {
        Objects.requireNonNull(artifact, "artifact");

        JarInspection inspection = inspectJar(artifact);
        MavenCoordinate rootCoordinate = inspection.rootCoordinate();
        CycloneDxSbom.Component root = new CycloneDxSbom.Component(
                rootCoordinate.isComplete() ? "application" : "file",
                rootCoordinate.bomRef("artifact:" + artifact.getFileName()),
                rootCoordinate.groupId(),
                rootCoordinate.displayName(artifact.getFileName().toString()),
                rootCoordinate.version(),
                rootCoordinate.purl(),
                "",
                Files.size(artifact),
                sha256(artifact),
                List.of(
                        new CycloneDxSbom.Property("meshingress:component-role", "root-artifact"),
                        new CycloneDxSbom.Property("meshingress:artifact-kind", "jar"),
                        new CycloneDxSbom.Property("meshingress:artifact-file-name", artifact.getFileName().toString())
                )
        );

        return new CycloneDxSbom(root, inspection.components());
    }

    private JarInspection inspectJar(Path artifact) throws IOException {
        List<CycloneDxSbom.Component> components = new ArrayList<>();
        List<MavenCoordinate> coordinates = new ArrayList<>();
        List<CycloneDxSbom.Component> dependencies = new ArrayList<>();
        try (JarFile jarFile = new JarFile(artifact.toFile())) {
            List<JarEntry> entries = jarFile.stream()
                    .filter(entry -> !entry.isDirectory())
                    .sorted(Comparator.comparing(JarEntry::getName))
                    .toList();
            for (JarEntry entry : entries) {
                if (isPomProperties(entry)) {
                    coordinates.add(readMavenCoordinate(jarFile, entry));
                }
                if (isPomXml(entry)) {
                    dependencies.addAll(readDependencyComponents(jarFile, entry));
                }
                components.add(toComponent(jarFile, entry));
            }
        }
        dependencies.stream()
                .sorted(Comparator.comparing(CycloneDxSbom.Component::group)
                        .thenComparing(CycloneDxSbom.Component::name)
                        .thenComparing(CycloneDxSbom.Component::version)
                        .thenComparing(CycloneDxSbom.Component::scope))
                .forEach(components::add);
        return new JarInspection(selectRootCoordinate(artifact, coordinates), List.copyOf(components));
    }

    private boolean isPomProperties(JarEntry entry) {
        return entry.getName().startsWith("META-INF/maven/") && entry.getName().endsWith("/pom.properties");
    }

    private boolean isPomXml(JarEntry entry) {
        return entry.getName().startsWith("META-INF/maven/") && entry.getName().endsWith("/pom.xml");
    }

    private MavenCoordinate readMavenCoordinate(JarFile jarFile, JarEntry entry) throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = jarFile.getInputStream(entry)) {
            properties.load(inputStream);
        }
        return new MavenCoordinate(
                property(properties, "groupId"),
                property(properties, "artifactId"),
                property(properties, "version"),
                ""
        );
    }

    private String property(Properties properties, String name) {
        String value = properties.getProperty(name);
        return value == null ? "" : value.trim();
    }

    private MavenCoordinate selectRootCoordinate(Path artifact, List<MavenCoordinate> coordinates) {
        if (coordinates.isEmpty()) {
            return MavenCoordinate.empty();
        }
        String fileName = artifact.getFileName().toString().toLowerCase(Locale.ROOT);
        return coordinates.stream()
                .filter(MavenCoordinate::hasArtifactId)
                .filter(coordinate -> fileName.startsWith(coordinate.artifactId().toLowerCase(Locale.ROOT) + "-")
                        || fileName.equalsIgnoreCase(coordinate.artifactId() + ".jar"))
                .findFirst()
                .orElse(coordinates.getFirst());
    }

    private List<CycloneDxSbom.Component> readDependencyComponents(JarFile jarFile, JarEntry entry) throws IOException {
        Document document = readXml(jarFile, entry);
        List<CycloneDxSbom.Component> components = new ArrayList<>();
        for (Element dependency : childElements(document.getDocumentElement(), "dependencies", "dependency")) {
            MavenCoordinate coordinate = new MavenCoordinate(
                    childText(dependency, "groupId"),
                    childText(dependency, "artifactId"),
                    childText(dependency, "version"),
                    childText(dependency, "scope")
            );
            if (coordinate.groupId().isBlank() || !coordinate.hasArtifactId()) {
                continue;
            }
            components.add(new CycloneDxSbom.Component(
                    "library",
                    coordinate.bomRef("maven-dependency:" + coordinate.groupId() + ":" + coordinate.artifactId()),
                    coordinate.groupId(),
                    coordinate.artifactId(),
                    coordinate.version(),
                    coordinate.purl(),
                    coordinate.scope(),
                    null,
                    null,
                    List.of(
                            new CycloneDxSbom.Property("meshingress:component-role", "maven-dependency"),
                            new CycloneDxSbom.Property("meshingress:maven-scope", coordinate.scope())
                    )
            ));
        }
        return List.copyOf(components);
    }

    private Document readXml(JarFile jarFile, JarEntry entry) throws IOException {
        try (InputStream inputStream = jarFile.getInputStream(entry)) {
            byte[] bytes = inputStream.readAllBytes();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
        } catch (ParserConfigurationException | SAXException exception) {
            throw new IOException("unable to parse Maven metadata " + entry.getName(), exception);
        }
    }

    private List<Element> childElements(Element parent, String firstName, String secondName) {
        List<Element> elements = new ArrayList<>();
        for (Element first : directChildren(parent, firstName)) {
            elements.addAll(directChildren(first, secondName));
        }
        return elements;
    }

    private List<Element> directChildren(Element parent, String localName) {
        List<Element> elements = new ArrayList<>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element element && localName.equals(elementLocalName(element))) {
                elements.add(element);
            }
        }
        return elements;
    }

    private String childText(Element parent, String localName) {
        return directChildren(parent, localName).stream()
                .findFirst()
                .map(Element::getTextContent)
                .map(String::trim)
                .orElse("");
    }

    private String elementLocalName(Element element) {
        return element.getLocalName() == null ? element.getNodeName() : element.getLocalName();
    }

    private CycloneDxSbom.Component toComponent(JarFile jarFile, JarEntry entry) throws IOException {
        return new CycloneDxSbom.Component(
                "file",
                "jar-entry:" + entry.getName(),
                entry.getName(),
                entry.getSize(),
                sha256(jarFile, entry),
                List.of(
                        new CycloneDxSbom.Property("meshingress:component-role", "jar-entry"),
                        new CycloneDxSbom.Property("meshingress:jar-entry:name", entry.getName())
                )
        );
    }

    private String sha256(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return sha256(inputStream);
        }
    }

    private String sha256(JarFile jarFile, JarEntry entry) throws IOException {
        try (InputStream inputStream = jarFile.getInputStream(entry)) {
            return sha256(inputStream);
        }
    }

    private String sha256(InputStream inputStream) throws IOException {
        MessageDigest digest = sha256Digest();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }

    private record JarInspection(MavenCoordinate rootCoordinate, List<CycloneDxSbom.Component> components) {
    }

    private record MavenCoordinate(String groupId, String artifactId, String version, String scope) {
        MavenCoordinate {
            groupId = groupId == null ? "" : groupId.trim();
            artifactId = artifactId == null ? "" : artifactId.trim();
            version = version == null ? "" : version.trim();
            scope = scope == null || scope.isBlank() ? "compile" : scope.trim();
        }

        static MavenCoordinate empty() {
            return new MavenCoordinate("", "", "", "");
        }

        boolean hasArtifactId() {
            return !artifactId.isBlank();
        }

        boolean isComplete() {
            return !groupId.isBlank() && !artifactId.isBlank() && !version.isBlank();
        }

        String displayName(String fallback) {
            return artifactId.isBlank() ? fallback : artifactId;
        }

        String bomRef(String fallback) {
            return isComplete() ? purl() : fallback;
        }

        String purl() {
            if (!isComplete()) {
                return "";
            }
            return "pkg:maven/" + encode(groupId) + "/" + encode(artifactId) + "@" + encode(version);
        }

        private static String encode(String value) {
            StringBuilder encoded = new StringBuilder();
            for (char item : value.toCharArray()) {
                if (Character.isLetterOrDigit(item) || item == '.' || item == '-' || item == '_') {
                    encoded.append(item);
                } else {
                    encoded.append('%').append(HexFormat.of().toHexDigits(item).substring(2));
                }
            }
            return encoded.toString();
        }
    }
}

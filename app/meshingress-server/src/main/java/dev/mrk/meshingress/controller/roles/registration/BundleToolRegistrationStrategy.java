package dev.mrk.meshingress.controller.roles.registration;

import dev.mrk.meshingress.api.tools.McpToolDescriptor;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.tools.registry.ToolRegistry;
import dev.mrk.meshingress.runtime.artifacts.ToolArtifactResolutionContext;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Service
class BundleToolRegistrationStrategy implements ToolRegistrationStrategy {

    private static final List<String> RESERVED_MAVEN_GROUP_PREFIXES = List.of(
            "dev.mrk.meshingress",
            "org.meshingress"
    );
    private static final List<String> RESERVED_CLASS_PATH_PREFIXES = List.of(
            "dev/mrk/meshingress/",
            "org/meshingress/"
    );

    private final MeshingressProperties properties;
    private final ToolArtifactResolutionContext resolutionContext;
    private final ToolRegistry toolRegistry;
    private final ToolRegistrationStore store;

    BundleToolRegistrationStrategy(
            MeshingressProperties properties,
            ToolArtifactResolutionContext resolutionContext,
            ToolRegistry toolRegistry,
            ToolRegistrationStore store
    ) {
        this.properties = properties;
        this.resolutionContext = resolutionContext;
        this.toolRegistry = toolRegistry;
        this.store = store;
    }

    @Override
    public ToolRegistrationPhase phase() {
        return ToolRegistrationPhase.BUNDLE;
    }

    @Override
    public ToolRegistrationResult register(ToolRegistrationRequest request, ToolRegistrationContext context) {
        McpToolDescriptor tool = toolRegistry.findTool(request.toolId()).orElse(null);
        McpFunctionDescriptor function = toolRegistry.findEnabledFunction(request.toolId()).orElse(null);
        if (tool != null || function != null) {
            return reconcileClasspathBundle(request, context, tool, function);
        }

        return installMavenArtifactIntoBundle(request, context);
    }

    private ToolRegistrationResult reconcileClasspathBundle(
            ToolRegistrationRequest request,
            ToolRegistrationContext context,
            McpToolDescriptor tool,
            McpFunctionDescriptor function
    ) {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("bundleId", request.bundle().bundleId());
        source.put("lookup", tool == null ? "function" : "tool");
        List<String> functions = tool == null
                ? List.of(function.name())
                : tool.functions().stream().map(McpFunctionDescriptor::name).toList();
        ToolRegistrationRecord record = new ToolRegistrationRecord(
                ToolRegistrationIds.registrationId(request),
                request.toolId(),
                phase(),
                ToolSourceKind.CLASSPATH_BUNDLE,
                "reconciled",
                source,
                context.actor(),
                context.call().requestId(),
                context.requestedAt(),
                null,
                null,
                functions
        );
        store.saveActive(record);
        return new ToolRegistrationResult(record, toolRegistry.registryVersion());
    }

    private ToolRegistrationResult installMavenArtifactIntoBundle(ToolRegistrationRequest request, ToolRegistrationContext context) {
        MavenCoordinatesSpec maven = request.maven();
        if (maven == null) {
            throw ToolRegistrationErrors.invalidParams(
                    "Requested bundled tool is not present on the server classpath. Provide maven coordinates to install it into the bundle module.",
                    "BUNDLE_TOOL_NOT_PRESENT"
            );
        }
        if (maven.groupId().isBlank() || maven.artifactId().isBlank() || maven.version().isBlank()) {
            throw ToolRegistrationErrors.invalidParams(
                    "maven.groupId, maven.artifactId, and maven.version are required.",
                    "TOOL_REGISTRATION_MAVEN_COORDINATES_REQUIRED"
            );
        }

        // TODO: Approve ADMIN and OPERATOR scope types
        rejectReservedMavenGroup(maven.groupId());
        if (properties.tools().registration().requireMavenVersionPin()
                && (maven.version().equalsIgnoreCase("LATEST") || maven.version().equalsIgnoreCase("RELEASE"))) {
            throw ToolRegistrationErrors.invalidParams(
                    "maven.version must be pinned.",
                    "TOOL_REGISTRATION_MAVEN_VERSION_PIN_REQUIRED"
            );
        }

        boolean localArtifactInstalled = false;
        String pomSource = "existing";
        if (request.localJar() != null) {
            pomSource = installLocalJarIntoMavenRepository(request.localJar(), maven);
            localArtifactInstalled = true;
        }

        Path localArtifact = localMavenArtifact(maven);
        if (!Files.isRegularFile(localArtifact)) {
            throw ToolRegistrationErrors.invalidParams(
                    "Bundle Maven artifact is not installed in the local repository.",
                    "BUNDLE_MAVEN_ARTIFACT_NOT_FOUND"
            );
        }

        Path bundlePom = Path.of(properties.tools().registration().bundlePomPath()).toAbsolutePath().normalize();
        boolean dependencyAdded = installDependency(bundlePom, maven);

        Map<String, String> source = new LinkedHashMap<>();
        source.put("bundleId", request.bundle().bundleId());
        source.put("groupId", maven.groupId());
        source.put("artifactId", maven.artifactId());
        source.put("version", maven.version());
        source.put("localRepositoryArtifact", localArtifact.toString());
        source.put("localRepositoryArtifactInstalled", Boolean.toString(localArtifactInstalled));
        source.put("mavenPomSource", pomSource);
        source.put("bundlePomPath", bundlePom.toString());
        source.put("dependencyAdded", Boolean.toString(dependencyAdded));

        ToolRegistrationRecord record = new ToolRegistrationRecord(
                ToolRegistrationIds.registrationId(request),
                request.toolId(),
                phase(),
                ToolSourceKind.MAVEN_BUNDLE,
                dependencyAdded ? "installed-restart-required" : "already-installed-restart-required",
                source,
                context.actor(),
                context.call().requestId(),
                context.requestedAt(),
                null,
                null,
                List.of(request.toolId())
        );
        store.saveActive(record);
        return new ToolRegistrationResult(record, toolRegistry.registryVersion());
    }

    private Path localMavenArtifact(MavenCoordinatesSpec maven) {
        return resolutionContext.localRepository()
                .resolve(maven.groupId().replace('.', '/'))
                .resolve(maven.artifactId())
                .resolve(maven.version())
                .resolve(maven.artifactId() + "-" + maven.version() + ".jar");
    }

    private String installLocalJarIntoMavenRepository(LocalJarSpec localJar, MavenCoordinatesSpec maven) {
        if (localJar.path() == null || localJar.path().isBlank()) {
            throw ToolRegistrationErrors.invalidParams("localJar.path is required.", "TOOL_REGISTRATION_LOCAL_JAR_PATH_REQUIRED");
        }

        MeshingressProperties.Tools.Registration config = properties.tools().registration();
        if (config.requireLocalJarChecksum() && (localJar.checksumSha256() == null || localJar.checksumSha256().isBlank())) {
            throw ToolRegistrationErrors.invalidParams(
                    "localJar.checksumSha256 is required.",
                    "TOOL_REGISTRATION_CHECKSUM_REQUIRED"
            );
        }

        Path sourceJar = resolveJar(localJar.path(), properties.tools().registration().localJarRoot());
        verifyChecksum(sourceJar, localJar.checksumSha256());
        rejectReservedClasses(sourceJar);
        PomMaterial pom = pomFor(localJar, sourceJar, maven);
        Path artifactDirectory = resolutionContext.localRepository()
                .resolve(maven.groupId().replace('.', '/'))
                .resolve(maven.artifactId())
                .resolve(maven.version());
        Path artifactJar = artifactDirectory.resolve(maven.artifactId() + "-" + maven.version() + ".jar");
        Path artifactPom = artifactDirectory.resolve(maven.artifactId() + "-" + maven.version() + ".pom");

        try {
            Files.createDirectories(artifactDirectory);
            ensureSameArtifactOrCreate(sourceJar, artifactJar);
            ensureSamePomOrCreate(pom.contents(), artifactPom);
            return pom.source();
        } catch (IOException exception) {
            throw ToolRegistrationErrors.invalidParams(
                    "Unable to install local jar into the local Maven repository: " + exception.getMessage(),
                    "BUNDLE_LOCAL_JAR_MAVEN_INSTALL_FAILED"
            );
        }
    }

    private PomMaterial pomFor(LocalJarSpec localJar, Path sourceJar, MavenCoordinatesSpec maven) {
        String pom;
        String source;
        if (localJar.pomPath() != null && !localJar.pomPath().isBlank()) {
            Path sourcePom = resolvePom(localJar.pomPath(), properties.tools().registration().localJarRoot());
            try {
                pom = Files.readString(sourcePom, StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw ToolRegistrationErrors.invalidParams(
                        "Unable to read localJar.pomPath: " + exception.getMessage(),
                        "BUNDLE_LOCAL_JAR_POM_READ_FAILED"
                );
            }
            source = "local-file";
        } else {
            pom = embeddedPom(sourceJar, maven);
            source = pom == null ? "generated-minimal" : "embedded";
            if (pom == null) {
                pom = minimalPom(maven);
            }
        }
        validatePomCoordinates(pom, maven);
        return new PomMaterial(pom, source);
    }

    private String embeddedPom(Path sourceJar, MavenCoordinatesSpec maven) {
        String entryName = "META-INF/maven/%s/%s/pom.xml".formatted(maven.groupId(), maven.artifactId());
        try (JarFile jar = new JarFile(sourceJar.toFile())) {
            JarEntry entry = jar.getJarEntry(entryName);
            if (entry == null) {
                return null;
            }
            try (InputStream input = jar.getInputStream(entry)) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            throw ToolRegistrationErrors.invalidParams(
                    "Unable to read the embedded Maven POM: " + exception.getMessage(),
                    "BUNDLE_EMBEDDED_POM_READ_FAILED"
            );
        }
    }

    private void validatePomCoordinates(String pom, MavenCoordinatesSpec maven) {
        try {
            DocumentBuilderFactory factory = secureDocumentBuilderFactory();
            Document document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(pom.getBytes(StandardCharsets.UTF_8)));
            Element project = document.getDocumentElement();
            String groupId = childText(project, "groupId");
            String artifactId = childText(project, "artifactId");
            String version = childText(project, "version");
            if (!maven.groupId().equals(groupId)
                    || !maven.artifactId().equals(artifactId)
                    || !maven.version().equals(version)) {
                throw ToolRegistrationErrors.invalidParams(
                        "The published Maven POM coordinates must match maven.groupId, maven.artifactId, and maven.version.",
                        "BUNDLE_MAVEN_POM_COORDINATES_MISMATCH"
                );
            }
        } catch (org.xml.sax.SAXException | IOException exception) {
            throw ToolRegistrationErrors.invalidParams(
                    "Unable to parse the published Maven POM: " + exception.getMessage(),
                    "BUNDLE_MAVEN_POM_INVALID"
            );
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw ToolRegistrationErrors.invalidParams(
                    "Unable to parse the published Maven POM: " + exception.getMessage(),
                    "BUNDLE_MAVEN_POM_INVALID"
            );
        }
    }

    private void ensureSameArtifactOrCreate(Path sourceJar, Path artifactJar) throws IOException {
        if (Files.isRegularFile(artifactJar)) {
            if (!sha256(sourceJar).equalsIgnoreCase(sha256(artifactJar))) {
                throw ToolRegistrationErrors.invalidParams(
                        "A different JAR is already stored for these Maven coordinates. Register a new version instead.",
                        "BUNDLE_MAVEN_COORDINATE_CONFLICT"
                );
            }
            return;
        }
        Files.copy(sourceJar, artifactJar);
    }

    private void ensureSamePomOrCreate(String pom, Path artifactPom) throws IOException {
        if (Files.isRegularFile(artifactPom)) {
            if (!Files.readString(artifactPom, StandardCharsets.UTF_8).equals(pom)) {
                throw ToolRegistrationErrors.invalidParams(
                        "A different Maven POM is already stored for these Maven coordinates. Register a new version instead.",
                        "BUNDLE_MAVEN_COORDINATE_CONFLICT"
                );
            }
            return;
        }
        Files.writeString(artifactPom, pom, StandardCharsets.UTF_8);
    }

    private void rejectReservedMavenGroup(String groupId) {
        boolean reserved = RESERVED_MAVEN_GROUP_PREFIXES.stream()
                .anyMatch(prefix -> groupId.equals(prefix) || groupId.startsWith(prefix + "."));
        if (reserved) {
            throw ToolRegistrationErrors.invalidParams(
                    "maven.groupId is reserved for native Meshingress modules.",
                    "BUNDLE_RESERVED_MAVEN_GROUP"
            );
        }
    }

    private void rejectReservedClasses(Path sourceJar) {
        try (JarFile jar = new JarFile(sourceJar.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String entryName = entries.nextElement().getName();
                if (entryName.endsWith(".class") && RESERVED_CLASS_PATH_PREFIXES.stream()
                        .anyMatch(prefix -> entryName.startsWith(prefix) || entryName.contains("/" + prefix))) {
                    throw ToolRegistrationErrors.invalidParams(
                            "The tool JAR contains classes in a reserved native Meshingress package.",
                            "BUNDLE_RESERVED_JAVA_PACKAGE"
                    );
                }
            }
        } catch (IOException exception) {
            throw ToolRegistrationErrors.invalidParams(
                    "Unable to inspect the local JAR packages: " + exception.getMessage(),
                    "BUNDLE_LOCAL_JAR_INSPECTION_FAILED"
            );
        }
    }

    private Path resolveJar(String jarPath, String root) {
        Path rootPath = Path.of(root).toAbsolutePath().normalize();
        Path requested = Path.of(jarPath);
        Path resolved = requested.isAbsolute() ? requested.toAbsolutePath().normalize() : rootPath.resolve(requested).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw ToolRegistrationErrors.invalidParams(
                    "localJar.path must resolve under meshingress.tools.registration.local-jar-root.",
                    "TOOL_REGISTRATION_LOCAL_JAR_OUTSIDE_ROOT"
            );
        }
        if (!Files.isRegularFile(resolved) || !resolved.getFileName().toString().endsWith(".jar")) {
            throw ToolRegistrationErrors.invalidParams(
                    "localJar.path must point to an existing .jar file.",
                    "TOOL_REGISTRATION_LOCAL_JAR_NOT_FOUND"
            );
        }
        return resolved;
    }

    private Path resolvePom(String pomPath, String root) {
        Path rootPath = Path.of(root).toAbsolutePath().normalize();
        Path requested = Path.of(pomPath);
        Path resolved = requested.isAbsolute() ? requested.toAbsolutePath().normalize() : rootPath.resolve(requested).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw ToolRegistrationErrors.invalidParams(
                    "localJar.pomPath must resolve under meshingress.tools.registration.local-jar-root.",
                    "TOOL_REGISTRATION_LOCAL_POM_OUTSIDE_ROOT"
            );
        }
        if (!Files.isRegularFile(resolved)) {
            throw ToolRegistrationErrors.invalidParams(
                    "localJar.pomPath must point to an existing Maven POM file.",
                    "TOOL_REGISTRATION_LOCAL_POM_NOT_FOUND"
            );
        }
        return resolved;
    }

    private void verifyChecksum(Path sourceJar, String expectedSha256) {
        if (expectedSha256 == null || expectedSha256.isBlank()) {
            return;
        }
        String actual = sha256(sourceJar);
        if (!actual.equalsIgnoreCase(expectedSha256)) {
            throw ToolRegistrationErrors.invalidParams(
                    "localJar.checksumSha256 does not match the local jar.",
                    "TOOL_REGISTRATION_CHECKSUM_MISMATCH"
            );
        }
    }

    private String sha256(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new DigestInputStream(Files.newInputStream(path), digest)) {
                input.transferTo(java.io.OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw ToolRegistrationErrors.invalidParams(
                    "Unable to calculate local jar checksum: " + exception.getMessage(),
                    "TOOL_REGISTRATION_CHECKSUM_FAILED"
            );
        }
    }

    private String minimalPom(MavenCoordinatesSpec maven) {
        return """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>%s</groupId>
                  <artifactId>%s</artifactId>
                  <version>%s</version>
                </project>
                """.formatted(maven.groupId(), maven.artifactId(), maven.version());
    }

    private boolean installDependency(Path bundlePom, MavenCoordinatesSpec maven) {
        if (!Files.isRegularFile(bundlePom)) {
            throw ToolRegistrationErrors.invalidParams(
                    "Bundle POM does not exist.",
                    "BUNDLE_POM_NOT_FOUND"
            );
        }
        try (InputStream input = Files.newInputStream(bundlePom)) {
            DocumentBuilderFactory documentFactory = secureDocumentBuilderFactory();
            Document document = documentFactory.newDocumentBuilder().parse(input);
            document.getDocumentElement().normalize();

            Element dependencies = firstChild(document.getDocumentElement(), "dependencies");
            if (dependencies != null && hasDependency(dependencies, maven)) {
                return false;
            }

            writeDependency(bundlePom, dependencies != null, maven);
            return true;
        } catch (Exception exception) {
            throw ToolRegistrationErrors.invalidParams(
                    "Unable to install dependency into bundle POM: " + exception.getMessage(),
                    "BUNDLE_POM_UPDATE_FAILED"
            );
        }
    }

    private boolean hasDependency(Element dependencies, MavenCoordinatesSpec maven) {
        NodeList nodes = dependencies.getElementsByTagName("dependency");
        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = nodes.item(index);
            if (!(node instanceof Element dependency)) {
                continue;
            }
            if (maven.groupId().equals(childText(dependency, "groupId"))
                    && maven.artifactId().equals(childText(dependency, "artifactId"))) {
                return true;
            }
        }
        return false;
    }

    private Element firstChild(Element parent, String name) {
        NodeList nodes = parent.getElementsByTagName(name);
        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = nodes.item(index);
            if (node instanceof Element element && node.getParentNode() == parent) {
                return element;
            }
        }
        return null;
    }

    private String childText(Element element, String tagName) {
        NodeList children = element.getElementsByTagName(tagName);
        if (children.getLength() == 0) {
            return "";
        }
        return children.item(0).getTextContent().trim();
    }

    private void writeDependency(Path bundlePom, boolean hasDependenciesElement, MavenCoordinatesSpec maven) throws Exception {
        String content = Files.readString(bundlePom, StandardCharsets.UTF_8);
        String newline = content.contains("\r\n") ? "\r\n" : "\n";
        String dependency = dependencyXml(maven, newline);

        String updated;
        if (hasDependenciesElement) {
            int closingDependencies = content.lastIndexOf("</dependencies>");
            if (closingDependencies < 0) {
                throw new IllegalStateException("POM has a dependencies element but no closing dependencies tag.");
            }
            updated = content.substring(0, closingDependencies)
                    + newline
                    + dependency
                    + "    "
                    + content.substring(closingDependencies);
        } else {
            int closingProject = content.lastIndexOf("</project>");
            if (closingProject < 0) {
                throw new IllegalStateException("POM has no closing project tag.");
            }
            String dependencies = "    <dependencies>" + newline
                    + dependency
                    + "    </dependencies>" + newline;
            updated = content.substring(0, closingProject)
                    + newline
                    + dependencies
                    + content.substring(closingProject);
        }
        Files.writeString(bundlePom, updated, StandardCharsets.UTF_8);
    }

    private String dependencyXml(MavenCoordinatesSpec maven, String newline) {
        return "        <dependency>" + newline
                + "            <groupId>" + maven.groupId() + "</groupId>" + newline
                + "            <artifactId>" + maven.artifactId() + "</artifactId>" + newline
                + "            <version>" + maven.version() + "</version>" + newline
                + "        </dependency>" + newline;
    }

    private DocumentBuilderFactory secureDocumentBuilderFactory() throws Exception {
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        documentFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return documentFactory;
    }

    private record PomMaterial(String contents, String source) {
    }
}

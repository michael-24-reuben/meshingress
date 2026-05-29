package dev.mrk.meshingress.artifact.scope;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

public class BytecodeScopeScanner {
    private static final String MCP_TOOL_ANNOTATION = "dev/mrk/meshingress/api/tools/annotation/McpTool";
    private static final String MCP_FUNCTION_ANNOTATION = "dev/mrk/meshingress/api/tools/annotation/McpFunction";
    private static final String MCP_TOOL_HANDLER = "dev/mrk/meshingress/api/tools/McpToolHandler";

    public JarScopeScanResult scan(Path jarPath, ScopeInferenceCatalog catalog) throws IOException {
        Objects.requireNonNull(jarPath, "jarPath");
        Objects.requireNonNull(catalog, "catalog");
        catalog.validate();

        ScanCollection collection = collect(jarPath, catalog);
        return new JarScopeScanResult(
                jarPath,
                catalog.version(),
                collection.findings().stream()
                        .map(candidate -> candidate.toFinding(true, "not-analyzed"))
                        .toList()
        );
    }

    public JarScopeScanResult scanReachableFromToolEntrypoints(Path jarPath, ScopeInferenceCatalog catalog) throws IOException {
        Objects.requireNonNull(jarPath, "jarPath");
        Objects.requireNonNull(catalog, "catalog");
        catalog.validate();

        ScanCollection collection = collect(jarPath, catalog);
        if (collection.entrypoints().isEmpty()) {
            return new JarScopeScanResult(
                    jarPath,
                    catalog.version(),
                    collection.findings().stream()
                            .map(candidate -> candidate.toFinding(true, "fallback:no-tool-entrypoints"))
                            .toList(),
                    "bytecode-full-no-tool-entrypoints",
                    List.of(),
                    List.of("No Meshingress tool entrypoints were found; full bytecode scan was used.")
            );
        }

        try {
            SootUpReachabilityAnalyzer.ReachabilityResult reachability =
                    new SootUpReachabilityAnalyzer().analyze(jarPath, collection.entrypoints());
            if (reachability.reachableMethods().isEmpty()) {
                return new JarScopeScanResult(
                        jarPath,
                        catalog.version(),
                        collection.findings().stream()
                                .map(candidate -> candidate.toFinding(true, "fallback:sootup-no-reachable-methods"))
                                .toList(),
                        "bytecode-full-sootup-fallback",
                        collection.entrypoints().stream().map(MethodReference::display).toList(),
                        reachability.diagnostics()
                );
            }

            Set<MethodReference> reachableMethods = reachability.reachableMethods();
            List<ScopeFinding> reachableFindings = collection.findings().stream()
                    .filter(candidate -> candidate.scannedMethod() == null || reachableMethods.contains(candidate.scannedMethod()))
                    .map(candidate -> candidate.toFinding(true, "reachable-from-tool-entrypoint"))
                    .toList();
            return new JarScopeScanResult(
                    jarPath,
                    catalog.version(),
                    reachableFindings,
                    "sootup-cha-reachable-tool-entrypoints",
                    reachability.entrypoints(),
                    reachability.diagnostics()
            );
        } catch (RuntimeException exception) {
            return new JarScopeScanResult(
                    jarPath,
                    catalog.version(),
                    collection.findings().stream()
                            .map(candidate -> candidate.toFinding(true, "fallback:sootup-error"))
                            .toList(),
                    "bytecode-full-sootup-error-fallback",
                    collection.entrypoints().stream().map(MethodReference::display).toList(),
                    List.of("SootUp reachability failed: " + exception.getMessage())
            );
        }
    }

    private ScanCollection collect(Path jarPath, ScopeInferenceCatalog catalog) throws IOException {
        List<FindingCandidate> findings = new ArrayList<>();
        Set<MethodReference> entrypoints = new LinkedHashSet<>();
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }
                try (InputStream stream = jarFile.getInputStream(entry)) {
                    ClassReader reader = new ClassReader(stream);
                    reader.accept(new ScopeClassVisitor(entry.getName(), catalog.rules(), findings, entrypoints), ClassReader.SKIP_FRAMES);
                }
            }
        }
        return new ScanCollection(List.copyOf(findings), Set.copyOf(entrypoints));
    }

    private record ScanCollection(
            List<FindingCandidate> findings,
            Set<MethodReference> entrypoints
    ) {
    }

    private record FindingCandidate(
            ScopeInferenceRule rule,
            ScopeMatcherDefinition matcher,
            String scannedClass,
            MethodReference scannedMethod,
            String location,
            String evidence
    ) {
        ScopeFinding toFinding(boolean reachableFromEntrypoint, String reachability) {
            return ScopeFinding.from(rule, matcher, scannedClass, scannedMethod, location, evidence, reachableFromEntrypoint, reachability);
        }
    }

    private static final class ScopeClassVisitor extends ClassVisitor {
        private final String entryName;
        private final List<ScopeInferenceRule> rules;
        private final List<FindingCandidate> findings;
        private final Set<MethodReference> entrypoints;
        private String className;
        private boolean toolClass;
        private boolean toolHandlerClass;

        private ScopeClassVisitor(
                String entryName,
                List<ScopeInferenceRule> rules,
                List<FindingCandidate> findings,
                Set<MethodReference> entrypoints
        ) {
            super(Opcodes.ASM9);
            this.entryName = entryName;
            this.rules = rules;
            this.findings = findings;
            this.entrypoints = entrypoints;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.className = name;
            this.toolHandlerClass = implementsToolHandler(interfaces);
            recordClassFinding(name, entryName);
            recordClassFinding(superName, entryName + " extends");
            if (interfaces != null) {
                for (String interfaceName : interfaces) {
                    recordClassFinding(interfaceName, entryName + " implements");
                }
            }
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            String owner = descriptorToOwner(descriptor);
            if (MCP_TOOL_ANNOTATION.equals(owner)) {
                toolClass = true;
            }
            recordAnnotationFinding(descriptor, null, entryName + " class annotation");
            return super.visitAnnotation(descriptor, visible);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodReference methodReference = new MethodReference(className, name, descriptor);
            String methodLocation = methodReference.display();
            MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new MethodVisitor(Opcodes.ASM9, delegate) {
                @Override
                public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
                    String owner = descriptorToOwner(annotationDescriptor);
                    if (toolClass && MCP_FUNCTION_ANNOTATION.equals(owner)) {
                        entrypoints.add(methodReference);
                    }
                    recordAnnotationFinding(annotationDescriptor, methodReference, methodLocation + " annotation");
                    return super.visitAnnotation(annotationDescriptor, visible);
                }

                @Override
                public void visitCode() {
                    if (toolHandlerClass && "call".equals(name)) {
                        entrypoints.add(methodReference);
                    }
                    super.visitCode();
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String invokedName, String invokedDescriptor, boolean isInterface) {
                    recordMethodFinding(owner, invokedName, invokedDescriptor, methodReference, methodLocation);
                    super.visitMethodInsn(opcode, owner, invokedName, invokedDescriptor, isInterface);
                }
            };
        }

        private boolean implementsToolHandler(String[] interfaces) {
            if (interfaces == null) {
                return false;
            }
            for (String interfaceName : interfaces) {
                if (MCP_TOOL_HANDLER.equals(interfaceName)) {
                    return true;
                }
            }
            return false;
        }

        private void recordMethodFinding(
                String owner,
                String methodName,
                String descriptor,
                MethodReference scannedMethod,
                String location
        ) {
            for (ScopeInferenceRule rule : rules) {
                for (ScopeMatcherDefinition matcher : rule.matchers()) {
                    if (matcher.type() != ScopeMatcherType.BYTECODE_METHOD) {
                        continue;
                    }
                    if (owner.equals(matcher.owner())
                            && wildcardMatches(methodName, matcher.namePattern())
                            && wildcardMatches(descriptor, matcher.descriptorPattern())) {
                        findings.add(new FindingCandidate(
                                rule,
                                matcher,
                                className,
                                scannedMethod,
                                location,
                                owner + "." + methodName + descriptor
                        ));
                    }
                }
            }
        }

        private void recordAnnotationFinding(String descriptor, MethodReference scannedMethod, String location) {
            String owner = descriptorToOwner(descriptor);
            recordTypedOwnerFinding(ScopeMatcherType.BYTECODE_ANNOTATION, owner, scannedMethod, location, owner);
        }

        private void recordClassFinding(String owner, String location) {
            if (owner == null || owner.isBlank()) {
                return;
            }
            recordTypedOwnerFinding(ScopeMatcherType.BYTECODE_CLASS, owner, null, location, owner);
        }

        private void recordTypedOwnerFinding(
                ScopeMatcherType type,
                String owner,
                MethodReference scannedMethod,
                String location,
                String evidence
        ) {
            for (ScopeInferenceRule rule : rules) {
                for (ScopeMatcherDefinition matcher : rule.matchers()) {
                    if (matcher.type() != type) {
                        continue;
                    }
                    if (owner.equals(matcher.owner())) {
                        findings.add(new FindingCandidate(rule, matcher, className, scannedMethod, location, evidence));
                    }
                }
            }
        }

        private static String descriptorToOwner(String descriptor) {
            if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
                return descriptor.substring(1, descriptor.length() - 1);
            }
            return descriptor;
        }

        private static boolean wildcardMatches(String value, String pattern) {
            if (pattern == null || pattern.isBlank()) {
                return true;
            }
            String regex = Pattern.quote(pattern)
                    .replace("%", "\\E.*\\Q")
                    .replace(".*", "\\E.*\\Q");
            return Pattern.matches(regex, value);
        }
    }
}

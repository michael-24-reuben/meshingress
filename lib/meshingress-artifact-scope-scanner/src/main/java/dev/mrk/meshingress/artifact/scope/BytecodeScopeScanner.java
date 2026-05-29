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
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

public class BytecodeScopeScanner {
    public JarScopeScanResult scan(Path jarPath, ScopeInferenceCatalog catalog) throws IOException {
        Objects.requireNonNull(jarPath, "jarPath");
        Objects.requireNonNull(catalog, "catalog");
        catalog.validate();

        List<ScopeFinding> findings = new ArrayList<>();
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }
                try (InputStream stream = jarFile.getInputStream(entry)) {
                    ClassReader reader = new ClassReader(stream);
                    reader.accept(new ScopeClassVisitor(entry.getName(), catalog.rules(), findings), ClassReader.SKIP_FRAMES);
                }
            }
        }

        return new JarScopeScanResult(jarPath, catalog.version(), findings);
    }

    private static final class ScopeClassVisitor extends ClassVisitor {
        private final String entryName;
        private final List<ScopeInferenceRule> rules;
        private final List<ScopeFinding> findings;
        private String className;

        private ScopeClassVisitor(String entryName, List<ScopeInferenceRule> rules, List<ScopeFinding> findings) {
            super(Opcodes.ASM9);
            this.entryName = entryName;
            this.rules = rules;
            this.findings = findings;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.className = name;
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
            recordAnnotationFinding(descriptor, entryName + " class annotation");
            return super.visitAnnotation(descriptor, visible);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            String methodLocation = className + "." + name + descriptor;
            MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new MethodVisitor(Opcodes.ASM9, delegate) {
                @Override
                public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
                    recordAnnotationFinding(annotationDescriptor, methodLocation + " annotation");
                    return super.visitAnnotation(annotationDescriptor, visible);
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String invokedName, String invokedDescriptor, boolean isInterface) {
                    recordMethodFinding(owner, invokedName, invokedDescriptor, methodLocation);
                    super.visitMethodInsn(opcode, owner, invokedName, invokedDescriptor, isInterface);
                }
            };
        }

        private void recordMethodFinding(String owner, String methodName, String descriptor, String location) {
            for (ScopeInferenceRule rule : rules) {
                for (ScopeMatcherDefinition matcher : rule.matchers()) {
                    if (matcher.type() != ScopeMatcherType.BYTECODE_METHOD) {
                        continue;
                    }
                    if (owner.equals(matcher.owner())
                            && wildcardMatches(methodName, matcher.namePattern())
                            && wildcardMatches(descriptor, matcher.descriptorPattern())) {
                        findings.add(ScopeFinding.from(
                                rule,
                                matcher,
                                className,
                                location,
                                owner + "." + methodName + descriptor
                        ));
                    }
                }
            }
        }

        private void recordAnnotationFinding(String descriptor, String location) {
            String owner = descriptorToOwner(descriptor);
            recordTypedOwnerFinding(ScopeMatcherType.BYTECODE_ANNOTATION, owner, location, owner);
        }

        private void recordClassFinding(String owner, String location) {
            if (owner == null || owner.isBlank()) {
                return;
            }
            recordTypedOwnerFinding(ScopeMatcherType.BYTECODE_CLASS, owner, location, owner);
        }

        private void recordTypedOwnerFinding(ScopeMatcherType type, String owner, String location, String evidence) {
            for (ScopeInferenceRule rule : rules) {
                for (ScopeMatcherDefinition matcher : rule.matchers()) {
                    if (matcher.type() != type) {
                        continue;
                    }
                    if (owner.equals(matcher.owner())) {
                        findings.add(ScopeFinding.from(rule, matcher, className, location, evidence));
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

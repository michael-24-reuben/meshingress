package dev.mrk.meshingress.artifact.scope;

import sootup.callgraph.CallGraph;
import sootup.callgraph.ClassHierarchyAnalysisAlgorithm;
import sootup.callgraph.CallGraphAlgorithm;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SourceType;
import sootup.core.signatures.MethodSignature;
import sootup.java.bytecode.frontend.inputlocation.ArchiveBasedAnalysisInputLocation;
import sootup.java.bytecode.frontend.inputlocation.DefaultRuntimeAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class SootUpReachabilityAnalyzer {

    ReachabilityResult analyze(Path jarPath, Set<MethodReference> entrypoints) {
        Objects.requireNonNull(jarPath, "jarPath");
        Objects.requireNonNull(entrypoints, "entrypoints");
        if (entrypoints.isEmpty()) {
            return new ReachabilityResult(Set.of(), List.of("No Meshingress tool entrypoints were found."), List.of());
        }

        List<String> diagnostics = new ArrayList<>();
        List<AnalysisInputLocation> inputLocations = List.of(
                new ArchiveBasedAnalysisInputLocation(jarPath, SourceType.Application),
                new DefaultRuntimeAnalysisInputLocation(SourceType.Library)
        );
        JavaView view = new JavaView(inputLocations);
        List<MethodSignature> entrySignatures = resolveEntrySignatures(view, entrypoints, diagnostics);
        if (entrySignatures.isEmpty()) {
            diagnostics.add("SootUp could not resolve any discovered tool entrypoint methods.");
            return new ReachabilityResult(Set.of(), List.copyOf(diagnostics), List.of());
        }

        CallGraphAlgorithm algorithm = new ClassHierarchyAnalysisAlgorithm(view);
        CallGraph callGraph = algorithm.initialize(entrySignatures);
        Set<MethodReference> reachableMethods = new LinkedHashSet<>();
        for (MethodSignature signature : callGraph.getMethodSignatures()) {
            reachableMethods.add(MethodReference.fromMethodSignature(signature));
        }
        for (MethodSignature entrySignature : entrySignatures) {
            reachableMethods.add(MethodReference.fromMethodSignature(entrySignature));
        }

        return new ReachabilityResult(
                Set.copyOf(reachableMethods),
                List.copyOf(diagnostics),
                entrySignatures.stream().map(MethodSignature::toString).toList()
        );
    }

    private List<MethodSignature> resolveEntrySignatures(
            JavaView view,
            Set<MethodReference> entrypoints,
            List<String> diagnostics
    ) {
        List<MethodSignature> signatures = new ArrayList<>();
        for (MethodReference entrypoint : entrypoints) {
            var classType = view.getIdentifierFactory().getClassType(entrypoint.ownerClassName());
            var sootClass = view.getClass(classType);
            if (sootClass.isEmpty()) {
                diagnostics.add("SootUp could not resolve entrypoint class " + entrypoint.ownerClassName() + ".");
                continue;
            }
            resolveEntrySignature(sootClass.get(), entrypoint, signatures, diagnostics);
        }
        return List.copyOf(signatures);
    }

    private void resolveEntrySignature(
            JavaSootClass sootClass,
            MethodReference entrypoint,
            List<MethodSignature> signatures,
            List<String> diagnostics
    ) {
        for (JavaSootMethod method : sootClass.getMethods()) {
            MethodReference candidate = MethodReference.fromSootMethod(method);
            if (candidate.equals(entrypoint)) {
                signatures.add(method.getSignature());
                return;
            }
        }
        diagnostics.add("SootUp could not resolve entrypoint method " + entrypoint.display() + ".");
    }

    record ReachabilityResult(
            Set<MethodReference> reachableMethods,
            List<String> diagnostics,
            List<String> entrypoints
    ) {
    }
}

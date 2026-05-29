package dev.mrk.meshingress.artifact.scope;

public record ScopeFinding(
        String scope,
        String ruleId,
        ScopeMatcherType matcherType,
        String confidence,
        boolean reviewOnly,
        String scannedClass,
        String scannedMethod,
        String location,
        String evidence,
        boolean reachableFromEntrypoint,
        String reachability
) {
    static ScopeFinding from(
            ScopeInferenceRule rule,
            ScopeMatcherDefinition matcher,
            String scannedClass,
            MethodReference scannedMethod,
            String location,
            String evidence,
            boolean reachableFromEntrypoint,
            String reachability
    ) {
        return new ScopeFinding(
                rule.scope(),
                rule.id(),
                matcher.type(),
                matcher.confidence(),
                matcher.reviewOnly(),
                scannedClass,
                scannedMethod == null ? "" : scannedMethod.display(),
                location,
                evidence,
                reachableFromEntrypoint,
                reachability == null ? "" : reachability
        );
    }
}

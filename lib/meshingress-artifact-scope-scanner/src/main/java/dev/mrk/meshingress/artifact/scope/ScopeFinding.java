package dev.mrk.meshingress.artifact.scope;

public record ScopeFinding(
        String scope,
        String ruleId,
        ScopeMatcherType matcherType,
        String confidence,
        boolean reviewOnly,
        String scannedClass,
        String location,
        String evidence
) {
    static ScopeFinding from(
            ScopeInferenceRule rule,
            ScopeMatcherDefinition matcher,
            String scannedClass,
            String location,
            String evidence
    ) {
        return new ScopeFinding(
                rule.scope(),
                rule.id(),
                matcher.type(),
                matcher.confidence(),
                matcher.reviewOnly(),
                scannedClass,
                location,
                evidence
        );
    }
}

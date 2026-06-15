package dev.mrk.meshingress.codeqlscope;

public record ScopeFinding(
        String scope,
        String ruleId,
        ScopeMatcherType matcherType,
        String confidence,
        boolean reviewOnly,
        String location,
        String evidence
) {
}

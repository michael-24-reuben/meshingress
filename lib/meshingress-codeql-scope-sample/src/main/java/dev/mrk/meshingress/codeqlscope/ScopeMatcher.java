package dev.mrk.meshingress.codeqlscope;

public record ScopeMatcher(
        ScopeMatcherType type,
        String owner,
        String namePattern,
        String pattern,
        String confidence,
        boolean reviewOnly
) {
    public static ScopeMatcher bytecodeMethod(String owner, String namePattern, String confidence) {
        return new ScopeMatcher(ScopeMatcherType.BYTECODE_METHOD, owner, namePattern, null, confidence, false);
    }

    public static ScopeMatcher bytecodeAnnotation(String owner, String confidence) {
        return new ScopeMatcher(ScopeMatcherType.BYTECODE_ANNOTATION, owner, null, null, confidence, false);
    }

    public static ScopeMatcher codeQlCall(String owner, String namePattern, String confidence) {
        return new ScopeMatcher(ScopeMatcherType.CODEQL_CALL, owner, namePattern, null, confidence, false);
    }

    public static ScopeMatcher regex(String pattern, String confidence, boolean reviewOnly) {
        return new ScopeMatcher(ScopeMatcherType.REGEX, null, null, pattern, confidence, reviewOnly);
    }
}

package dev.mrk.meshingress.artifact.scope;

public record ScopeMatcherDefinition(
        ScopeMatcherType type,
        String owner,
        String namePattern,
        String descriptorPattern,
        String confidence,
        boolean reviewOnly
) {
    public void validate(String ruleId) {
        if (type == null) {
            throw new IllegalArgumentException("Matcher type is required for rule " + ruleId);
        }
        if (ownerRequired() && (owner == null || owner.isBlank())) {
            throw new IllegalArgumentException("Matcher owner is required for rule " + ruleId);
        }
    }

    private boolean ownerRequired() {
        return type == ScopeMatcherType.BYTECODE_METHOD
                || type == ScopeMatcherType.BYTECODE_ANNOTATION
                || type == ScopeMatcherType.BYTECODE_CLASS;
    }
}

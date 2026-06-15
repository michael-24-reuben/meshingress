package dev.mrk.meshingress.codeqlscope;

public enum ScopeMatcherType {
    BYTECODE_METHOD("bytecode-method"),
    BYTECODE_ANNOTATION("bytecode-annotation"),
    CODEQL_CALL("codeql-call"),
    REGEX("regex");

    private final String wireName;

    ScopeMatcherType(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }
}

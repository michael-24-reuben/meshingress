package dev.mrk.meshingress.artifact.scope;

import java.util.Arrays;

public enum ScopeMatcherType {
    BYTECODE_METHOD("bytecode-method"),
    BYTECODE_ANNOTATION("bytecode-annotation"),
    BYTECODE_CLASS("bytecode-class");

    private final String wireName;

    ScopeMatcherType(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static ScopeMatcherType fromWireName(String wireName) {
        return Arrays.stream(values())
                .filter(type -> type.wireName.equals(wireName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown scope matcher type: " + wireName));
    }
}

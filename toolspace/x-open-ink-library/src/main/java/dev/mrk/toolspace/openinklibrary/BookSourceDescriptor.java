package dev.mrk.toolspace.openinklibrary;

import java.util.Set;

public record BookSourceDescriptor(
        String id,
        String displayName,
        BookKind bookKind,
        Set<String> capabilities
) {
    public BookSourceDescriptor {
        capabilities = Set.copyOf(capabilities);
    }
}

package dev.mrk.meshingress.codeqlscope;

import java.util.List;

public record ScopeRule(
        String id,
        String scope,
        List<ScopeMatcher> matchers
) {
}

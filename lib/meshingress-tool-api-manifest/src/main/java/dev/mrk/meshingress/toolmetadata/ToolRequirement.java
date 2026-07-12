package dev.mrk.meshingress.toolmetadata;

import dev.mrk.meshingress.scopes.McpToolScope;

import java.net.URI;
import java.util.Locale;

public record ToolRequirement(
        String kind,
        String name,
        String description,
        boolean required,
        String label,
        String license,
        String vcs,
        String cloneUrl,
        String checkoutRef,
        String baseUrlProperty,
        String credentialProperty,
        String scope,
        String canonicalIdentity
) {
    public ToolRequirement {
        kind = cleanRequired(kind, "tool requirement kind");
        name = cleanRequired(name, "tool requirement name");
        description = clean(description);
        label = clean(label);
        license = clean(license);
        vcs = clean(vcs);
        cloneUrl = clean(cloneUrl);
        checkoutRef = clean(checkoutRef);
        baseUrlProperty = clean(baseUrlProperty);
        credentialProperty = clean(credentialProperty);
        scope = clean(scope);
        canonicalIdentity = clean(canonicalIdentity);
    }

    public static ToolRequirement executable(String command) {
        return new ToolRequirement("executable", command, "", true, "", "", "", "", "", "", "", "", "");
    }

    public static ToolRequirement sourceRepository(String repositoryUrl) {
        return new ToolRequirement(
                "sourceRepository",
                repositoryUrl,
                "",
                true,
                "",
                "",
                "git",
                "",
                "",
                "",
                "",
                "",
                canonicalRepositoryIdentity(repositoryUrl)
        );
    }

    public static ToolRequirement externalApi(String name) {
        return new ToolRequirement("externalApi", name, "", true, "", "", "", "", "", "", "", "", "");
    }

    public static ToolRequirement scope(McpToolScope scope) {
        if (scope == null) {
            throw new IllegalArgumentException("tool scope requirement must not be null");
        }
        return new ToolRequirement("scope", scope.name(), scope.description(), true, "", "", "", "", "", "", "", scope.name(), "");
    }

    public ToolRequirement description(String value) {
        return new ToolRequirement(kind, name, value, required, label, license, vcs, cloneUrl, checkoutRef, baseUrlProperty, credentialProperty, scope, canonicalIdentity);
    }

    public ToolRequirement required(boolean value) {
        return new ToolRequirement(kind, name, description, value, label, license, vcs, cloneUrl, checkoutRef, baseUrlProperty, credentialProperty, scope, canonicalIdentity);
    }

    public ToolRequirement label(String value) {
        return new ToolRequirement(kind, name, description, required, value, license, vcs, cloneUrl, checkoutRef, baseUrlProperty, credentialProperty, scope, canonicalIdentity);
    }

    public ToolRequirement license(String value) {
        return new ToolRequirement(kind, name, description, required, label, value, vcs, cloneUrl, checkoutRef, baseUrlProperty, credentialProperty, scope, canonicalIdentity);
    }

    public ToolRequirement vcs(String value) {
        return new ToolRequirement(kind, name, description, required, label, license, value, cloneUrl, checkoutRef, baseUrlProperty, credentialProperty, scope, canonicalIdentity);
    }

    public ToolRequirement cloneUrl(String value) {
        return new ToolRequirement(kind, name, description, required, label, license, vcs, value, checkoutRef, baseUrlProperty, credentialProperty, scope, canonicalIdentity);
    }

    public ToolRequirement checkoutRef(String value) {
        return new ToolRequirement(kind, name, description, required, label, license, vcs, cloneUrl, value, baseUrlProperty, credentialProperty, scope, canonicalIdentity);
    }

    public ToolRequirement baseUrlProperty(String value) {
        return new ToolRequirement(kind, name, description, required, label, license, vcs, cloneUrl, checkoutRef, value, credentialProperty, scope, canonicalIdentity);
    }

    public ToolRequirement credentialProperty(String value) {
        return new ToolRequirement(kind, name, description, required, label, license, vcs, cloneUrl, checkoutRef, baseUrlProperty, value, scope, canonicalIdentity);
    }

    static String canonicalRepositoryIdentity(String value) {
        String normalized = cleanRequired(value, "source repository URL");
        if (normalized.startsWith("git@")) {
            int colon = normalized.indexOf(':');
            if (colon > 4) {
                String host = normalized.substring(4, colon).toLowerCase(Locale.ROOT);
                return stripGitSuffix(host + "/" + normalized.substring(colon + 1));
            }
        }
        try {
            URI uri = URI.create(normalized);
            if (uri.getHost() != null && uri.getPath() != null && !uri.getPath().isBlank()) {
                return stripGitSuffix(uri.getHost().toLowerCase(Locale.ROOT) + "/" + uri.getPath().replaceFirst("^/+", ""));
            }
        } catch (IllegalArgumentException ignored) {
            // Fall through to best-effort path normalization.
        }
        return stripGitSuffix(normalized.replace('\\', '/').replaceFirst("^/+", ""));
    }

    private static String stripGitSuffix(String value) {
        String stripped = value.endsWith(".git") ? value.substring(0, value.length() - 4) : value;
        return stripped.replaceAll("/+", "/");
    }

    private static String cleanRequired(String value, String name) {
        String cleaned = clean(value);
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return cleaned;
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }
}

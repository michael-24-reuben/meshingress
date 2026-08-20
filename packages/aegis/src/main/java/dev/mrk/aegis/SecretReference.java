package dev.mrk.aegis;

/**
 * Opaque pointer to secret material managed by a host-selected secret store.
 * This type intentionally has no field for an access token, refresh token,
 * API key, private key, or other secret value.
 */
public record SecretReference(
        String provider,
        String key,
        String version
) {
    public SecretReference {
        provider = requireText(provider, "provider");
        key = requireText(key, "key");
        if (version != null && version.isBlank()) {
            throw new IllegalArgumentException("version must not be blank when provided");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

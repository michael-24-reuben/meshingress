package dev.mrk.aegis;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Objects;
import java.util.Properties;

/**
 * Framework-free bootstrap settings parsed from this module's
 * {@code application.properties}. Hosts may parse an alternate properties
 * stream before constructing their Aegis bootstrap service.
 */
public record BootstrapEnrollmentProperties(
        boolean enabled,
        String action,
        Duration credentialTtl,
        int maxAttempts,
        String administratorRole
) {
    public static final String RESOURCE_NAME = "application.properties";
    private static final String PREFIX = "aegis.bootstrap.";

    public BootstrapEnrollmentProperties {
        action = requireText(action, "action");
        credentialTtl = Objects.requireNonNull(credentialTtl, "credentialTtl");
        if (credentialTtl.isZero() || credentialTtl.isNegative()) {
            throw new IllegalArgumentException("credentialTtl must be positive");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least one");
        }
        administratorRole = requireText(administratorRole, "administratorRole");
    }

    public static BootstrapEnrollmentProperties loadDefault() {
        try (InputStream input = BootstrapEnrollmentProperties.class.getClassLoader()
                .getResourceAsStream(RESOURCE_NAME)) {
            if (input == null) {
                throw new IllegalStateException("Missing Aegis " + RESOURCE_NAME);
            }
            return load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load Aegis " + RESOURCE_NAME, exception);
        }
    }

    public static BootstrapEnrollmentProperties load(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input");
        Properties properties = new Properties();
        properties.load(input);
        return from(properties);
    }

    public static BootstrapEnrollmentProperties from(Properties properties) {
        Objects.requireNonNull(properties, "properties");
        return new BootstrapEnrollmentProperties(
                parseBoolean(properties, "enabled"),
                required(properties, "action"),
                parseDuration(properties, "credential-ttl"),
                parsePositiveInt(properties, "max-attempts"),
                required(properties, "administrator-role")
        );
    }

    private static Duration parseDuration(Properties properties, String key) {
        try {
            return Duration.parse(required(properties, key));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(PREFIX + key + " must be an ISO-8601 duration", exception);
        }
    }

    private static boolean parseBoolean(Properties properties, String key) {
        String value = required(properties, key);
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw new IllegalArgumentException(PREFIX + key + " must be true or false");
        }
        return Boolean.parseBoolean(value);
    }

    private static int parsePositiveInt(Properties properties, String key) {
        try {
            return Integer.parseInt(required(properties, key));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(PREFIX + key + " must be an integer", exception);
        }
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(PREFIX + key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required property " + PREFIX + key);
        }
        return value.trim();
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

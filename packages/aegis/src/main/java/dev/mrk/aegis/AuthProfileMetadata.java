package dev.mrk.aegis;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Safe, durable profile metadata. Labels must not contain credential material. */
public record AuthProfileMetadata(
        String displayName,
        Instant createdAt,
        Instant updatedAt,
        Map<String, String> labels
) {
    public AuthProfileMetadata {
        if (displayName != null && displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank when provided");
        }
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
        labels = copyTextMap(labels, "labels");
    }

    static Map<String, String> copyTextMap(Map<String, String> values, String name) {
        values = Map.copyOf(Objects.requireNonNull(values, name));
        if (values.entrySet().stream().anyMatch(entry -> isBlank(entry.getKey()) || isBlank(entry.getValue()))) {
            throw new IllegalArgumentException(name + " must not contain blank keys or values");
        }
        return values;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

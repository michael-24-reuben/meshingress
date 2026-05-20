package dev.mrk.toolspace.instagram.instafetch;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class RequestConfigType {
    private final Map<String, Object> values;

    public RequestConfigType(Map<String, ?> values) {
        this.values = copy(values);
    }

    public static RequestConfigType empty() {
        return new RequestConfigType(Map.of());
    }

    public static RequestConfigType of(Map<String, ?> values) {
        return new RequestConfigType(values);
    }

    public Map<String, Object> values() {
        return values;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    private static Map<String, Object> copy(Map<String, ?> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> copy.put(Objects.requireNonNull(key, "request config key"), value));
        return Collections.unmodifiableMap(copy);
    }
}

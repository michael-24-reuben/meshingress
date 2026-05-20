package dev.mrk.toolspace.instagram.instafetch;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class InstaFetchOptions {
    private static final InstaFetchOptions DEFAULTS = builder().build();

    private final Map<String, String> headers;
    private final String userAgent;
    private final String cookie;
    private final RequestConfigType requestConfig;

    private InstaFetchOptions(Builder builder) {
        this.headers = copyHeaders(builder.headers);
        this.userAgent = builder.userAgent;
        this.cookie = builder.cookie;
        this.requestConfig = builder.requestConfig == null ? RequestConfigType.empty() : builder.requestConfig;
    }

    public static InstaFetchOptions defaults() {
        return DEFAULTS;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, String> headers() {
        return headers;
    }

    public String userAgent() {
        return userAgent;
    }

    public String cookie() {
        return cookie;
    }

    public RequestConfigType requestConfig() {
        return requestConfig;
    }

    public boolean isEmpty() {
        return headers.isEmpty()
                && userAgent == null
                && cookie == null
                && requestConfig.isEmpty();
    }

    private static Map<String, String> copyHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        headers.forEach((key, value) -> copy.put(Objects.requireNonNull(key, "header name"), value));
        return Collections.unmodifiableMap(copy);
    }

    public static final class Builder {
        private final Map<String, String> headers = new LinkedHashMap<>();
        private String userAgent;
        private String cookie;
        private RequestConfigType requestConfig;

        private Builder() {
        }

        public Builder header(String name, String value) {
            headers.put(Objects.requireNonNull(name, "name"), value);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            if (headers != null) {
                this.headers.putAll(headers);
            }
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder cookie(String cookie) {
            this.cookie = cookie;
            return this;
        }

        public Builder requestConfig(RequestConfigType requestConfig) {
            this.requestConfig = requestConfig;
            return this;
        }

        public InstaFetchOptions build() {
            return new InstaFetchOptions(this);
        }
    }
}

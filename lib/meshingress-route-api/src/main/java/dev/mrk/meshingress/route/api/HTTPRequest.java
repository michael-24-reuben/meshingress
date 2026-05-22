package dev.mrk.meshingress.route.api;

import java.util.Map;

public record HTTPRequest<Q, P, B>(
        Q query,
        P params,
        B body,
        String routeId,
        Map<String, String> headers,
        Map<String, Object> attributes
) {

    public HTTPRequest {
        routeId = routeId == null ? "" : routeId;
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static <Q, P, B> HTTPRequest<Q, P, B> of(Q query, P params, B body) {
        return new HTTPRequest<>(query, params, body, "", Map.of(), Map.of());
    }

    public HTTPRequest<Q, P, B> withRouteId(String nextRouteId) {
        return new HTTPRequest<>(query, params, body, nextRouteId, headers, attributes);
    }

    public HTTPRequest<Q, P, B> withAttribute(String name, Object value) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Request attribute name must not be blank");
        }
        Map<String, Object> nextAttributes = new java.util.LinkedHashMap<>(attributes);
        nextAttributes.put(name, value);
        return new HTTPRequest<>(query, params, body, routeId, headers, nextAttributes);
    }
}
